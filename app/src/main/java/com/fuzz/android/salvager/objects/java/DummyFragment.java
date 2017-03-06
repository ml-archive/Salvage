package com.fuzz.android.salvager.objects.java;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.fuzz.android.salvage.Salvager;
import com.fuzz.android.salvage.core.PersistArguments;
import com.fuzz.android.salvage.core.PersistField;

/**
 * Description:
 */
@PersistArguments
public class DummyFragment extends Fragment {

    public static DummyFragment newInstance(String title, String message) {
        DummyFragment fragment = new DummyFragment();
        Bundle bundle = new Bundle();
        bundle.putString(DummyFragmentPersister.key_title, title);
        bundle.putString(DummyFragmentPersister.key_message, message);
        fragment.setArguments(bundle);
        return fragment;
    }

    @PersistField
    String title;

    @PersistField
    String message;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Salvager.loadArguments(this, savedInstanceState);
    }

}
