package com.fuzz.android.salvage

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

val PACKAGE = "com.fuzz.android.salvage"

val BUNDLE = ClassName.get("android.os", "Bundle")

val BUNDLE_PERSISTER = ClassName.get(PACKAGE, "BundlePersister")

val BASE_BUNDLE_PERSISTER = ClassName.get(PACKAGE, "BaseBundlePersister")

val SERIALIZABLE_PERSISTER = ClassName.get(PACKAGE, "SerializablePersister")

val SALVAGER = ClassName.get(PACKAGE, "Salvager")