package com.eveningoutpost.dexdrip.carblookup.api;

import com.google.gson.annotations.SerializedName;

public class ProductData {
    public int status;   // 1 = found, 0 = not found
    public String code;  // barcode echoed back
    public Product product;

    public static class Product {
        public String code;

        @SerializedName("product_name")
        public String productName;

        @SerializedName("product_name_en")
        public String productNameEn;

        public String brands;
        public String quantity;

        @SerializedName("serving_size")
        public String servingSize;

        public Nutriments nutriments;
    }

    public static class Nutriments {
        @SerializedName("carbohydrates_100g")
        public double carbohydrates100g;

        @SerializedName("sugars_100g")
        public double sugars100g;

        @SerializedName("energy-kcal_100g")
        public double energyKcal100g;

        @SerializedName("fat_100g")
        public double fat100g;

        @SerializedName("proteins_100g")
        public double proteins100g;

        @SerializedName("fiber_100g")
        public Double fiber100g;
    }
}
