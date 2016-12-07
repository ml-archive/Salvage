package com.fuzz.android.salvage

import com.squareup.javapoet.TypeName
import java.io.Serializable

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
object ClassLookupMap {

    val map: Map<TypeName, String> = mapOf(
            TypeName.BOOLEAN to "Boolean",
            TypeName.INT to "Int",
            TypeName.CHAR to "Char",
            TypeName.LONG to "Long",
            TypeName.BYTE to "Byte",
            TypeName.DOUBLE to "Double",
            TypeName.FLOAT to "Float",
            TypeName.SHORT to "Short",
            TypeName.get(Serializable::class.java) to "Serializable",
            TypeName.get(String::class.java) to "String")

    fun valueForType(typeName: TypeName): String? {
        var type = map[typeName]
        if (type == null) {
            type = map[typeName.unbox()]
        }
        return type
    }
}