package com.fuzz.android.salvage

import javax.annotation.processing.FilerException
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
class ProcessorManager(val processingEnvironment: ProcessingEnvironment) : Handler {

    val messager: Messager = processingEnvironment.messager

    val typeUtils: Types = processingEnvironment.typeUtils

    val elements: Elements = processingEnvironment.elementUtils

    val persistenceDefinitions: MutableList<PersistenceDefinition> = arrayListOf()

    val handlers: List<Handler> = mutableListOf(PersistenceHandler())


    fun logError(callingClass: KClass<*>?, error: String?, vararg args: Any?) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format("*==========*$callingClass :" + error?.trim { it <= ' ' } + "*==========*", *args))
        var stackTraceElements = Thread.currentThread().stackTrace
        if (stackTraceElements.size > 8) {
            stackTraceElements = stackTraceElements.copyOf(8)
        }
        for (stackTrace in stackTraceElements) {
            messager.printMessage(Diagnostic.Kind.ERROR, stackTrace.toString())
        }
    }

    fun logError(error: String?, vararg args: Any?) = logError(callingClass = null, error = error, args = args)

    fun logWarning(error: String, vararg args: Any?) {
        messager.printMessage(Diagnostic.Kind.WARNING, String.format("*==========*\n$error\n*==========*", *args))
    }

    fun logWarning(callingClass: KClass<*>, error: String, vararg args: Any?) {
        logWarning("$callingClass : $error", *args)
    }

    override fun handle(processorManager: ProcessorManager, roundEnvironment: RoundEnvironment) {
        handlers.forEach { it.handle(processorManager, roundEnvironment) }

        persistenceDefinitions.forEach {
            writeBaseDefinition(it, this)
            try {
                it.writePackageHelper(processingEnvironment)
            } catch (e: FilerException) { /*Ignored intentionally to allow multi-round table generation*/
            }
        }
    }
}