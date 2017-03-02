package com.fuzz.android.salvager.objects.test.pocketknife;

import com.fuzz.android.salvager.objects.test.ProductList;

import java.util.Date;

import pocketknife.SaveState;

class ProductListState {

    @SaveState
    String query;

    @SaveState
    String partialQuery;

    @SaveState
    int totalResultCount;

    @SaveState
    Date startTime;

    @SaveState
    Date endTime;

    @SaveState
    String departmentFilterName;

    @SaveState
    String nextProductsPage;

    @SaveState
    ProductList productListState;

    @SaveState
    int resultsMode;

    @SaveState
    String lastLoadedUrl;

}
