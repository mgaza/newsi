package com.firexweb.newsi.utilities;

import com.google.gson.annotations.SerializedName;

public class StoreConfig {
    public BeStore bestore;

    public static class BeStore {
        public String name;
        @SerializedName("package")
        public String packageName;
        public String logo;
    }
}
