package com.eveningoutpost.dexdrip.carblookup.model;

public class RecipeItem {
    public long id;
    public long recipeId;
    public String barcode;
    public String productName;
    public String brand;
    public double carbsPer100g;
    public double itemWeightGrams;
    public double itemCarbsGrams;
    public int giEstimate;         // 0 = unknown

    public RecipeItem() {
    }
}