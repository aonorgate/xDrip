package com.eveningoutpost.dexdrip.carblookup;

import android.content.Intent;

import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbRepository;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProductDetailContractTest extends com.eveningoutpost.dexdrip.RobolectricTestWithConfig {

    @Test
    public void manualEntryIntentRequestsManualEntry() {
        Intent intent = ProductDetailContract.manualEntry(RuntimeEnvironment.getApplication());

        assertTrue(intent.getBooleanExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, false));
    }

    @Test
    public void mealItemIntentAddsManualEntryWhenBarcodeMissing() {
        MealItem item = new MealItem();
        item.productName = "Soup";
        item.brand = "Kitchen";
        item.carbsPer100g = 8.5;
        item.portionGrams = 150.0;
        item.carbsForPortion = 12.8;

        Intent intent = ProductDetailContract.forMealItem(RuntimeEnvironment.getApplication(), item);

        assertTrue(intent.getBooleanExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, false));
        assertTrue(ProductDetailContract.hasExistingItemExtras(intent));
        assertEquals("Soup", intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals(150.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
    }

    @Test
    public void favoriteIntentCarriesFavoriteMetadataAndDefaultPortion() {
        FavoriteItem favorite = new FavoriteItem();
        favorite.id = 42L;
        favorite.productName = "Rice";
        favorite.brand = "Brand";
        favorite.barcode = "123";
        favorite.carbsPer100g = 28.0;
        favorite.defaultPortionGrams = 200.0;
        favorite.useCount = 3;

        Intent intent = ProductDetailContract.forFavorite(RuntimeEnvironment.getApplication(), favorite);

        assertEquals(42L, intent.getLongExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, 0L));
        assertEquals(4, intent.getIntExtra(ProductDetailActivity.EXTRA_FAVORITE_USE_COUNT, 0));
        assertEquals(200.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
    }

    @Test
    public void putAndReadProductResultRoundTripsMealItem() {
        Intent intent = new Intent();
        ProductDetailContract.putProductResult(intent, "Pasta", "Kitchen", "456", 72.0, 80.0, 57.6);
        intent.putExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, 42L);

        MealItem item = ProductDetailContract.mealItemFromResult(intent);

        assertEquals("Pasta", item.productName);
        assertEquals("Kitchen", item.brand);
        assertEquals("456", item.barcode);
        assertEquals(72.0, item.carbsPer100g, 0.01);
        assertEquals(80.0, item.portionGrams, 0.01);
        assertEquals(57.6, item.carbsForPortion, 0.01);
        assertEquals(42L, item.favoriteId);
    }

    @Test
    public void favoriteWithoutPortionIntentCarriesFavoriteId() {
        Intent intent = ProductDetailContract.forFavoriteWithoutPortion(
                RuntimeEnvironment.getApplication(), "Rice", "Brand", "123", 28.0, 42L);

        assertEquals(42L, intent.getLongExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, 0L));
        assertEquals("Rice", intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals(28.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 0.0), 0.01);
    }

    @Test
    public void standardCarbItemIntentCarriesPortionSuggestionsAndSource() {
        StandardCarbItem rice = StandardCarbRepository.forSource(FoodDbSource.US).get(0);

        Intent intent = ProductDetailContract.forStandardCarbItem(RuntimeEnvironment.getApplication(), rice);

        assertTrue(intent.getBooleanExtra(ProductDetailActivity.EXTRA_STANDARD_CARB_ITEM, false));
        assertEquals("Rice, white, cooked", intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals("Source: USDA FoodData Central", intent.getStringExtra(ProductDetailActivity.EXTRA_LOOKUP_SOURCE));
        assertEquals(28.2, intent.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 0.0), 0.01);
        assertEquals(158.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(100.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_SMALL_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(158.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_MEDIUM_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(250.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_LARGE_PORTION_GRAMS, 0.0), 0.01);
        assertEquals("g", intent.getStringExtra(ProductDetailActivity.EXTRA_PORTION_UNIT));
        assertEquals(1.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_GRAMS_PER_PORTION_UNIT, 0.0), 0.01);
    }

    @Test
    public void standardLiquidCarbItemIntentCarriesMilliliterDisplayUnit() {
        StandardCarbItem milk = findByName(StandardCarbRepository.forSource(FoodDbSource.US), "2% milk");

        Intent intent = ProductDetailContract.forStandardCarbItem(RuntimeEnvironment.getApplication(), milk);

        assertEquals("2% milk", intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals(200.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
        assertEquals("ml", intent.getStringExtra(ProductDetailActivity.EXTRA_PORTION_UNIT));
        assertEquals(1.0, intent.getDoubleExtra(ProductDetailActivity.EXTRA_GRAMS_PER_PORTION_UNIT, 0.0), 0.01);
    }

    private StandardCarbItem findByName(java.util.List<StandardCarbItem> items, String name) {
        for (StandardCarbItem item : items) {
            if (name.equals(item.name)) {
                return item;
            }
        }
        throw new AssertionError("Missing standard carb item: " + name);
    }
}