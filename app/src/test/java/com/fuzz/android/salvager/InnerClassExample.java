package com.fuzz.android.salvager;

import com.fuzz.android.salvage.core.Persist;
import com.fuzz.android.salvage.core.PersistField;
import com.fuzz.android.salvage.core.PersistPolicy;

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

public class InnerClassExample {

    @Persist(persistPolicy = PersistPolicy.ANNOTATIONS_ONLY)
    public static class Inner {

        @PersistField
        int id;

        @PersistField
        String name;

        @PersistField
        boolean isSet;

        @PersistField
        final Example example = new Example();
    }
}
