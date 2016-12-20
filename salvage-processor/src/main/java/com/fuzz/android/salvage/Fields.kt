package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */

abstract class Field(val manager: ProcessorManager,
                     val persistenceField: PersistenceField) {
    var isNested = false
    var isSerializable = false
    open val hasBundleMethod: Boolean
        get() {
            val typeName = basicTypeName
            return typeName != null && ClassLookupMap.hasType(typeName)
        }

    open val basicElement: TypeElement?
        get() = persistenceField.typeElement

    open val basicTypeName: TypeName?
        get() = persistenceField.elementTypeName

    abstract val nestedAccessor: Accessor?

    fun init() {
        initialize()

        val basicElement = basicElement
        val basicTypeName = basicTypeName
        isNested = basicElement?.getAnnotation(Persist::class.java) != null
        isSerializable = isSerializable(manager, basicElement) && (basicTypeName != null
                && !ClassLookupMap.hasType(basicTypeName))

    }

    abstract fun initialize()

    open fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        if (isSerializable || persistenceField.hasCustomConverter) {
            val persisterTypeName = if (isSerializable) {
                ParameterizedTypeName.get(SERIALIZABLE_PERSISTER, basicTypeName)
            } else {
                persistenceField.persisterDefinitionTypeName
            }
            constructorCode.addStatement(CodeBlock.of("\$L = new \$T()",
                    persistenceField.persisterFieldName, persisterTypeName))
        } else if (!hasBundleMethod) {
            constructorCode.addStatement(CodeBlock.of("\$L = \$T.getBundlePersister(\$T.class)",
                    persistenceField.persisterFieldName, SALVAGER, basicElement))
        }
    }

    open fun writeFields(typeBuilder: TypeSpec.Builder) {
        if (!hasBundleMethod || persistenceField.hasCustomConverter) {
            typeBuilder.addField(persistenceField.persisterDefinitionTypeName,
                    persistenceField.persisterFieldName,
                    Modifier.PRIVATE, Modifier.FINAL)
        }
    }
}

class BasicField(manager: ProcessorManager,
                 persistenceField: PersistenceField) : Field(manager, persistenceField) {

    override val nestedAccessor: Accessor?
        get() = null

    override fun initialize() {

    }
}

class CustomField(manager: ProcessorManager,
                  persistenceField: PersistenceField) : Field(manager, persistenceField) {
    override val nestedAccessor: Accessor
        get() = NestedAccessor(persistenceField.persisterFieldName,
                persistenceField.keyFieldName, persistenceField.accessor)

    override fun initialize() {
    }
}

class ListField(manager: ProcessorManager,
                persistenceField: PersistenceField) : Field(manager, persistenceField) {

    var componentTypeName: TypeName? = null
    var componentElement: TypeElement? = null

    override val hasBundleMethod: Boolean
        get() = false

    override val basicElement: TypeElement?
        get() = componentElement

    override val basicTypeName: TypeName?
        get() = componentTypeName

    override val nestedAccessor: Accessor? by lazy {
        ListAccessor(persistenceField.keyFieldName, persistenceField.accessor,
                persistenceField.persisterFieldName)
    }

    override fun initialize() {
        componentTypeName = (persistenceField.elementTypeName as ParameterizedTypeName).typeArguments[0]
        componentElement = manager.elements.getTypeElement(componentTypeName.toString())

        val compTypeName = componentTypeName
        if (compTypeName != null && compTypeName is WildcardTypeName) {
            componentTypeName = compTypeName.upperBounds[0]
        }
    }

}

class MapField(manager: ProcessorManager,
               persistenceField: PersistenceField) : Field(manager, persistenceField) {

    var componentTypeName: TypeName? = null
    var componentElement: TypeElement? = null
    var keyTypeName: TypeName? = null
    var keyElement: TypeElement? = null

    override val basicTypeName: TypeName?
        get() = componentTypeName

    override val basicElement: TypeElement?
        get() = componentElement

    override val hasBundleMethod: Boolean
        get() = false

    val keyPersisterFieldName: String
        get() = persistenceField.elementName + "_keypersister"

    val keyPersisterDefinitionTypeName: TypeName
        get() = ParameterizedTypeName.get(BUNDLE_PERSISTER, keyTypeName)

    override val nestedAccessor: Accessor? by lazy {
        MapAccessor(persistenceField.keyFieldName, persistenceField.accessor,
                persistenceField.persisterFieldName, keyPersisterFieldName)
    }

    override fun initialize() {

        val parametrized = (persistenceField.elementTypeName as ParameterizedTypeName)
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

    override fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        super.writeForConstructor(constructorCode)
        constructorCode.addStatement(CodeBlock.of("\$L = \$T.getBundlePersister(\$T.class)",
                keyPersisterFieldName, SALVAGER, keyElement))
    }

    override fun writeFields(typeBuilder: TypeSpec.Builder) {
        super.writeFields(typeBuilder)
        typeBuilder.addField(keyPersisterDefinitionTypeName, keyPersisterFieldName,
                Modifier.PRIVATE, Modifier.FINAL)
    }
}