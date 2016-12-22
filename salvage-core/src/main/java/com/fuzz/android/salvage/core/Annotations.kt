package com.fuzz.android.salvage.core

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Persist

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class PersistIgnore

/**
 * Not required, but when specified, you can override a few values here to configure each field.
 *
 * [bundlePersister] Allows you to specify a Bundle Persister that overrides default implementation.
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class PersistField(val bundlePersister: KClass<*> = Any::class,
                              val getterName: String = "",
                              val setterName: String = "")
