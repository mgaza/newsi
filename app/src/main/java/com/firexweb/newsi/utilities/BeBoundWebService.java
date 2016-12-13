package com.firexweb.newsi.utilities;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BeBoundWebService {
    String BASE_URL = "https://web.be-bound.com/mob/";

    @GET("store.php")
    Call<StoreConfig> getStore(@Query("v") String version, @Query("m") String mccmnc);
}
