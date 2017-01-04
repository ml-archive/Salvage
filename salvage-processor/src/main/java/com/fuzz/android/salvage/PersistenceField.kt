package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistField
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
class PersistenceField(manager: ProcessorManager, element: Element, isPackagePrivate: Boolean)
    : BaseDefinition(element, manager) {


    val accessor: Accessor

    val bundleKey: String

    val bundleMethodName: String

    val nestedAccessor: Accessor?

    val keyFieldName: String

    val isList: Boolean
    val isMap: Boolean
    var hasCustomConverter: Boolean = false

    val persisterFieldName: String
    var persisterDefinitionTypeName: TypeName? = null

    val field: Field

    init {

        val annotation = element.getAnnotation(PersistField::class.java)
        var getterName = ""
        var setterName = ""
        if (annotation != null) {
            try {
                annotation.bundlePersister
            } catch (mte: MirroredTypeException) {
                persisterDefinitionTypeName = ClassName.get(mte.typeMirror)
                hasCustomConverter = true
            }

            getterName = annotation.getterName
            setterName = annotation.setterName
        }

        if (isPackagePrivate) {
            accessor = PackagePrivateScopeAccessor(elementName, packageName,
                    ClassName.get(element.enclosingElement as TypeElement).simpleName())
            PackagePrivateScopeAccessor.putElement(accessor.helperClassName, elementName)
        } else if (element.modifiers.contains(Modifier.PRIVATE)) {

            val isBoolean = elementTypeName?.box() == TypeName.BOOLEAN.box()

            accessor = PrivateScopeAccessor(elementName, object : GetterSetter {
                override val getterName = getterName
                override val setterName = setterName

            }, isBoolean)
        } else {
            accessor = VisibleScopeAccessor(elementName)
        }

        // for now, later we can configure
        bundleKey = elementName

        persisterFieldName = elementName + "_persister"

        val typeName = elementTypeName

        var typeElement: TypeElement? = manager.elements.getTypeElement(elementTypeName.toString())
        if (typeElement == null && erasedTypeName != null) {
            typeElement = manager.elements.getTypeElement(erasedTypeName.toString())
        }


        if (typeElement == null && typeName is ArrayTypeName) {
            typeElement = manager.elements.getTypeElement(typeName.componentType.toString())
        }

        if (typeElement != null) {
            this.typeElement = typeElement
        }

        isList = implementsClass(manager.processingEnvironment, List::class.java.name, typeElement)
        isMap = implementsClass(manager.processingEnvironment, Map::class.java.name, typeElement)

        val isNested = typeElement != null && typeElement.getAnnotation(Persist::class.java) != null

        val isSerializable = !isList && !isMap &&
                isSerializable(manager, typeElement) && (typeName != null
                && !ClassLookupMap.hasType(typeName))

        keyFieldName = "key_" + elementName

        bundleMethodName = if (typeName != null) ClassLookupMap.valueForType(typeName, isSerializable) ?: "" else ""

        if (isList) {
            field = ListField(manager, this)
        } else if (isMap) {
            field = MapField(manager, this)
        } else if (isSerializable || isNested || hasCustomConverter) {
            field = CustomField(manager, this)
        } else {
            field = BasicField(manager, this)
        }

        field.init()


        nestedAccessor = field.nestedAccessor
    }

    fun writePersistence(methodBuilder: MethodSpec.Builder) {
        elementTypeName?.let { typeName ->
            val block = accessor.get(CodeBlock.of(defaultParam), null)
            if (nestedAccessor != null) {
                methodBuilder.addCode(nestedAccessor.get(block, null))
            } else {
                methodBuilder.addStatement("bundle.put\$L(\$L + \$L, \$L)",
                        bundleMethodName, uniqueBaseKey, keyFieldName, block)
            }
        }
    }

    fun writeUnpack(methodBuilder: MethodSpec.Builder) {
        elementTypeName?.let { typeName ->
            val bundleMethod = bundleMethodName
            val block = CodeBlock.of("bundle.get\$L(\$L + \$L)", bundleMethod, uniqueBaseKey, keyFieldName)
            val accessedBlock: CodeBlock;
            if (nestedAccessor == null) {
                accessedBlock = accessor.set(block, CodeBlock.of(defaultParam))
                methodBuilder.addStatement(accessedBlock)
            } else {
                accessedBlock = nestedAccessor.set(accessor.get(CodeBlock.of(defaultParam), null),
                        CodeBlock.of(defaultParam))
                methodBuilder.addCode(accessedBlock)
            }
        }
    }

    fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        field.writeForConstructor(constructorCode)
    }

    fun writeFields(typeBuilder: TypeSpec.Builder) {
        typeBuilder.addField(FieldSpec.builder(String::class.java, keyFieldName,
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$BASE_KEY + \$S", elementName)
                .build())

        field.writeFields(typeBuilder)
    }

}