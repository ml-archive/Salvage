package com.fuzz.android.salvager;

import com.fuzz.android.salvage.core.Persist;

/**
 * Description:
 *
 * @author Andrew Grosner (Fuzz)
 */

public class InnerClassExample {

    @Persist
    public static class Inner {

        int id;

        String name;

        final Example example = new Example();
    }
}
