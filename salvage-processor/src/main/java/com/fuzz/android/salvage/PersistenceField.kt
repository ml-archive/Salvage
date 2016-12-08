package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.Serializable
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

        val typeElement: TypeElement? = manager.elements.getTypeElement(elementTypeName.toString())
        isSerializable = implementsClass(manager.processingEnvironment,
                Serializable::class.java.name, typeElement)

        isNested = typeElement != null && typeElement.getAnnotation(Persist::class.java) != null

        val typeName = elementTypeName
        wrapperAccessor = if (isSerializable && typeName != null
                && !ClassLookupMap.hasType(typeName)) SerialiableAccessor(typeName) else null

        keyFieldName = "key_" + elementName

        nestedAccessor = if (isNested && typeName != null) NestedAccessor(elementName, typeName,
                keyFieldName, accessor) else null

        bundleMethodName = if (typeName != null) ClassLookupMap.valueForType(typeName, isSerializable) ?: "" else ""

    }

    fun writePersistence(methodBuilder: MethodSpec.Builder, inlineBundles: Boolean) {
        elementTypeName?.let { typeName ->
            var block = accessor.get(CodeBlock.of(defaultParam), !inlineBundles)
            wrapperAccessor?.let { block = wrapperAccessor.get(block, !inlineBundles) }
            if (nestedAccessor != null) {
                methodBuilder.addCode(nestedAccessor.get(block, !inlineBundles))
            } else {
                methodBuilder.addStatement("bundle.put\$L(\$L, \$L)",
                        bundleMethodName, keyFieldName, block)
            }
        }
    }

    fun writeUnpack(methodBuilder: MethodSpec.Builder, inlineBundles: Boolean) {
        elementTypeName?.let { typeName ->
            val bundleMethod = if (isNested) ClassLookupMap.map[BUNDLE] else bundleMethodName
            var block = CodeBlock.of("bundle.get\$L(\$L)", bundleMethod, keyFieldName)
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