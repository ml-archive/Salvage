package com.fuzz.android.salvager.objects.kotlin

import com.fuzz.android.salvage.core.Persist

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */

@Persist
class User(var name: String = "", var age: Int = 0)

@Persist
class ViewData(var visibility: Int = 0, var isShown: Boolean = false)

@Persist
class ComplexObject(var list: List<String> = arrayListOf(),
                    var map: Map<String, Int> = mutableMapOf())