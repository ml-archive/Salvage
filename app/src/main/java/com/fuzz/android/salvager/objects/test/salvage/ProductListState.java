package com.fuzz.android.salvager.objects.test.salvage;

import com.fuzz.android.salvage.core.Persist;
import com.fuzz.android.salvager.objects.test.ProductList;

import java.util.Date;

@Persist
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
