package com.fuzz.android.salvage

import android.os.Bundle

/**
 * Description: The main interface by which objects get persisted. Implement this class
 * to provide your own implementation for persistence.

 * @author Andrew Grosner (Fuzz)
 */
interface BundlePersister<T> {

    /**
     * Called when data is persisted to a [Bundle]. The [uniqueBaseKey] should be appended
     * first to the custom key you use in the [Bundle].
     */
    fun persist(obj: T?, bundle: Bundle, uniqueBaseKey: String = "")

    /**
     * Called when data is restored. If the [object] is not null, reuse the object if you can
     * to lessen garbage collection impact.
     */
    fun unpack(`object`: T?, bundle: Bundle, uniqueBaseKey: String = ""): T?
}
