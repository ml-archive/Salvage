package com.fuzz.android.salvage

import com.squareup.javapoet.ClassName

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

val BUNDLE = ClassName.get("android.os", "Bundle")

val BUNDLE_PERSISTER = ClassName.get("com.fuzz.android.salvage", "BundlePersister")