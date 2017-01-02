package com.fuzz.android.salvage

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import java.io.Serializable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

val defaultParam = "obj"

@Throws(IOException::class)
fun writePackageHelper(processingEnvironment: ProcessingEnvironment,
                       baseDefinition: BaseDefinition,
                       packagePrivateList: List<PersistenceField>,
                       manager: ProcessorManager) {
    var count = 0

    if (!packagePrivateList.isEmpty()) {
        val typeBuilder = TypeSpec.classBuilder("${baseDefinition.elementClassName?.simpleName()}_PersistenceHelper")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)

        for (persistenceField in packagePrivateList) {
            var helperClassName = "${manager.elements.getPackageOf(persistenceField.element)}" +
                    ".${ClassName.get(persistenceField.element.enclosingElement as TypeElement).simpleName()}_PersistenceHelper"

            val className = ElementUtility.getClassName(helperClassName, manager)

            if (className != null && PackagePrivateScopeAccessor.containsField(className,
                    persistenceField.elementName)) {

                var method: MethodSpec.Builder = MethodSpec
                        .methodBuilder("get${persistenceField.elementName.capitalizeFirstLetter()}")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addParameter(baseDefinition.elementTypeName, defaultParam)
                        .returns(persistenceField.elementTypeName)
                val samePackage = ElementUtility.isInSamePackage(manager, persistenceField.element,
                        baseDefinition.element)

                if (samePackage) {
                    method.addStatement("return \$L.\$L", defaultParam, persistenceField.elementName)
                } else {
                    method.addStatement("return \$T.get\$L(\$L)", className,
                            persistenceField.elementName.capitalizeFirstLetter(),
                            defaultParam)
                }

                typeBuilder.addMethod(method.build())

                method = MethodSpec.methodBuilder("set" + persistenceField.elementName.capitalizeFirstLetter())
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addParameter(baseDefinition.elementTypeName, defaultParam)
                        .addParameter(persistenceField.elementTypeName, "var")

                if (samePackage) {
                    method.addStatement("\$L.\$L = \$L", defaultParam,
                            persistenceField.elementName, "var")
                } else {

                    method.addStatement("\$T.set\$L(\$L, \$L)", className,
                            persistenceField.elementName.capitalizeFirstLetter(),
                            defaultParam, "var")
                }
                typeBuilder.addMethod(method.build())
                count++
            } else if (className == null) {
                manager.logError("Could not find classname for:" + helperClassName)
            }
        }

        // only write class if we have referenced fields.
        if (count > 0) {
            val javaFileBuilder = JavaFile.builder(baseDefinition.packageName, typeBuilder.build())
            javaFileBuilder.build().writeTo(processingEnvironment.filer)
        }
    }
}

fun writeBaseDefinition(baseDefinition: BaseDefinition, processorManager: ProcessorManager): Boolean {
    var success = false
    try {
        val javaFile = JavaFile.builder(baseDefinition.packageName, baseDefinition.typeSpec).build()
        javaFile.writeTo(processorManager.processingEnvironment.filer)
        success = true
    } catch (e: IOException) {
        // ignored
    } catch (i: IllegalStateException) {
        processorManager.logError("Found error for class:" + baseDefinition.elementName)
    }

    return success
}

/**
 * Whether the specified element is assignable to the fqTn parameter

 * @param processingEnvironment The environment this runs in
 * *
 * @param fqTn                  THe fully qualified type name of the element we want to check
 * *
 * @param element               The element to check that implements
 * *
 * @return true if element implements the fqTn
 */
fun implementsClass(processingEnvironment: ProcessingEnvironment, fqTn: String, element: TypeElement?): Boolean {
    val typeElement = processingEnvironment.elementUtils.getTypeElement(fqTn)
    if (typeElement == null) {
        processingEnvironment.messager.printMessage(Diagnostic.Kind.ERROR, "Type Element was null for: " + fqTn + "" +
                "ensure that the visibility of the class is not private.")
        return false
    } else {
        var classMirror: TypeMirror? = typeElement.asType()
        if (classMirror != null) {
            classMirror = processingEnvironment.typeUtils.erasure(classMirror)
        }
        return classMirror != null && element != null && element.asType() != null &&
                processingEnvironment.typeUtils.isAssignable(element.asType(), classMirror)
    }
}

fun isSerializable(processorManager: ProcessorManager, element: TypeElement?)
        = implementsClass(processorManager.processingEnvironment, Serializable::class.java.name, element)
