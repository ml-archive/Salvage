package com.fuzz.android.salvage

import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.raizlabs.android.dbflow.processor.utils.ElementUtility
import com.raizlabs.android.dbflow.processor.utils.capitalizeFirstLetter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

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

            // TODO: nested fields?
            /*if (persistenceField is ForeignKeyColumnDefinition) {
                val tableDefinition: TableDefinition? = databaseDefinition?.objectHolder?.tableDefinitionMap?.get(persistenceField.referencedTableClassName as TypeName)
                if (tableDefinition != null) {
                    helperClassName = manager.elements.getPackageOf(tableDefinition.element).toString() +
                            "." + ClassName.get(tableDefinition.element as TypeElement).simpleName() +
                            databaseDefinition?.classSeparator + "Helper"
                }
            }*/

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