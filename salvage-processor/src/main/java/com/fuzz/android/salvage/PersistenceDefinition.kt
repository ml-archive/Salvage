package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistArguments
import com.fuzz.android.salvage.core.PersistField
import com.fuzz.android.salvage.core.PersistPolicy
import com.grosner.kpoet.*
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

val BASE_KEY = "BASE_KEY"

val uniqueBaseKey = "uniqueBaseKey"

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
class PersistenceDefinition(typeElement: TypeElement, manager: ProcessorManager)
    : BaseDefinition(typeElement, manager) {

    val persistenceFields: MutableList<PersistenceField> = arrayListOf()

    val persistPolicy: PersistPolicy

    val argument: Boolean

    init {
        setOutputClassName("Persister")

        val persistenceAnnotation: Persist? = typeElement.getAnnotation(Persist::class.java)
        if (persistenceAnnotation != null) {
            persistPolicy = persistenceAnnotation.persistPolicy
            argument = false
        } else {
            val persistArgument: PersistArguments? = typeElement.getAnnotation(PersistArguments::class.java);
            if (persistArgument != null) {
                persistPolicy = persistArgument.persistPolicy;
                argument = true;
            } else {
                persistPolicy = PersistPolicy.VISIBLE_FIELDS_AND_METHODS
                argument = false
            }
        }

        val elements = ElementUtility.getAllElements(typeElement, manager)

        // use map to find getter / setters
        val elementMap = elements.associateBy { it.simpleName.toString() }

        elements.forEach {

            var isValidField = ElementUtility.isValidAllFields(true, it)

            // package private, will generate helper
            val isPackagePrivate = ElementUtility.isPackagePrivate(it)
            val isPackagePrivateNotInSamePackage = isPackagePrivate &&
                    !ElementUtility.isInSamePackage(manager, it, typeElement)

            if (isValidField) {
                if (persistPolicy == PersistPolicy.ANNOTATIONS_ONLY) {
                    isValidField = it.getAnnotation(PersistField::class.java) != null
                } else if (persistPolicy == PersistPolicy.PRIVATE_ACCESSORS_ONLY) {
                    isValidField = it.modifiers.contains(Modifier.PRIVATE)
                } else if (persistPolicy == PersistPolicy.VISIBLE_FIELDS_ONLY) {
                    isValidField = !it.modifiers.contains(Modifier.PRIVATE)
                }
                if (isValidField) {
                    val persistenceField = PersistenceField(manager, it, isPackagePrivateNotInSamePackage)

                    var success = true
                    if (persistenceField.accessor is PrivateScopeAccessor) {
                        val getterName = persistenceField.accessor.getterNameElement
                        val setterName = persistenceField.accessor.setterNameElement

                        if (elementMap[getterName] == null ||
                                elementMap[setterName] == null && !it.modifiers.contains(Modifier.FINAL)) {
                            manager.logWarning(PersistenceDefinition::class, "Cannot find the referenced getterName" +
                                    "of $getterName or setterName of $setterName for $elementName. Ensure they are defined and visible. " +
                                    "If this is not intended, try adding @PersistIgnore or Persist Policy ${PersistPolicy.VISIBLE_FIELDS_ONLY}." +
                                    "This field will be ignored.")
                            success = false
                        }
                    }

                    if (success) persistenceFields.add(persistenceField)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun writePackageHelper(processingEnvironment: ProcessingEnvironment) {
        writePackageHelper(processingEnvironment, this, persistenceFields, manager)
    }

    override val extendsClass: TypeName?
        get() = ParameterizedTypeName.get(BASE_BUNDLE_PERSISTER, elementTypeName)

    override fun onWriteDefinition(typeBuilder: TypeSpec.Builder) {
        super.onWriteDefinition(typeBuilder)

        typeBuilder.apply {
            `private static final field`(String::class, BASE_KEY) { `=`("${elementName}Persister".S) }

            persistenceFields.forEach { it.writeFields(typeBuilder) }

            val constructorCode = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)

            // only add constructor if its actively used.
            var count = 0
            persistenceFields.forEach { if (it.writeForConstructor(constructorCode)) count++ }
            if (count > 0) typeBuilder.addMethod(constructorCode.build())

            if (!argument) {
                `fun`(TypeName.VOID, "persist", param(elementTypeName!!, defaultParam),
                        param(BUNDLE, "bundle"), param(String::class, uniqueBaseKey)) {
                    modifiers(public, final)
                    `if`("bundle == null || $defaultParam == null") {
                        `return`("")
                    }.end()
                    statement("bundle.putString($uniqueBaseKey + $BASE_KEY, \"\")")
                    persistenceFields.forEach { it.writePersistence(this@`fun`) }
                    this
                }
            }

            `fun`(elementTypeName!!, "unpack", param(elementTypeName!!, defaultParam),
                    param(BUNDLE, "bundle"), param(String::class, uniqueBaseKey)) {
                modifiers(public, final)
                `if`("bundle == null") {
                    `return`(null.L)
                }.end()

                if (!argument) {
                    `if`("bundle.containsKey($uniqueBaseKey + $BASE_KEY)") {
                        `if`("$defaultParam == null") {
                            statement("$defaultParam = new \$T()", elementTypeName)
                        }.end()
                        persistenceFields.forEach { it.writeUnpack(this) }
                        this
                    }.end()
                } else {
                    persistenceFields.forEach { it.writeUnpack(this) }
                }
                `return`(defaultParam.L)
            }
        }
    }
}