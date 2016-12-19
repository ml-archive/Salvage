package com.fuzz.android.salvage

import android.os.Bundle
import java.util.*

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */
abstract class BaseBundlePersister<T> : BundlePersister<T> {

    protected fun <T : Any> persistList(list: List<T>?, bundle: Bundle, uniqueBaseKey: String,
                                        fieldKey: String, bundlePersister: BundlePersister<T>) {
        if (list != null) {
            val count = list.size
            bundle.putInt(uniqueBaseKey + fieldKey + ":count", count)
            for (i in 0..count - 1) {
                bundlePersister.persist(list[i], bundle, uniqueBaseKey + fieldKey + i)
            }
        }
    }

    protected fun <T : Any> restoreList(bundle: Bundle, uniqueBaseKey: String,
                                        fieldKey: String, bundlePersister: BundlePersister<T>): List<T>? {
        val count = bundle.getInt(uniqueBaseKey + fieldKey + ":count", 0)
        if (count > 0) {
            val list = ArrayList<T>()
            for (i in 0..count - 1) {
                val item = bundlePersister.unpack(null, bundle, uniqueBaseKey + fieldKey + i)
                if (item != null) {
                    list.add(item)
                }
            }
            return list
        }
        return null
    }

}