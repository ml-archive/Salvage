package com.fuzz.android.salvager.objects.test.parceler;

import com.fuzz.android.salvager.objects.test.ProductList;

import org.parceler.Parcel;

import java.util.Date;

@Parcel
class ProductListState {

    String query;

    String partialQuery;

    int totalResultCount;

    Date startTime;
    Date endTime;

    String departmentFilterName;

    String nextProductsPage;

    ProductList productListState;

    int resultsMode;

    String lastLoadedUrl;

}
