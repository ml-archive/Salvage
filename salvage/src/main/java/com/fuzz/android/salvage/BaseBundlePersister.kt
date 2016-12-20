package com.fuzz.android.salvage

import android.os.Bundle
import java.util.ArrayList

/**
 * Description: Provides some helper methods to the implementations in generated code.
 *
 * @author Andrew Grosner (fuzz)
 */
abstract class BaseBundlePersister<T> : BundlePersister<T> {

    protected fun <T : Any> persistList(list: List<T>?, bundle: Bundle, uniqueBaseKey: String,
                                        fieldKey: String, bundlePersister: BundlePersister<T>) {
        if (list != null) {
            val count = list.size
            bundle.putInt(uniqueBaseKey + fieldKey + ":count", count)
            (0..count - 1).forEach {
                bundlePersister.persist(list[it], bundle, uniqueBaseKey + fieldKey + it)
            }
        }
    }

    protected fun <T : Any> restoreList(bundle: Bundle, uniqueBaseKey: String,
                                        fieldKey: String, bundlePersister: BundlePersister<T>): List<T>? {
        val count = bundle.getInt(uniqueBaseKey + fieldKey + ":count", 0)
        if (count > 0) {
            return (0..count - 1).mapNotNull {
                bundlePersister.unpack(null, bundle, uniqueBaseKey + fieldKey + it)
            }
        }
        return null
    }

    protected fun <K : Any, V : Any> persistMap(
            map: Map<K, V?>?, bundle: Bundle,
            uniqueBaseKey: String, fieldKey: String,
            keyBundlePersister: BundlePersister<K>,
            variableBundlePersister: BundlePersister<V>): Map<K, V?>? {
        if (map != null) {
            val count = map.size
            bundle.putInt(uniqueBaseKey + fieldKey + ":count", count)
            val entries = ArrayList(map.entries)
            (0..count - 1).forEach {
                val (key, value) = entries[it]
                keyBundlePersister.persist(key, bundle, uniqueBaseKey + fieldKey + it + ":key")
                variableBundlePersister.persist(value, bundle, uniqueBaseKey + fieldKey + it + ":value")
            }
        }
        return map
    }

    protected fun <K : Any, V : Any> restoreMap(
            bundle: Bundle, uniqueBaseKey: String,
            fieldKey: String,
            keyBundlePersister: BundlePersister<K>,
            variableBundlePersister: BundlePersister<V>): Map<K, V?>? {
        val count = bundle.getInt(uniqueBaseKey + fieldKey + ":count", 0)
        if (count > 0) {
            val map: MutableMap<K, V?> = mutableMapOf()
            (0..count - 1).forEach {
                val key = keyBundlePersister.unpack(null, bundle, uniqueBaseKey + fieldKey + it + ":key")
                if (key != null) {
                    val value = variableBundlePersister.unpack(null, bundle,
                            uniqueBaseKey + fieldKey + it + ":value")
                    map.put(key, value)
                }
            }
            return map
        }
        return null
    }

}