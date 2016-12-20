package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistField
import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
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

    var isSerializable = false

    var isNested = false

    val bundleMethodName: String

    val wrapperAccessor: Accessor?

    val nestedAccessor: Accessor?

    val keyFieldName: String

    val isList: Boolean
    val isMap: Boolean
    var hasCustomConverter: Boolean = false

    var componentTypeName: TypeName? = null
    var componentElement: TypeElement? = null
    var keyTypeName: TypeName? = null
    var keyElement: TypeElement? = null

    val basicTypeName: TypeName?
    val basicElement: Element?

    val persisterFieldName: String
    var persisterDefinitionTypeName: TypeName? = null
    val keyPersisterFieldName: String
    val keyPersisterDefinitionTypeName: TypeName

    init {

        val annotation = element.getAnnotation(PersistField::class.java)
        if (annotation != null) {
            try {
                annotation.bundlePersister
            } catch (mte: MirroredTypeException) {
                persisterDefinitionTypeName = ClassName.get(mte.typeMirror)
                hasCustomConverter = true
            }
        }

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
        isMap = implementsClass(manager.processingEnvironment, Map::class.java.name, typeElement)

        if (isList) {
            componentTypeName = (elementTypeName as ParameterizedTypeName).typeArguments[0]
            componentElement = manager.elements.getTypeElement(componentTypeName.toString())

            val compTypeName = componentTypeName
            if (compTypeName != null && compTypeName is WildcardTypeName) {
                componentTypeName = compTypeName.upperBounds[0]
            }
        } else if (isMap) {
            val parametrized = (elementTypeName as ParameterizedTypeName)
            componentTypeName = parametrized.typeArguments[1]
            componentElement = manager.elements.getTypeElement(componentTypeName.toString())

            val compTypeName = componentTypeName
            if (compTypeName != null && compTypeName is WildcardTypeName) {
                componentTypeName = compTypeName.upperBounds[0]
            }

            keyTypeName = parametrized.typeArguments[0]
            keyElement = manager.elements.getTypeElement(keyTypeName.toString())

            val keyTypeName = keyTypeName
            if (keyTypeName != null && keyTypeName is WildcardTypeName) {
                this.keyTypeName = keyTypeName.upperBounds[0]
            }
        }

        basicTypeName = if (isList || isMap) componentTypeName else elementTypeName
        basicElement = if (isList || isMap) componentElement else typeElement

        isNested = basicElement != null && basicElement.getAnnotation(Persist::class.java) != null
        isSerializable = isSerializable(manager, basicElement) && (basicTypeName != null
                && !ClassLookupMap.hasType(basicTypeName) || isList || isMap)

        val typeName = elementTypeName
        wrapperAccessor = if (isSerializable && basicTypeName != null) SerializableAccessor(basicTypeName) else null

        keyFieldName = "key_" + elementName

        bundleMethodName = if (basicTypeName != null) ClassLookupMap.valueForType(basicTypeName, isSerializable) ?: "" else ""

        persisterFieldName = elementName + "_persister"

        if (persisterDefinitionTypeName == null) {
            if (isSerializable || isNested) {
                persisterDefinitionTypeName = ParameterizedTypeName.get(BUNDLE_PERSISTER, basicTypeName)
            }
        }

        if (isMap) {
            keyPersisterFieldName = elementName + "_keypersister"
            keyPersisterDefinitionTypeName = ParameterizedTypeName.get(BUNDLE_PERSISTER, keyTypeName)
        } else {
            keyPersisterFieldName = ""
            keyPersisterDefinitionTypeName = ClassName.OBJECT
        }

        nestedAccessor = if ((isNested || hasCustomConverter) && !isList && !isMap && typeName != null) {
            NestedAccessor(persisterFieldName, keyFieldName,
                    accessor)
        } else if (isList && typeName != null) {
            ListAccessor(keyFieldName, accessor, persisterFieldName)
        } else if (isMap && typeName != null) {
            MapAccessor(keyFieldName, accessor, persisterFieldName,
                    keyPersisterFieldName)
        } else {
            null
        }
    }

    fun writePersistence(methodBuilder: MethodSpec.Builder) {
        elementTypeName?.let { typeName ->
            var block = accessor.get(CodeBlock.of(defaultParam), null)
            wrapperAccessor?.let { block = wrapperAccessor.get(block, null) }
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
            val bundleMethod = if (isNested) ClassLookupMap.map[BUNDLE] else bundleMethodName
            var block = CodeBlock.of("bundle.get\$L(\$L + \$L)", bundleMethod, uniqueBaseKey, keyFieldName)
            wrapperAccessor?.let { block = wrapperAccessor.set(block, null) }

            val accessedBlock: CodeBlock;
            if (nestedAccessor == null) {
                accessedBlock = accessor.set(block, CodeBlock.of(defaultParam))
                methodBuilder.addStatement(accessedBlock)
            } else {
                accessedBlock = nestedAccessor.set(block, CodeBlock.of(defaultParam))
                methodBuilder.addCode(accessedBlock)
            }
        }
    }

    fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        if (isNested) {
            constructorCode.addStatement(CodeBlock.of("\$L = \$T.getBundlePersister(\$T.class)",
                    persisterFieldName, SALVAGER, basicElement))
        } else if (isSerializable) {
            constructorCode.addStatement(CodeBlock.of("\$L = new \$T()", persisterFieldName,
                    ParameterizedTypeName.get(SERIALIZABLE_PERSISTER, basicTypeName)))
        } else if (hasCustomConverter) {
            constructorCode.addStatement(CodeBlock.of("\$L = new \$T()", persisterFieldName,
                    persisterDefinitionTypeName))
        }

        if (isMap) {
            constructorCode.addStatement(CodeBlock.of("\$L = \$T.getBundlePersister(\$T.class)",
                    keyPersisterFieldName, SALVAGER, keyElement))
        }
    }

    fun writeFields(typeBuilder: TypeSpec.Builder) {
        typeBuilder.addField(FieldSpec.builder(String::class.java, keyFieldName,
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$BASE_KEY + \$S", elementName)
                .build())

        if (isSerializable || isNested || hasCustomConverter) {
            typeBuilder.addField(persisterDefinitionTypeName, persisterFieldName,
                    Modifier.PRIVATE, Modifier.FINAL)
        }

        if (isMap) {
            typeBuilder.addField(keyPersisterDefinitionTypeName, keyPersisterFieldName,
                    Modifier.PRIVATE, Modifier.FINAL)
        }
    }

}