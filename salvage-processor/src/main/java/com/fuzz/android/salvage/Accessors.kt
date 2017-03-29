package com.fuzz.android.salvage

import com.google.common.collect.Maps
import com.grosner.kpoet.CodeMethod
import com.grosner.kpoet.L
import com.grosner.kpoet.code
import com.grosner.kpoet.statement
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import java.util.*
import java.util.List
import java.util.Map

/**
 * Description: Base interface for accessing fields
 *
 * @author Andrew Grosner (fuzz)
 */
abstract class Accessor(val propertyName: String?) {

    open val isPrimitiveTarget: Boolean = false

    abstract fun get(existingBlock: CodeBlock? = null, baseVariableName: String?): CodeBlock

    abstract fun set(existingBlock: CodeBlock? = null, baseVariableName: CodeBlock? = null): CodeBlock

    protected fun prependPropertyName(code: CodeBlock.Builder) {
        propertyName?.let {
            code.add("\$L.", propertyName)
        }
    }

    protected fun appendPropertyName(code: CodeBlock.Builder) {
        propertyName?.let {
            code.add(".\$L", propertyName)
        }
    }

    protected fun appendAccess(codeAccess: CodeMethod): CodeBlock {
        val codeBuilder = CodeBlock.builder()
        prependPropertyName(codeBuilder)
        codeAccess(codeBuilder)
        return codeBuilder.build()
    }
}

fun Accessor?.isPrimitiveTarget(): Boolean = this?.isPrimitiveTarget ?: true

interface GetterSetter {

    val getterName: String
    val setterName: String
}

class VisibleScopeAccessor(propertyName: String) : Accessor(propertyName) {

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?): CodeBlock {
        val codeBlock: CodeBlock.Builder = CodeBlock.builder()
        baseVariableName?.let { codeBlock.add("\$L.", baseVariableName) }
        return codeBlock.add("\$L = \$L", propertyName, existingBlock)
                .build()
    }

    override fun get(existingBlock: CodeBlock?, baseVariableName: String?): CodeBlock {
        val codeBlock: CodeBlock.Builder = CodeBlock.builder()
        existingBlock?.let { codeBlock.add("\$L.", existingBlock) }
        return codeBlock.add(propertyName)
                .build()
    }
}

class PrivateScopeAccessor(propertyName: String?, getterSetter: GetterSetter? = null,
                           private val isBoolean: Boolean = false,
                           private var getterName: String = "",
                           private var setterName: String = "") : Accessor(propertyName) {

    val getterNameElement: String by lazy {
        if (getterName.isNullOrEmpty()) {
            if (propertyName != null) {
                if (isBoolean && !propertyName.startsWith("is", ignoreCase = true)) {
                    "is" + propertyName.capitalizeFirstLetter()
                } else if (!isBoolean && !propertyName.startsWith("get", ignoreCase = true)) {
                    "get" + propertyName.capitalizeFirstLetter()
                } else propertyName.lower()
            } else {
                ""
            }
        } else getterName
    }

    val setterNameElement: String by lazy {
        if (propertyName != null) {
            var setElementName = propertyName
            if (setterName.isNullOrEmpty()) {
                if (!setElementName.startsWith("set", ignoreCase = true)) {
                    if (isBoolean && setElementName.startsWith("is")) {
                        setElementName = setElementName.replaceFirst("is".toRegex(), "")
                    } else if (isBoolean && setElementName.startsWith("Is")) {
                        setElementName = setElementName.replaceFirst("Is".toRegex(), "")
                    }
                    "set" + setElementName.capitalizeFirstLetter()
                } else setElementName.lower()
            } else setterName
        } else ""
    }

    init {
        getterSetter?.let {
            getterName = getterSetter.getterName
            setterName = getterSetter.setterName
        }
    }

    override fun get(existingBlock: CodeBlock?, baseVariableName: String?) = code {
        existingBlock?.let { this@code.add("$existingBlock.") }
        add("$getterNameElement()")
    }

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?) = code {
        baseVariableName?.let { add("$baseVariableName.") }
        add("$setterNameElement($existingBlock)")
    }
}

