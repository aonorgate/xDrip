package com.eveningoutpost.dexdrip.carblookup.standard;

public class StandardCarbItem {
    public static final String UNIT_GRAMS = "g";
    public static final String UNIT_MILLILITERS = "ml";

    public final StandardCarbCategory category;
    public final String name;
    public final double carbsPer100g;
    public final double smallPortionGrams;
    public final double mediumPortionGrams;
    public final double largePortionGrams;
    public final String source;
    public final String portionUnit;
    public final double gramsPerPortionUnit;

    StandardCarbItem(StandardCarbCategory category, String name, double carbsPer100g, double smallPortionGrams,
            double mediumPortionGrams, double largePortionGrams, String source) {
        this(category, name, carbsPer100g, smallPortionGrams, mediumPortionGrams, largePortionGrams,
                source, UNIT_GRAMS, 1.0);
    }

    StandardCarbItem(StandardCarbCategory category, String name, double carbsPer100g, double smallPortionGrams,
            double mediumPortionGrams, double largePortionGrams, String source,
            String portionUnit, double gramsPerPortionUnit) {
        this.category = category;
        this.name = name;
        this.carbsPer100g = carbsPer100g;
        this.smallPortionGrams = smallPortionGrams;
        this.mediumPortionGrams = mediumPortionGrams;
        this.largePortionGrams = largePortionGrams;
        this.source = source;
        this.portionUnit = portionUnit == null || portionUnit.trim().isEmpty() ? UNIT_GRAMS : portionUnit;
        this.gramsPerPortionUnit = gramsPerPortionUnit > 0.0 ? gramsPerPortionUnit : 1.0;
    }

    public double displayAmountForGrams(double grams) {
        return grams / gramsPerPortionUnit;
    }

    public double gramsForDisplayAmount(double amount) {
        return amount * gramsPerPortionUnit;
    }

    public double carbsPer100PortionUnits() {
        return carbsPer100g * gramsPerPortionUnit;
    }
}