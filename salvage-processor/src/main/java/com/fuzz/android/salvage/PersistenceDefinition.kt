package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.raizlabs.android.dbflow.processor.utils.ElementUtility
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

    init {
        setOutputClassName("Persister")

        val persistenceAnnotation: Persist? = typeElement.getAnnotation(Persist::class.java)

        val elements = ElementUtility.getAllElements(typeElement, manager)

        elements.forEach {

            val isValidField = ElementUtility.isValidAllFields(true, it)

            // package private, will generate helper
            val isPackagePrivate = ElementUtility.isPackagePrivate(it)
            val isPackagePrivateNotInSamePackage = isPackagePrivate &&
                    !ElementUtility.isInSamePackage(manager, it, typeElement)

            if (isValidField) {
                val persistenceField = PersistenceField(manager, it, isPackagePrivateNotInSamePackage)

                persistenceFields.add(persistenceField)
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
                        .beginControlFlow("if ($defaultParam == null)")
                        .addStatement("$defaultParam = new \$T()", elementTypeName)
                        .endControlFlow()
                        .build())

        persistenceFields.forEach { it.writeUnpack(unpackMethod) }

        unpackMethod.addStatement("return \$L", defaultParam)

        typeBuilder.addMethod(unpackMethod.build())
    }
}