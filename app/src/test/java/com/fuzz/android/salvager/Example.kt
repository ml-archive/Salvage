package com.fuzz.android.salvager

import com.fuzz.android.salvage.core.Persist

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */
@Persist
data class Example(var name: String,
                   var age: Int?)