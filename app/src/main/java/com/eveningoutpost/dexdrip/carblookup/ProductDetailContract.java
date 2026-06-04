package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;

class ProductDetailContract {
    private ProductDetailContract() {
    }

    static Intent manualEntry(Context context) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);
        return intent;
    }

    static Intent forBarcode(Context context, String barcode) {
        Intent intent = new Intent(context, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_BARCODE, barcode);
        return intent;
    }

    static Intent forMealItem(Context context, MealItem item) {
        Intent intent = withProductValues(new Intent(context, ProductDetailActivity.class),
                item.productName, item.brand, item.barcode, item.carbsPer100g, item.portionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, item.carbsForPortion);
        putManualEntryWhenNeeded(intent, item.barcode);
        return intent;
    }

    static Intent forRecipeItem(Context context, RecipeItem item) {
        Intent intent = withProductValues(manualEntry(context), item.productName, item.brand,
                item.barcode, item.carbsPer100g, item.itemWeightGrams);
        return intent;
    }

    static Intent forFavorite(Context context, FavoriteItem item) {
        Intent intent = withProductValues(manualEntry(context), item.productName, item.brand,
                item.barcode, item.carbsPer100g, item.defaultPortionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, item.id);
        intent.putExtra(ProductDetailActivity.EXTRA_FAVORITE_USE_COUNT, item.useCount + 1);
        return intent;
    }

    static Intent forFavoriteWithoutPortion(Context context, String name, String brand,
            String barcode, double carbsPer100g, long favoriteId) {
        Intent intent = withProductValues(new Intent(context, ProductDetailActivity.class), name, brand,
                barcode, carbsPer100g, 0.0);
        intent.putExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, favoriteId);
        putManualEntryWhenNeeded(intent, barcode);
        return intent;
    }

    static Intent forStandardCarbItem(Context context, StandardCarbItem item) {
        Intent intent = withProductValues(new Intent(context, ProductDetailActivity.class), item.name, "",
                null, item.carbsPer100g, item.mediumPortionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_LOOKUP_SOURCE, "Source: " + item.source);
        intent.putExtra(ProductDetailActivity.EXTRA_SMALL_PORTION_GRAMS, item.smallPortionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_MEDIUM_PORTION_GRAMS, item.mediumPortionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_LARGE_PORTION_GRAMS, item.largePortionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_PORTION_UNIT, item.portionUnit);
        intent.putExtra(ProductDetailActivity.EXTRA_GRAMS_PER_PORTION_UNIT, item.gramsPerPortionUnit);
        intent.putExtra(ProductDetailActivity.EXTRA_STANDARD_CARB_ITEM, true);
        return intent;
    }

    static boolean hasExistingItemExtras(Intent intent) {
        return intent != null && (intent.hasExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME)
                || intent.hasExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G)
                || intent.hasExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS));
    }

    static void putProductResult(Intent intent, String name, String brand, String barcode,
            double carbsPer100g, double portionGrams, double carbsForPortion) {
        withProductValues(intent, name, brand, barcode, carbsPer100g, portionGrams);
        intent.putExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, carbsForPortion);
    }

    static MealItem mealItemFromResult(Intent data) {
        MealItem item = new MealItem();
        item.productName = data.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME);
        item.brand = data.getStringExtra(ProductDetailActivity.EXTRA_BRAND);
        item.barcode = data.getStringExtra(ProductDetailActivity.EXTRA_BARCODE);
        item.carbsPer100g = data.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 0.0);
        item.portionGrams = data.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0);
        item.carbsForPortion = data.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 0.0);
        item.favoriteId = data.getLongExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, 0L);
        return item;
    }

    private static Intent withProductValues(Intent intent, String name, String brand, String barcode,
            double carbsPer100g, double portionGrams) {
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME, name);
        intent.putExtra(ProductDetailActivity.EXTRA_BRAND, brand);
        intent.putExtra(ProductDetailActivity.EXTRA_BARCODE, barcode);
        intent.putExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, carbsPer100g);
        if (portionGrams > 0.0) {
            intent.putExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, portionGrams);
        }
        return intent;
    }

    private static void putManualEntryWhenNeeded(Intent intent, String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);
        }
    }
}