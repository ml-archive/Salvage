package com.fuzz.android.salvage

import android.os.Bundle
import java.util.HashMap

@Suppress("UNCHECKED_CAST")
/**
 * Description: The top-level entrance to the generated classes in this library.
 * @author Andrew Grosner (Fuzz)
 */
object Salvager {

    private val persisterMap = HashMap<Class<*>, BundlePersister<*>>()

    /**
     * Attempts to retrieve the [BundlePersister] if one exists for this class. If the [BundlePersister]
     * is not already cached, we cache for fast lookup next time. The corresponding [BundlePersister] must
     * have a public, empty, default constructor.
     * @throws RuntimeException when one does not exist for class passed.
     */
    @JvmStatic
    fun <T> getBundlePersister(tClass: Class<T>): BundlePersister<T> {
        var persister: BundlePersister<*>? = persisterMap[tClass]
        if (persister == null) {
            try {
                persister = Class.forName(tClass.name + "Persister")
                        .newInstance() as BundlePersister<*>
            } catch (e: Exception) {
                throw RuntimeException("Could not find generated BundlePersister for: \$clazz. Ensure " + "you specified the @Persist annotation.")
            }

            persisterMap.put(tClass, persister)
        }
        return persister as BundlePersister<T>
    }

    /**
     * Save your state here.
     * [obj] the nullable object to save. If null we ignore saving
     * [bundle] the bundle to save. If null we ignore saving
     * [uniqueBaseKey] default is "". If specified it will adjust every key of every object saved
     * by prepending this key to the base. This is to ensure we can store any nested object in same bundle.
     * By default you should not use this method without a corresponding call to [onRestoreInstanceState]
     */
    @JvmStatic
    fun <T : Any> onSaveInstanceState(obj: T?, bundle: Bundle?, uniqueBaseKey: String = "") {
        if (obj == null || bundle == null) {
            return
        }
        getBundlePersister(obj.javaClass).persist(obj, bundle, uniqueBaseKey)
    }

    /**
     * Restore your state here.
     * [obj] the nullable object to restore. If null we ignore restoring. So ensure its not null.
     * [bundle] the bundle to restore. If null we ignore restoring.
     * [uniqueBaseKey] default is "". If specified it will adjust every key of every object saved
     * by prepending this key to the base. This is to ensure we can restore any nested object in same bundle.
     * By default you should not use this method without a corresponding call to [onSaveInstanceState]
     */
    @JvmStatic
    fun <T : Any> onRestoreInstanceState(obj: T?, bundle: Bundle?, uniqueBaseKey: String = "") {
        if (obj == null || bundle == null) {
            return
        }
        getBundlePersister(obj.javaClass).unpack(obj, bundle, uniqueBaseKey)
    }

}
