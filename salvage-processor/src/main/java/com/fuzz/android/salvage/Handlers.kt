package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistArguments
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

/**
 * Description: The main base-level handler for performing some action when the
 * [Processor.process] is called.
 */
interface Handler {

    /**
     * Called when the process of the [Processor] is called

     * @param processorManager The manager that holds processing information
     * *
     * @param roundEnvironment The round environment
     */
    fun handle(processorManager: ProcessorManager, roundEnvironment: RoundEnvironment)
}

/**
 * Description: The base handler than provides common callbacks into processing annotated top-level elements
 */
abstract class BaseHandler : Handler {

    override fun handle(processorManager: ProcessorManager, roundEnvironment: RoundEnvironment) {
        val annotatedElements = annotationClass.map { roundEnvironment.getElementsAnnotatedWith(it) }.flatten()
        processElements(processorManager, annotatedElements)
        if (annotatedElements.isNotEmpty()) {
            annotatedElements.forEach { onProcessElement(processorManager, it) }
        }
    }

    protected abstract val annotationClass: Array<Class<out Annotation>>

    open fun processElements(processorManager: ProcessorManager, annotatedElements: List<Element>) {

    }

    protected abstract fun onProcessElement(processorManager: ProcessorManager, element: Element)
}

class PersistenceHandler : BaseHandler() {
    override val annotationClass: Array<Class<out Annotation>>
        get() = arrayOf(Persist::class.java, PersistArguments::class.java)

    override fun onProcessElement(processorManager: ProcessorManager, element: Element) {
        val persistenceDefinition = PersistenceDefinition(element as TypeElement, processorManager)

        processorManager.persistenceDefinitions.add(persistenceDefinition)
    }

}