package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CurrentMealTest extends com.eveningoutpost.dexdrip.RobolectricTestWithConfig {

    @Test
    public void addOrUpdate_updatesEditingItemAndResetsEditState() {
        CurrentMeal currentMeal = new CurrentMeal();
        currentMeal.add(mealItem("Apple", 12.0));
        currentMeal.add(mealItem("Bread", 20.0));

        assertTrue(currentMeal.beginEditItem(0));
        assertTrue(currentMeal.addOrUpdate(mealItem("Pear", 14.0)));
        assertFalse(currentMeal.addOrUpdate(mealItem("Rice", 30.0)));

        assertEquals(3, currentMeal.getItems().size());
        assertEquals("Pear", currentMeal.getItems().get(0).productName);
        assertEquals("Bread", currentMeal.getItems().get(1).productName);
        assertEquals("Rice", currentMeal.getItems().get(2).productName);
    }

    @Test
    public void removeKeepsEditingIndexAligned() {
        CurrentMeal currentMeal = new CurrentMeal();
        currentMeal.add(mealItem("Apple", 12.0));
        currentMeal.add(mealItem("Bread", 20.0));
        currentMeal.add(mealItem("Rice", 30.0));

        assertTrue(currentMeal.beginEditItem(2));
        assertEquals("Apple", currentMeal.remove(0).productName);
        assertTrue(currentMeal.addOrUpdate(mealItem("Pasta", 40.0)));

        assertEquals(2, currentMeal.getItems().size());
        assertEquals("Bread", currentMeal.getItems().get(0).productName);
        assertEquals("Pasta", currentMeal.getItems().get(1).productName);
        assertNull(currentMeal.remove(20));
    }

    @Test
    public void totalCarbsRoundsThroughCalculator() {
        CurrentMeal currentMeal = new CurrentMeal();
        currentMeal.add(mealItem("A", 1.24));
        currentMeal.add(mealItem("B", 2.31));

        assertEquals(3.6, currentMeal.totalCarbs(), 0.01);
    }

    private MealItem mealItem(String name, double carbs) {
        MealItem item = new MealItem();
        item.productName = name;
        item.carbsForPortion = carbs;
        return item;
    }
}