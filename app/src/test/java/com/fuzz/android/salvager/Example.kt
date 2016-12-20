package com.fuzz.android.salvager

import android.os.Bundle
import com.fuzz.android.salvage.BundlePersister
import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistField
import java.io.Serializable

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
@Persist
data class Example(@PersistField(bundlePersister = CustomStringPersister::class)
                   var name: String? = null,
                   var age: Int? = null,
                   var charSequence: Array<CharSequence>? = null,
                   var serializable: SimpleSerializable? = null)

class SimpleSerializable : Serializable

@Persist
data class ParentObject(var example: Example? = null)

@Persist
data class ListExample(var list: List<ParentObject> = arrayListOf(),
                       var listString: List<String> = arrayListOf(),
                       var listSerializable: List<SimpleSerializable> = arrayListOf())

@Persist
data class MapExample(var map: Map<String, ParentObject> = mutableMapOf())

class CustomStringPersister : BundlePersister<String> {
    override fun persist(obj: String?, bundle: Bundle, uniqueBaseKey: String) {
        bundle.putString(uniqueBaseKey + "candy", obj)
    }

    override fun unpack(`object`: String?, bundle: Bundle, uniqueBaseKey: String): String {
        return bundle.getString(uniqueBaseKey + "candy")
    }
}

data class MapListExample(var mapList: Map<String, List<ParentObject>>)