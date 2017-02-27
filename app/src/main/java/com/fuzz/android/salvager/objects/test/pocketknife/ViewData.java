package com.fuzz.android.salvager.objects.test.pocketknife;

import pocketknife.SaveState;

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */
public class ViewData {

    @SaveState
    int visibility;

    @SaveState
    boolean isShown;
}
