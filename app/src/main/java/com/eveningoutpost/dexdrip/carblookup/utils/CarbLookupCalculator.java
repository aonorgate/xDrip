package com.eveningoutpost.dexdrip.carblookup.utils;

import java.util.List;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

public class CarbLookupCalculator {

    /**
     * Calculates the carbs for a given portion of food.
     *
     * @param carbsPer100g Carbs per 100 grams of the food.
     * @param portionGrams The portion size in grams.
     * @return The calculated carbs for the portion, rounded to one decimal place.
     * @throws IllegalArgumentException if portionGrams <= 0 or carbsPer100g < 0.
     */
    public static double calculateCarbs(double carbsPer100g, double portionGrams) {
        if (portionGrams <= 0) {
            throw new IllegalArgumentException("portionGrams must be > 0");
        }
        if (carbsPer100g < 0) {
            throw new IllegalArgumentException("carbsPer100g must be >= 0");
        }
        return Math.round((carbsPer100g * portionGrams / 100.0) * 10.0) / 10.0;
    }

    /**
     * Calculates the total carbs for a list of meal items.
     *
     * @param items The list of meal items.
     * @return The total carbs for all items, rounded to one decimal place.
     *         Returns 0.0 if items is null or empty.
     */
    public static double totalMealCarbs(List<MealItem> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (MealItem item : items) {
            total += item.carbsForPortion;
        }
        return Math.round(total * 10.0) / 10.0;
    }
}