package com.fuzz.android.salvage

import android.os.Bundle
import java.util.*

@Suppress("UNCHECKED_CAST")
/**
 * Description:

 * @author Andrew Grosner (Fuzz)
 */

object Salvager {

    private val persisterMap = HashMap<Class<*>, BundlePersister<*>>()

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

    @JvmStatic
    fun <T : Any> onSaveInstanceState(obj: T?, bundle: Bundle?, uniqueBaseKey: String = "") {
        if (obj == null || bundle == null) {
            return
        }
        getBundlePersister(obj.javaClass).persist(obj, bundle, uniqueBaseKey)
    }

    @JvmStatic
    fun <T : Any> onRestoreInstanceState(obj: T?, bundle: Bundle?, uniqueBaseKey: String = "") {
        if (obj == null || bundle == null) {
            return
        }
        getBundlePersister(obj.javaClass).unpack(obj, bundle, uniqueBaseKey)
    }

}
