package com.fuzz.android.salvage

import com.raizlabs.android.dbflow.processor.definition.BaseDefinition
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
class PersistenceField(manager: ProcessorManager, element: Element, isPackagePrivate: Boolean)
    : BaseDefinition(element, manager) {


    val accessor: Accessor

    val bundleKey: String

    init {
        if (isPackagePrivate) {
            accessor = PackagePrivateScopeAccessor(elementName, packageName,
                    ClassName.get(element.enclosingElement as TypeElement).simpleName())
            PackagePrivateScopeAccessor.putElement(accessor.helperClassName, elementName)
        } else if (element.modifiers.contains(Modifier.PRIVATE)) {

            val isBoolean = elementTypeName?.box() == TypeName.BOOLEAN.box()

            accessor = PrivateScopeAccessor(elementName, object : GetterSetter {
                override val getterName = ""
                override val setterName = ""

            }, isBoolean, false)
        } else {
            accessor = VisibleScopeAccessor(elementName)
        }

        // for now, later we can configure
        bundleKey = elementName
    }

    fun writePersistence(methodBuilder: MethodSpec.Builder) {
        elementTypeName?.let { typeName ->
            methodBuilder.addStatement("bundle.put\$L(\$L + \$S, ${accessor.get(CodeBlock.of(defaultParam))})",
                    ClassLookupMap.valueForType(typeName), "BASE_KEY", bundleKey)
        }
    }

    fun writeUnpack(methodBuilder: MethodSpec.Builder) {
        elementTypeName?.let { typeName ->
            methodBuilder.addStatement("${accessor.set(
                    CodeBlock.of("bundle.get\$L(\$L + \$S)",
                            ClassLookupMap.valueForType(typeName), "BASE_KEY", bundleKey),
                    CodeBlock.of(defaultParam))}")
        }
    }

}