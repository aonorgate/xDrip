package com.eveningoutpost.dexdrip.carblookup.model;

import java.util.List;
import java.util.ArrayList;

public class Recipe {
    public long id;               // 0 for new (unsaved) recipes
    public String name;
    public double portionCount;
    public long createdAt;
    public long updatedAt;
    public long lastUsedAt;
    public double totalCarbsGrams;
    public double carbsPerPortion;
    public String mealTime;        // "breakfast", "lunch", "dinner", "snack", "any"
    public int useCount;
    public String notes;
    public List<RecipeItem> items;

    public Recipe() {
        this.items = new ArrayList<>();
        this.mealTime = "any";
    }
}