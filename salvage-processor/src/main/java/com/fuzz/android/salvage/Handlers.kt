package com.fuzz.android.salvage

import com.fuzz.android.salvage.core.Persist
import com.google.common.collect.Sets
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
abstract class BaseHandler<AnnotationClass : Annotation> : Handler {

    override fun handle(processorManager: ProcessorManager, roundEnvironment: RoundEnvironment) {
        val annotatedElements = Sets.newHashSet(roundEnvironment.getElementsAnnotatedWith(annotationClass))
        processElements(processorManager, annotatedElements)
        if (annotatedElements.size > 0) {
            annotatedElements.forEach { onProcessElement(processorManager, it) }
        }
    }

    protected abstract val annotationClass: Class<AnnotationClass>

    open fun processElements(processorManager: ProcessorManager, annotatedElements: MutableSet<Element>) {

    }

    protected abstract fun onProcessElement(processorManager: ProcessorManager, element: Element)
}

class PersistenceHandler : BaseHandler<Persist>() {
    override val annotationClass: Class<Persist>
        get() = Persist::class.java

    override fun onProcessElement(processorManager: ProcessorManager, element: Element) {
        val persistenceDefinition = PersistenceDefinition(element as TypeElement, processorManager)

        processorManager.persistenceDefinitions.add(persistenceDefinition)
    }

}