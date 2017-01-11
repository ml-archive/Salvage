package com.fuzz.android.salvage.core

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Persist(val persistPolicy: PersistPolicy = PersistPolicy.VISIBLE_FIELDS_AND_METHODS)

enum class PersistPolicy {

    /**
     * Grabs any package private, public, or visible accessors for private fields.
     * This is the default.
     */
    VISIBLE_FIELDS_AND_METHODS,

    /**
     * Package private or public fields are only included.
     */
    VISIBLE_FIELDS_ONLY,

    /**
     * Any annotated field with [PersistField]. If they are private + have associated accessors,
     * they're counted too.
     */
    ANNOTATIONS_ONLY,

    /**
     * Any private fields that have accessors.
     */
    PRIVATE_ACCESSORS_ONLY
}

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
                              val setterName: String = "",
                              val defaultValue: String = "")
