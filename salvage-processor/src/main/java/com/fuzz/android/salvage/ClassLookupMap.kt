package com.fuzz.android.salvage

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.WildcardTypeName
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
            BUNDLE to "Bundle",
            TypeName.get(CharSequence::class.java) to "CharSequence",
            ArrayTypeName.of(CharSequence::class.java) to "CharSequenceArray",
            ArrayTypeName.of(TypeName.BYTE) to "ByteArray",
            ArrayTypeName.of(TypeName.CHAR) to "CharArray",
            ArrayTypeName.of(TypeName.INT) to "IntArray",
            ArrayTypeName.of(TypeName.BOOLEAN) to "BooleanArray",
            ArrayTypeName.of(TypeName.DOUBLE) to "DoubleArray",
            ArrayTypeName.of(TypeName.FLOAT) to "FloatArray",
            ArrayTypeName.of(TypeName.SHORT) to "ShortArray",
            TypeName.get(Serializable::class.java) to "Serializable",
            TypeName.get(String::class.java) to "String")

    fun valueForType(typeName: TypeName, isSerializable: Boolean): String? {
        var newTypeName = typeName
        if (typeName is WildcardTypeName) {
            newTypeName = typeName.upperBounds[0]
        }
        var type = lookupTypeInMap(newTypeName)
        // if serializable last and not specified, get the name for serializable
        if (type == null && isSerializable) {
            type = map[TypeName.get(Serializable::class.java)]
        }
        return type
    }

    fun hasType(typeName: TypeName): Boolean {
        return lookupTypeInMap(typeName) != null
    }

    private fun lookupTypeInMap(typeName: TypeName): String? {
        var type = map[typeName]
        if (type == null) {
            try {
                type = map[typeName.unbox()]
            } catch (u: UnsupportedOperationException) {
                // ignore
            }
        }
        return type
    }
}