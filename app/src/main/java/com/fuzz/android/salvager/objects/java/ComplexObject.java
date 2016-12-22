package com.fuzz.android.salvager.objects.java;

import com.fuzz.android.salvage.core.Persist;

import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */
@Persist
public class ComplexObject {

    List<String> list;

    Map<String, Integer> map;
}
