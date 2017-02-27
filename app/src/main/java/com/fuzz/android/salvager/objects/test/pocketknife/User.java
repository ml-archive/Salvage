package com.fuzz.android.salvager.objects.test.pocketknife;

import pocketknife.SaveState;

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */
public class User {

    @SaveState
    String name;

    @SaveState
    int age;
}
