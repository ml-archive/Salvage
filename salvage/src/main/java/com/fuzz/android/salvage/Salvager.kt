package com.fuzz.android.salvage

import android.os.Bundle
import java.io.Serializable
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
/**
 * Description: The top-level entrance to the generated classes in this library.
 * @author Andrew Grosner (Fuzz)
 */
object Salvager {

    private val persisterMap: MutableMap<Class<*>, BundlePersister<*>>
            = mutableMapOf(Int::class.java to IntPersister(),
            Double::class.java to DoublePersister(),
            Float::class.java to FloatPersister(),
            Char::class.java to CharPersister(),
            Short::class.java to ShortPersister(),
            Byte::class.java to BytePersister(),
            Long::class.java to LongPersister(),
            Bundle::class.java to BundleBundlePersister(),
            CharSequence::class.java to CharSequencePersister(),
            String::class.java to StringPersister(),
            Serializable::class.java to SerializablePersister<Serializable>())

    /**
     * Attempts to retrieve the [BundlePersister] if one exists for this class. If the [BundlePersister]
     * is not already cached, we cache for fast lookup next time. The corresponding [BundlePersister] must
     * have a public, empty, default constructor.
     * @throws RuntimeException when one does not exist for class passed.
     */
    @JvmStatic
    fun <T> getBundlePersister(tClass: Class<T>): BundlePersister<T> {
        var persister: BundlePersister<*>? = persisterMap[tClass]
        if (persister == null) {
            try {
                persister = Class.forName(tClass.name + "Persister")
                        .newInstance() as BundlePersister<*>
            } catch (e: Exception) {
                throw RuntimeException("Could not find generated BundlePersister for: $tClass. " +
                        "Ensure you specified the @Persist annotation.")
            }

            persisterMap.put(tClass, persister)
        }
        return persister as BundlePersister<T>
    }

    /**
     * Load your arguments state.
     * [obj] the Fragment or Activity object to restore.
     * [bundle] the bundle to restore. If null we ignore restoring.
     */
    @JvmStatic
    fun <T : Any> loadArguments(obj: T, bundle: Bundle?) {
        onRestoreInstanceState(obj, bundle)
    }

    /**
     * Save your state here.
     * [obj] the nullable object to save. If null we ignore saving
     * [bundle] the bundle to save. If null we ignore saving
     */
    @JvmStatic
    fun <T : Any> onSaveInstanceState(obj: T?, bundle: Bundle?) {
        if (obj != null && bundle != null) {
            getBundlePersister(obj.javaClass).persist(obj, bundle)
        }
    }

    /**
     * Save your state here.
     * [obj] the nullable object to save. If null we ignore saving
     * [bundle] the bundle to save. If null we ignore saving
     */
    @JvmStatic
    fun <T : Any> onSaveInstanceState(obj: T?): Bundle {
        val bundle = Bundle()
        if (obj != null) {
            getBundlePersister(obj.javaClass).persist(obj, bundle)
        }
        return bundle
    }

    /**
     * Restore your state here.
     * [obj] the nullable object to restore. If null we ignore restoring. So ensure its not null.
     * [bundle] the bundle to restore. If null we ignore restoring.
     */
    @JvmStatic
    fun <T : Any> onRestoreInstanceState(obj: T?, bundle: Bundle?): T? {
        return if (obj != null && bundle != null) {
            getBundlePersister(obj.javaClass).unpack(obj, bundle)
        } else {
            obj
        }
    }


    /**
     * Restore your state here.
     * [clazz] the Class of the object to restore. Will return a new instance if [obj] is null.
     * [bundle] the bundle to restore. If null we ignore restoring.
     * [obj] the optional instance object to include that can be reused.
     */
    @JvmStatic
    @JvmOverloads
    fun <T : Any> onRestoreInstanceState(clazz: Class<T>, bundle: Bundle?, obj: T? = null): T? {
        return bundle?.let { getBundlePersister(clazz).unpack(obj, bundle) }
    }

    /**
     * Restore your state here.
     * [klazz] the Class of the object to restore. Will return a new instance if [obj] is null.
     * [bundle] the bundle to restore. If null we ignore restoring.
     * [obj] the optional instance object to include that can be reused.
     */
    fun <T : Any> onRestoreInstanceState(klazz: KClass<T>, bundle: Bundle?, obj: T? = null): T? {
        return onRestoreInstanceState(klazz.java, bundle, obj)
    }

}
