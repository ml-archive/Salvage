package com.fuzz.android.salvage

import com.squareup.javapoet.ClassName

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

val PACKAGE = "com.fuzz.android.salvage"

val BUNDLE = ClassName.get("android.os", "Bundle")

val BUNDLE_PERSISTER = ClassName.get(PACKAGE, "BundlePersister")

val SALVAGER = ClassName.get(PACKAGE, "Salvager")