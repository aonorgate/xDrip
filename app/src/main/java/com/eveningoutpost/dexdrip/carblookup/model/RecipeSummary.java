package com.eveningoutpost.dexdrip.carblookup.model;

public class RecipeSummary {
    public long id;
    public String name;
    public double portionCount;
    public double carbsPerPortion;
    public double totalCarbsGrams;
    public int itemCount;
    public long lastUsedAt;
    public String mealTime;
    public int useCount;

    public RecipeSummary() {
    }
}