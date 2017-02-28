package com.fuzz.android.salvager.objects.kotlin

import android.support.v4.app.Fragment
import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistField
import com.fuzz.android.salvage.core.PersistPolicy

/**
 * Description:
 */
@Persist(argument = true, persistPolicy = PersistPolicy.ANNOTATIONS_ONLY)
class DummyFragment : Fragment() {


    @PersistField
    var title = ""

    @PersistField
    var shouldLoad = false


}