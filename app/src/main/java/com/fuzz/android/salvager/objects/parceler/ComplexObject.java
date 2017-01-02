package com.fuzz.android.salvager.objects.parceler;

import org.parceler.Parcel;

import java.util.List;
import java.util.Map;

/**
 * Description:
 *
 * @author Andrew Grosner (fuzz)
 */
@Parcel
public class ComplexObject {

    List<String> list;

    Map<String, Integer> map;
}
