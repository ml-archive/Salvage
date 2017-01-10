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

class FieldHolder(val manager: ProcessorManager,
                  val basicElement: TypeElement?,
                  val basicTypeName: TypeName?,
                  val hasCustomConverter: Boolean,
                  var persisterDefinitionTypeName: TypeName?,
                  val persisterFieldName: String,
                  val hasBundleMethod: Boolean) {

    var isNested = false
    var isSerializable = false

    fun init() {
        val basicElement = basicElement
        val basicTypeName = basicTypeName
        isNested = basicElement?.getAnnotation(Persist::class.java) != null
        isSerializable = isSerializable(manager, basicElement) && (basicTypeName != null
                && !ClassLookupMap.hasType(basicTypeName))

        if (persisterDefinitionTypeName == null) {
            val basicType = basicTypeName
            if (basicType?.isPrimitive?.not() ?: false) {
                persisterDefinitionTypeName = ParameterizedTypeName.get(BUNDLE_PERSISTER, basicType)
            }
        }

    }

    fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        if (isSerializable || hasCustomConverter) {
            val persisterTypeName = if (isSerializable) {
                ParameterizedTypeName.get(SERIALIZABLE_PERSISTER, basicTypeName)
            } else {
                persisterDefinitionTypeName
            }
            constructorCode.addStatement(CodeBlock.of("\$L = new \$T()",
                    persisterFieldName, persisterTypeName))
        } else if (!hasBundleMethod) {
            constructorCode.addStatement(CodeBlock.of("\$L = \$T.getBundlePersister(\$T.class)",
                    persisterFieldName, SALVAGER, basicElement))
        }
    }

    fun writeFields(typeBuilder: TypeSpec.Builder) {
        if (!hasBundleMethod || hasCustomConverter) {
            val persisterTypeName = if (isSerializable) {
                ParameterizedTypeName.get(SERIALIZABLE_PERSISTER, basicTypeName)
            } else {
                persisterDefinitionTypeName
            }
            typeBuilder.addField(persisterTypeName, persisterFieldName,
                    Modifier.PRIVATE, Modifier.FINAL)
        }
    }
}

abstract class Field(val manager: ProcessorManager,
                     val persistenceField: PersistenceField,
                     open val basicElement: TypeElement? = persistenceField.typeElement,
                     open val basicTypeName: TypeName? = persistenceField.elementTypeName) {

    open val hasBundleMethod: Boolean
        get() {
            val typeName = basicTypeName
            return typeName != null && ClassLookupMap.hasType(typeName)
        }

    open val fieldHolder: FieldHolder by lazy {
        FieldHolder(manager, persistenceField.typeElement, persistenceField.elementTypeName,
                persistenceField.hasCustomConverter, persistenceField.persisterDefinitionTypeName,
                persistenceField.persisterFieldName, hasBundleMethod)
    }

    abstract val nestedAccessor: Accessor?

    fun init() {
        initialize()

        fieldHolder.init()

    }

    abstract fun initialize()

    open fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        fieldHolder.writeForConstructor(constructorCode)
    }

    open fun writeFields(typeBuilder: TypeSpec.Builder) {
        fieldHolder.writeFields(typeBuilder)
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
                  persistenceField: PersistenceField,
                  val isFinal: Boolean) : Field(manager, persistenceField) {
    override val nestedAccessor: Accessor
        get() = NestedAccessor(persistenceField.persisterFieldName,
                persistenceField.keyFieldName, if (isFinal) EmptyAccessor() else persistenceField.accessor)

    override fun initialize() {
    }
}

class ListField(manager: ProcessorManager,
                persistenceField: PersistenceField,
                val isFinal: Boolean) : Field(manager, persistenceField) {

    var componentTypeName: TypeName? = null
    var componentElement: TypeElement? = null

    override val fieldHolder: FieldHolder by lazy {
        FieldHolder(manager, componentElement, componentTypeName,
                persistenceField.hasCustomConverter, persistenceField.persisterDefinitionTypeName,
                persistenceField.persisterFieldName, hasBundleMethod)
    }

    override val hasBundleMethod: Boolean
        get() = false

    override val basicElement: TypeElement?
        get() = componentElement

    override val basicTypeName: TypeName?
        get() = componentTypeName

    override val nestedAccessor: Accessor? by lazy {
        ListAccessor(persistenceField.keyFieldName, if (isFinal) EmptyAccessor() else persistenceField.accessor,
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
               persistenceField: PersistenceField,
               val isFinal: Boolean) : Field(manager, persistenceField) {

    var componentTypeName: TypeName? = null
    var componentElement: TypeElement? = null
    var keyTypeName: TypeName? = null
    var keyElement: TypeElement? = null

    override val fieldHolder: FieldHolder by lazy {
        FieldHolder(manager, componentElement, componentTypeName,
                persistenceField.hasCustomConverter, persistenceField.persisterDefinitionTypeName,
                persistenceField.persisterFieldName, hasBundleMethod)
    }


    val keyFieldHolder: FieldHolder by lazy {
        FieldHolder(manager, keyElement, keyTypeName,
                persistenceField.hasCustomConverter, persistenceField.persisterDefinitionTypeName,
                keyPersisterFieldName, hasBundleMethod)
    }

    override val basicTypeName: TypeName?
        get() = componentTypeName

    override val basicElement: TypeElement?
        get() = componentElement

    override val hasBundleMethod: Boolean
        get() = false

    val keyPersisterFieldName: String
        get() = persistenceField.elementName + "_keypersister"

    override val nestedAccessor: Accessor? by lazy {
        MapAccessor(persistenceField.keyFieldName, if (isFinal) EmptyAccessor() else persistenceField.accessor,
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

        keyFieldHolder.init()
    }

    override fun writeForConstructor(constructorCode: MethodSpec.Builder) {
        super.writeForConstructor(constructorCode)
        keyFieldHolder.writeForConstructor(constructorCode)
    }

    override fun writeFields(typeBuilder: TypeSpec.Builder) {
        super.writeFields(typeBuilder)
        keyFieldHolder.writeFields(typeBuilder)
    }
}