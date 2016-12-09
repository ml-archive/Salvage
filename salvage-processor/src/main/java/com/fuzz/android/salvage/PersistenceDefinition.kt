package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.raizlabs.android.dbflow.processor.utils.ElementUtility
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
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

    val inlineBundles: Boolean

    init {
        setOutputClassName("Persister")

        val persistenceAnnotation: Persist? = typeElement.getAnnotation(Persist::class.java)
        if (persistenceAnnotation != null) {
            inlineBundles = persistenceAnnotation.inlineBundles;
        } else {
            inlineBundles = true;
        }

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

    override val implementsClasses: Array<TypeName>
        get() = arrayOf(ParameterizedTypeName.get(BUNDLE_PERSISTER, elementTypeName))

    override fun onWriteDefinition(typeBuilder: TypeSpec.Builder) {
        super.onWriteDefinition(typeBuilder)

        typeBuilder.addField(FieldSpec.builder(String::class.java, BASE_KEY, Modifier.PRIVATE,
                Modifier.STATIC, Modifier.FINAL)
                .initializer("\"$packageName.${elementName}Persister:\"")
                .build())

        persistenceFields.forEach { it.writeFieldDefinition(typeBuilder) }

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

        persistenceFields.forEach { it.writePersistence(persistMethod, inlineBundles) }

        typeBuilder.addMethod(persistMethod.build())

        val unpackMethod = MethodSpec.methodBuilder("unpack")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(elementTypeName, defaultParam)
                .addParameter(BUNDLE, "bundle")
                .addParameter(String::class.java, uniqueBaseKey)
                .returns(elementTypeName)
                .addCode(CodeBlock.builder() // if objects null return
                        .beginControlFlow("if (bundle == null || \$L == null)", defaultParam)
                        .addStatement("return \$L", defaultParam)
                        .endControlFlow()
                        .build())

        persistenceFields.forEach { it.writeUnpack(unpackMethod, inlineBundles) }

        unpackMethod.addStatement("return \$L", defaultParam)

        typeBuilder.addMethod(unpackMethod.build())
    }
}