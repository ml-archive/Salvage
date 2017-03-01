package com.fuzz.android.salvager.objects.kotlin

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View
import com.fuzz.android.salvage.Salvager
import com.fuzz.android.salvage.core.Persist
import com.fuzz.android.salvage.core.PersistField
import com.fuzz.android.salvage.core.PersistPolicy

/**
 * Description:
 */
@Persist(argument = true, persistPolicy = PersistPolicy.ANNOTATIONS_ONLY)
class DummyFragment : Fragment() {

    companion object {

        @JvmStatic
        fun newInstance(title: String, message: String) = DummyFragment().apply {
            arguments = Bundle().apply {
                putString(DummyFragmentPersister.key_title, title)
                putString(DummyFragmentPersister.key_message, message)
            }
        }
    }

    @PersistField
    var title = ""

    @PersistField
    var message = ""

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Salvager.loadArguments(this, arguments)
    }
}