package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import java.util.ArrayList;
import java.util.List;

public class MealSummary {
    public long id;
    public String name;
    public double totalCarbs;
    public long savedAt;
    public String treatmentUuid;
    public String notes;
    public String mealTime = "any";
    public int itemCount;
    public List<MealItem> items;

    public MealSummary() {
        this.items = new ArrayList<>();
    }

    public MealSummary(String name, double totalCarbs) {
        this.name = name;
        this.totalCarbs = totalCarbs;
        this.savedAt = System.currentTimeMillis();
        this.items = new ArrayList<>();
    }
}