class PackagePrivateScopeAccessor(propertyName: String, packageName: String,
                                  tableClassName: String)
    : Accessor(propertyName) {

    val helperClassName = ClassName.get(packageName, "${tableClassName}_$classSuffix")!!

    override fun get(existingBlock: CodeBlock?, baseVariableName: String?)
            = CodeBlock.of("\$T.get${propertyName.capitalizeFirstLetter()}($existingBlock)",
            helperClassName)

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?)
            = CodeBlock.of("\$T.set${propertyName.capitalizeFirstLetter()}($baseVariableName, $existingBlock)",
            helperClassName)

    companion object {

        const val classSuffix = "PersistenceHelper"

        private val methodWrittenMap = Maps.newHashMap<ClassName, MutableList<String>>()

        fun containsField(className: ClassName, columnName: String): Boolean {
            return methodWrittenMap[className]?.contains(columnName) ?: false
        }

        /**
         * Ensures we only map and use a package private field generated access method if its necessary.
         */
        fun putElement(className: ClassName, elementName: String) {
            var list: MutableList<String>? = methodWrittenMap[className]
            if (list == null) {
                list = ArrayList<String>()
                methodWrittenMap.put(className, list)
            }
            if (!list.contains(elementName)) {
                list.add(elementName)
            }
        }
    }
}

/**
 * Used for complex [List] or [Map] or Nested types that are final, we don't reassign, we just
 * attempt to assign fields to it.
 */
class EmptyAccessor(propertyName: String? = null)
    : Accessor(propertyName) {
    override fun get(existingBlock: CodeBlock?, baseVariableName: String?): CodeBlock {
        return appendAccess { add(existingBlock) }
    }

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?): CodeBlock {
        return appendAccess { add(existingBlock) }
    }

}

class NestedAccessor(val persisterFieldName: String,
                     val keyFieldName: String,
                     val baseFieldAcessor: Accessor,
                     propertyName: String? = null) : Accessor(propertyName) {
    override fun get(existingBlock: CodeBlock?, baseVariableName: String?): CodeBlock {
        return appendAccess {
            statement("$persisterFieldName.persist($existingBlock, bundle, $uniqueBaseKey + $keyFieldName)")
        }
    }

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?)
            : CodeBlock {

        return appendAccess {
            statement(baseFieldAcessor.set(
                    CodeBlock.of("$persisterFieldName.unpack($existingBlock, bundle," +
                            " $uniqueBaseKey + $keyFieldName)"), baseVariableName).L)
        }
    }
}

class ListAccessor(val keyFieldName: String,
                   val baseFieldAcessor: Accessor,
                   val persisterFieldName: String,
                   propertyName: String? = null) : Accessor(propertyName) {

    override fun get(existingBlock: CodeBlock?, baseVariableName: String?): CodeBlock {
        return appendAccess {
            statement("persistList($existingBlock, bundle, $uniqueBaseKey, $keyFieldName, " +
                    "$persisterFieldName)")
        }
    }

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?): CodeBlock {
        return appendAccess {
            statement(baseFieldAcessor.set(
                    CodeBlock.of("restoreList($existingBlock, bundle, $uniqueBaseKey, $keyFieldName, " +
                            "$persisterFieldName)"),
                    baseVariableName).L)
        }
    }
}


class MapAccessor(val keyFieldName: String,
                  val baseFieldAcessor: Accessor,
                  val persisterFieldName: String,
                  val keyPersisterFieldName: String,
                  propertyName: String? = null) : Accessor(propertyName) {

    override fun get(existingBlock: CodeBlock?, baseVariableName: String?): CodeBlock {
        return appendAccess {
            statement("persistMap($existingBlock, bundle, $uniqueBaseKey, $keyFieldName, " +
                    "$keyPersisterFieldName, $persisterFieldName)")
        }
    }

    override fun set(existingBlock: CodeBlock?, baseVariableName: CodeBlock?): CodeBlock {
        return appendAccess {
            statement(baseFieldAcessor.set(
                    CodeBlock.of("restoreMap(\$L, bundle, $uniqueBaseKey, $keyFieldName, " +
                            "$keyPersisterFieldName, $persisterFieldName)", existingBlock),
                    baseVariableName).L)
        }
    }
}

