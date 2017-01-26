package com.fuzz.android.salvage

import android.os.Build
import android.os.Bundle
import java.io.Serializable

class IntPersister : BundlePersister<Int> {
    override fun persist(obj: Int?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putInt(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Int?, bundle: Bundle, uniqueBaseKey: String): Int? {
        return bundle.getInt(uniqueBaseKey, `object` ?: 0)
    }
}

class DoublePersister : BundlePersister<Double> {
    override fun persist(obj: Double?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putDouble(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Double?, bundle: Bundle, uniqueBaseKey: String): Double? {
        return bundle.getDouble(uniqueBaseKey, `object` ?: 0.0)
    }
}

class FloatPersister : BundlePersister<Float> {
    override fun persist(obj: Float?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putFloat(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Float?, bundle: Bundle, uniqueBaseKey: String): Float? {
        return bundle.getFloat(uniqueBaseKey, `object` ?: 0.0f)
    }
}

class CharPersister : BundlePersister<Char> {
    override fun persist(obj: Char?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putChar(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Char?, bundle: Bundle, uniqueBaseKey: String): Char? {
        return bundle.getChar(uniqueBaseKey, `object` ?: Character.MIN_VALUE)
    }
}

class ShortPersister : BundlePersister<Short> {
    override fun persist(obj: Short?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putShort(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Short?, bundle: Bundle, uniqueBaseKey: String): Short? {
        return bundle.getShort(uniqueBaseKey, `object` ?: 0)
    }
}

class BytePersister : BundlePersister<Byte> {
    override fun persist(obj: Byte?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putByte(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Byte?, bundle: Bundle, uniqueBaseKey: String): Byte? {
        return bundle.getByte(uniqueBaseKey, `object` ?: 0)
    }
}

class LongPersister : BundlePersister<Long> {
    override fun persist(obj: Long?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putLong(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: Long?, bundle: Bundle, uniqueBaseKey: String): Long? {
        return bundle.getLong(uniqueBaseKey, `object` ?: 0)
    }
}

class BundleBundlePersister : BundlePersister<Bundle> {
    override fun persist(obj: Bundle?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putBundle(uniqueBaseKey, bundle) }
    }

    override fun unpack(`object`: Bundle?, bundle: Bundle, uniqueBaseKey: String): Bundle? {
        return bundle.getBundle(uniqueBaseKey)
    }
}

class CharSequencePersister : BundlePersister<CharSequence> {
    override fun persist(obj: CharSequence?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putCharSequence(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: CharSequence?, bundle: Bundle, uniqueBaseKey: String): CharSequence? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bundle.getCharSequence(uniqueBaseKey, `object` ?: "")
        } else {
            return bundle.getCharSequence(uniqueBaseKey)
        }
    }
}

class StringPersister : BundlePersister<String> {
    override fun persist(obj: String?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putString(uniqueBaseKey, obj) }
    }

    override fun unpack(`object`: String?, bundle: Bundle, uniqueBaseKey: String): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bundle.getString(uniqueBaseKey, `object` ?: "")
        } else {
            return bundle.getString(uniqueBaseKey)
        }
    }
}

class SerializablePersister<T : Serializable> : BundlePersister<T> {
    override fun persist(obj: T?, bundle: Bundle, uniqueBaseKey: String) {
        obj?.let { bundle.putSerializable(uniqueBaseKey, obj) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun unpack(`object`: T?, bundle: Bundle, uniqueBaseKey: String): T? {
        return bundle.getSerializable(uniqueBaseKey) as T? ?: `object`
    }
}