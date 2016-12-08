package com.fuzz.android.salvager

import com.fuzz.android.salvage.core.Persist
import java.io.Serializable

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
@Persist
data class Example(var name: String? = null,
                   var age: Int? = null,
                   var charSequence: Array<CharSequence>? = null,
                   var serializable: SimpleSerializable? = null)

class SimpleSerializable : Serializable {

}

@Persist
data class ParentObject(var example: Example?)

@Persist
data class ListExample(var list: List<ParentObject>,
                       var listString: List<String>,
                       var listSerializable: List<Serializable>)