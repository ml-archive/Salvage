package com.fuzz.android.salvage

import android.os.Bundle

/**
 * Description: The main interface by which objects get persisted. Implement this class
 * to provide your own implementation for persistence.

 * @author Andrew Grosner (Fuzz)
 */
interface BundlePersister<T> {

    fun persist(obj: T, bundle: Bundle, uniqueBaseKey: String)

    fun unpack(`object`: T, bundle: Bundle, uniqueBaseKey: String)
}
