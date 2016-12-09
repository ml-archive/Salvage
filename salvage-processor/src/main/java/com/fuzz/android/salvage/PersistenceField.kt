package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.squareup.javapoet.*
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
class PersistenceField(manager: ProcessorManager, element: Element, isPackagePrivate: Boolean)
    : BaseDefinition(element, manager) {


    val accessor: Accessor

    val bundleKey: String

    var isSerializable = false

    var isNested = false

    val bundleMethodName: String

    val wrapperAccessor: Accessor?

    val nestedAccessor: Accessor?

    val keyFieldName: String

    val isList: Boolean

    var componentTypeName: TypeName? = null
    var componentElement: TypeElement? = null

    init {
        if (isPackagePrivate) {
            accessor = PackagePrivateScopeAccessor(elementName, packageName,
                    ClassName.get(element.enclosingElement as TypeElement).simpleName())
            PackagePrivateScopeAccessor.putElement(accessor.helperClassName, elementName)
        } else if (element.modifiers.contains(Modifier.PRIVATE)) {

            val isBoolean = elementTypeName?.box() == TypeName.BOOLEAN.box()

            accessor = PrivateScopeAccessor(elementName, object : GetterSetter {
                override val getterName = ""
                override val setterName = ""

            }, isBoolean, false)
        } else {
            accessor = VisibleScopeAccessor(elementName)
        }

        // for now, later we can configure
        bundleKey = elementName

        var typeElement: TypeElement? = manager.elements.getTypeElement(elementTypeName.toString())
        if (typeElement == null && erasedTypeName != null) {
            typeElement = manager.elements.getTypeElement(erasedTypeName.toString())
        }


        isList = implementsClass(manager.processingEnvironment, List::class.java.name, typeElement)

        if (isList) {
            componentTypeName = (elementTypeName as ParameterizedTypeName).typeArguments[0]
            componentElement = manager.elements.getTypeElement(componentTypeName.toString())

            val compTypeName = componentTypeName
            if (compTypeName != null && compTypeName is WildcardTypeName) {
                componentTypeName = compTypeName.upperBounds[0]
            }
        }

        val basicTypeName = if (isList) componentTypeName else elementTypeName
        val basicElement = if (isList) componentElement else typeElement

        isNested = basicElement != null && basicElement.getAnnotation(Persist::class.java) != null
        isSerializable = isSerializable(manager, basicElement)

        val typeName = elementTypeName
        wrapperAccessor = if (isSerializable && basicTypeName != null
                && !ClassLookupMap.hasType(basicTypeName)) SerializableAccessor(basicTypeName) else null

        keyFieldName = "key_" + elementName

        bundleMethodName = if (basicTypeName != null) ClassLookupMap.valueForType(basicTypeName, isSerializable) ?: "" else ""

        nestedAccessor = if (isNested && !isList && typeName != null) {
            NestedAccessor(elementName, typeName,
                    keyFieldName, accessor)
        } else if (isList && typeName != null) {
            ListAccessor(elementName, typeName, bundleMethodName, keyFieldName, accessor,
                    componentTypeName, isNested)
        } else {
            null
        }


    }

    fun writePersistence(methodBuilder: MethodSpec.Builder, inlineBundles: Boolean) {
        elementTypeName?.let { typeName ->
            var block = accessor.get(CodeBlock.of(defaultParam), !inlineBundles, null)
            wrapperAccessor?.let { block = wrapperAccessor.get(block, !inlineBundles, null) }
            if (nestedAccessor != null) {
                methodBuilder.addCode(nestedAccessor.get(block, !inlineBundles, null))
            } else {
                methodBuilder.addStatement("bundle.put\$L(\$L + \$L, \$L)",
                        bundleMethodName, uniqueBaseKey, keyFieldName, block)
            }
        }
    }

    fun writeUnpack(methodBuilder: MethodSpec.Builder, inlineBundles: Boolean) {
        elementTypeName?.let { typeName ->
            val bundleMethod = if (isNested) ClassLookupMap.map[BUNDLE] else bundleMethodName
            var block = CodeBlock.of("bundle.get\$L(\$L + \$L)", bundleMethod, uniqueBaseKey, keyFieldName)
            wrapperAccessor?.let { block = wrapperAccessor.set(block, null, !inlineBundles) }

            val accessedBlock: CodeBlock;
            if (nestedAccessor == null) {
                accessedBlock = accessor.set(block, CodeBlock.of(defaultParam), !inlineBundles)
                methodBuilder.addStatement(accessedBlock)
            } else {
                accessedBlock = nestedAccessor.set(block, CodeBlock.of(defaultParam), !inlineBundles)
                methodBuilder.addCode(accessedBlock)
            }
        }
    }

    fun writeFieldDefinition(typeBuilder: TypeSpec.Builder) {
        typeBuilder.addField(FieldSpec.builder(String::class.java, keyFieldName,
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$BASE_KEY + \$S", elementName)
                .build())

    }

}