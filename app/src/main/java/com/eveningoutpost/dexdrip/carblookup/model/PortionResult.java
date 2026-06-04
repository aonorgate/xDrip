package com.eveningoutpost.dexdrip.carblookup.model;

import java.util.List;

public class PortionResult {
    private long recipeId;
    private String recipeName;
    private double numberOfPortions;
    private double totalCarbsGrams;
    private double carbsPerSinglePortion;
    private List<PortionItemResult> items;
    private boolean recipeFound;
    private double totalGlEstimate;
    private boolean glIsPartial;

    // Getters and Setters
    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public double getNumberOfPortions() {
        return numberOfPortions;
    }

    public void setNumberOfPortions(double numberOfPortions) {
        this.numberOfPortions = numberOfPortions;
    }

    public double getTotalCarbsGrams() {
        return totalCarbsGrams;
    }

    public void setTotalCarbsGrams(double totalCarbsGrams) {
        this.totalCarbsGrams = totalCarbsGrams;
    }

    public double getCarbsPerSinglePortion() {
        return carbsPerSinglePortion;
    }

    public void setCarbsPerSinglePortion(double carbsPerSinglePortion) {
        this.carbsPerSinglePortion = carbsPerSinglePortion;
    }

    public List<PortionItemResult> getItems() {
        return items;
    }

    public void setItems(List<PortionItemResult> items) {
        this.items = items;
    }

    public boolean isRecipeFound() {
        return recipeFound;
    }

    public void setRecipeFound(boolean recipeFound) {
        this.recipeFound = recipeFound;
    }

    public double getTotalGlEstimate() {
        return totalGlEstimate;
    }

    public void setTotalGlEstimate(double totalGlEstimate) {
        this.totalGlEstimate = totalGlEstimate;
    }

    public boolean isGlPartial() {
        return glIsPartial;
    }

    public void setGlIsPartial(boolean glIsPartial) {
        this.glIsPartial = glIsPartial;
    }
}