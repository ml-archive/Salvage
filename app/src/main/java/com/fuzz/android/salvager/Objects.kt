package com.fuzz.android.salvager

import com.fuzz.android.salvage.core.Persist

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */

@Persist
data class User(var name: String = "", var age: Int = 0)

@Persist
data class ViewData(var visibility: Int = 0, var isShown: Boolean = false)