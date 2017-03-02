package com.fuzz.android.salvager.objects.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: Example object that we use in ProductListState examples.
 */
public class ProductList implements Serializable {

    String name;

    List<Object> results = new ArrayList<>();
}
