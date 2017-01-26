package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistField
import com.fuzz.android.salvage.core.PersistPolicy
import com.squareup.javapoet.*
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

    init {
        setOutputClassName("Persister")

        val persistenceAnnotation: Persist? = typeElement.getAnnotation(Persist::class.java)
        if (persistenceAnnotation != null) {
            persistPolicy = persistenceAnnotation.persistPolicy
        } else {
            persistPolicy = PersistPolicy.VISIBLE_FIELDS_AND_METHODS
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

        typeBuilder.addField(FieldSpec.builder(String::class.java, BASE_KEY, Modifier.PRIVATE,
                Modifier.STATIC, Modifier.FINAL)
                .initializer("\"$packageName.${elementName}Persister:\"")
                .build())

        persistenceFields.forEach { it.writeFields(typeBuilder) }

        val constructorCode = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)

        persistenceFields.forEach { it.writeForConstructor(constructorCode) }

        typeBuilder.addMethod(constructorCode.build())

        val persistMethod = MethodSpec.methodBuilder("persist")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(elementTypeName, defaultParam)
                .addParameter(BUNDLE, "bundle")
                .addParameter(String::class.java, uniqueBaseKey)
                .addCode(CodeBlock.builder() // if objects null return
                        .beginControlFlow("if (bundle == null || \$L == null)", defaultParam)
                        .addStatement("return")
                        .endControlFlow()
                        .build())


        // persist identity key
        persistMethod.addStatement(CodeBlock.of("bundle.putString($uniqueBaseKey + $BASE_KEY, \"\")"))

        persistenceFields.forEach { it.writePersistence(persistMethod) }

        typeBuilder.addMethod(persistMethod.build())

        val unpackMethod = MethodSpec.methodBuilder("unpack")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(elementTypeName, defaultParam)
                .addParameter(BUNDLE, "bundle")
                .addParameter(String::class.java, uniqueBaseKey)
                .returns(elementTypeName)
                .addCode(CodeBlock.builder() // if objects null return
                        .beginControlFlow("if (bundle == null)")
                        .addStatement("return null")
                        .endControlFlow()
                        .build())

        unpackMethod.beginControlFlow("if (bundle.containsKey($uniqueBaseKey + $BASE_KEY))")
                .beginControlFlow("if ($defaultParam == null)")
                .addStatement("$defaultParam = new \$T()", elementTypeName)
                .endControlFlow()
        persistenceFields.forEach { it.writeUnpack(unpackMethod) }
        unpackMethod.endControlFlow()

        unpackMethod.addStatement("return \$L", defaultParam)

        typeBuilder.addMethod(unpackMethod.build())
    }
}