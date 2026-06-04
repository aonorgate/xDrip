package com.eveningoutpost.dexdrip.carblookup;

import android.content.Intent;

import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class MealItemFactoryTest extends com.eveningoutpost.dexdrip.RobolectricTestWithConfig {

    @Test
    public void fromProductResult_mapsProductExtras() {
        Intent data = new Intent();
        data.putExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME, "Pasta");
        data.putExtra(ProductDetailActivity.EXTRA_BRAND, "Kitchen");
        data.putExtra(ProductDetailActivity.EXTRA_BARCODE, "123");
        data.putExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 72.0);
        data.putExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 80.0);
        data.putExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 57.6);

        MealItem item = new MealItemFactory().fromProductResult(data);

        assertEquals("Pasta", item.productName);
        assertEquals("Kitchen", item.brand);
        assertEquals("123", item.barcode);
        assertEquals(72.0, item.carbsPer100g, 0.01);
        assertEquals(80.0, item.portionGrams, 0.01);
        assertEquals(57.6, item.carbsForPortion, 0.01);
    }

    @Test
    public void fromPortionResultAndToRecipeItem_preserveNutritionFields() {
        PortionItemResult portion = new PortionItemResult();
        portion.setProductName("Rice");
        portion.setBrand("Brand");
        portion.setBarcode("456");
        portion.setCarbsPer100g(28.0);
        portion.setScaledWeightGrams(200.0);
        portion.setScaledCarbsGrams(56.0);
        portion.setGiEstimate(73);
        portion.setScaledGlEstimate(40.9);

        MealItemFactory factory = new MealItemFactory();
        MealItem mealItem = factory.fromPortionItemResult(portion);
        RecipeItem recipeItem = factory.toRecipeItem(mealItem);

        assertEquals("Rice", mealItem.productName);
        assertEquals(40.9, mealItem.glEstimate, 0.01);
        assertEquals("Rice", recipeItem.productName);
        assertEquals(200.0, recipeItem.itemWeightGrams, 0.01);
        assertEquals(56.0, recipeItem.itemCarbsGrams, 0.01);
        assertEquals(73, recipeItem.giEstimate);
    }

    @Test
    public void copyMealItems_returnsIndependentCopies() {
        MealItem item = new MealItem();
        item.productName = "Apple";
        item.carbsForPortion = 12.0;

        List<MealItem> copies = new MealItemFactory().copyMealItems(Collections.singletonList(item));

        assertEquals(1, copies.size());
        assertEquals("Apple", copies.get(0).productName);
        assertNotSame(item, copies.get(0));
    }
}