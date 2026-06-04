package com.eveningoutpost.dexdrip.carblookup;

import android.content.Intent;

import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;

import java.util.ArrayList;
import java.util.List;

class MealItemFactory {

    MealItem fromProductResult(Intent data) {
        return ProductDetailContract.mealItemFromResult(data);
    }

    List<MealItem> copyMealItems(List<MealItem> sourceItems) {
        List<MealItem> copies = new ArrayList<>();
        if (sourceItems == null) {
            return copies;
        }
        for (MealItem source : sourceItems) {
            copies.add(copyMealItem(source));
        }
        return copies;
    }

    MealItem fromPortionItemResult(PortionItemResult itemResult) {
        MealItem item = new MealItem();
        item.productName = itemResult.getProductName();
        item.barcode = itemResult.getBarcode();
        item.brand = itemResult.getBrand();
        item.carbsPer100g = itemResult.getCarbsPer100g();
        item.portionGrams = itemResult.getScaledWeightGrams();
        item.carbsForPortion = itemResult.getScaledCarbsGrams();
        item.giEstimate = itemResult.getGiEstimate();
        item.glEstimate = itemResult.getScaledGlEstimate();
        return item;
    }

    RecipeItem toRecipeItem(MealItem mealItem) {
        RecipeItem item = new RecipeItem();
        item.productName = mealItem.productName;
        item.brand = mealItem.brand;
        item.barcode = mealItem.barcode;
        item.carbsPer100g = mealItem.carbsPer100g;
        item.itemWeightGrams = mealItem.portionGrams;
        item.itemCarbsGrams = mealItem.carbsForPortion;
        item.giEstimate = mealItem.giEstimate;
        return item;
    }

    List<RecipeItem> toRecipeItems(List<MealItem> mealItems) {
        List<RecipeItem> items = new ArrayList<>();
        if (mealItems == null) {
            return items;
        }
        for (MealItem mealItem : mealItems) {
            items.add(toRecipeItem(mealItem));
        }
        return items;
    }

    private MealItem copyMealItem(MealItem source) {
        MealItem copy = new MealItem();
        copy.productName = source.productName;
        copy.brand = source.brand;
        copy.barcode = source.barcode;
        copy.carbsPer100g = source.carbsPer100g;
        copy.portionGrams = source.portionGrams;
        copy.carbsForPortion = source.carbsForPortion;
        copy.giEstimate = source.giEstimate;
        copy.glEstimate = source.glEstimate;
        return copy;
    }
}