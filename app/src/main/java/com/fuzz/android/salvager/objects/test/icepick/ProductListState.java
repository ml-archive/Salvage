package com.fuzz.android.salvager.objects.test.icepick;

import com.fuzz.android.salvager.objects.test.ProductList;

import java.util.Date;

import icepick.State;

class ProductListState {

    @State
    String query;

    @State
    String partialQuery;

    @State
    int totalResultCount;

    @State
    Date startTime;

    @State
    Date endTime;

    @State
    String departmentFilterName;

    @State
    String nextProductsPage;

    @State
    ProductList productListState;

    @State
    int resultsMode;

    @State
    String lastLoadedUrl;

}
