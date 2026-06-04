package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.api.OpenFoodFactsClient;
import com.eveningoutpost.dexdrip.carblookup.api.ProductData;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.FavoriteItemRepository;
import com.eveningoutpost.dexdrip.carblookup.db.ProductCacheRepository;
import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

public class ProductDetailActivityTest extends RobolectricTestWithConfig {

    private final List<CarbLookupDatabase> testDatabases = new ArrayList<>();

    @Before
    public void resetOverrides() {
        TestProductDetailActivity.clientOverride = null;
        TestProductDetailActivity.cacheRepositoryOverride = null;
        TestProductDetailActivity.favoriteItemRepositoryOverride = null;
        TestProductDetailActivity.throwOnCacheRepositoryCreation = false;
        TestProductDetailActivity.sourceOverride = FoodDbSource.WORLD;
    }

    @After
    public void closeTestDatabases() {
        TestProductDetailActivity.clientOverride = null;
        TestProductDetailActivity.cacheRepositoryOverride = null;
        TestProductDetailActivity.favoriteItemRepositoryOverride = null;
        TestProductDetailActivity.throwOnCacheRepositoryCreation = false;
        TestProductDetailActivity.sourceOverride = FoodDbSource.WORLD;
        for (CarbLookupDatabase database : testDatabases) {
            database.close();
        }
        testDatabases.clear();
    }

    @Test
    public void missingBarcode_finishesImmediately() {
        ProductDetailActivity activity = Robolectric.buildActivity(ProductDetailActivity.class)
                .create()
                .get();

        assertTrue(activity.isFinishing());
    }

    @Test
    public void manualEntryWithoutBarcode_usesEnteredNameInResult() {
        TestProductDetailActivity.cacheRepositoryOverride = createIsolatedCacheRepository();

        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        EditText name = activity.findViewById(R.id.manualProductNameEditText);
        EditText brand = activity.findViewById(R.id.manualBrandEditText);
        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        EditText manualCarbs = activity.findViewById(R.id.manualCarbsPer100gEditText);
        Button addToMeal = activity.findViewById(R.id.addToMealButton);

        assertFalse(activity.isFinishing());

        name.setText("Homemade Soup");
        brand.setText("Kitchen");
        portion.setText("150");
        manualCarbs.setText("8.5");
        addToMeal.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals("Homemade Soup", result.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals("Kitchen", result.getStringExtra(ProductDetailActivity.EXTRA_BRAND));
        assertEquals(12.8, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 0.0), 0.01);
    }

    @Test
    public void manualEntryWithoutName_blocksResult() {
        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        ((EditText) activity.findViewById(R.id.portionGramsEditText)).setText("150");
        ((EditText) activity.findViewById(R.id.manualCarbsPer100gEditText)).setText("8.5");
        ((Button) activity.findViewById(R.id.addToMealButton)).performClick();

        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).getResultCode());
        assertFalse(activity.isFinishing());
        assertEquals("Please enter a product name", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void manualEntryWithoutBarcode_doesNotRequireCacheRepository() {
        TestProductDetailActivity.throwOnCacheRepositoryCreation = true;

        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        LinearLayout manualLayout = activity.findViewById(R.id.manualEntryLayout);
        assertFalse(activity.isFinishing());
        assertEquals(android.view.View.VISIBLE, manualLayout.getVisibility());
    }

    @Test
    public void saveToFavorites_savesManualItemWithDefaultPortion() {
        FavoriteItemRepository favoriteRepository = createIsolatedFavoriteRepository();
        TestProductDetailActivity.favoriteItemRepositoryOverride = favoriteRepository;

        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        ((EditText) activity.findViewById(R.id.manualProductNameEditText)).setText("Yogurt bowl");
        ((EditText) activity.findViewById(R.id.manualBrandEditText)).setText("Kitchen");
        ((EditText) activity.findViewById(R.id.manualCarbsPer100gEditText)).setText("12.5");
        ((EditText) activity.findViewById(R.id.portionGramsEditText)).setText("160");
        ((Button) activity.findViewById(R.id.saveToFavoritesButton)).performClick();

        java.util.List<FavoriteItem> favorites = favoriteRepository.getAll();
        assertEquals(1, favorites.size());
        assertEquals("Yogurt bowl", favorites.get(0).productName);
        assertEquals("Kitchen", favorites.get(0).brand);
        assertEquals(12.5, favorites.get(0).carbsPer100g, 0.01);
        assertEquals(160.0, favorites.get(0).defaultPortionGrams, 0.01);
    }

    @Test
    public void favoriteUseCountReset_clearsRepositoryCountAndHidesBadge() {
        FavoriteItemRepository favoriteRepository = createIsolatedFavoriteRepository();
        FavoriteItem favorite = new FavoriteItem();
        favorite.productName = "Toast";
        favorite.carbsPer100g = 45.0;
        favorite.defaultPortionGrams = 40.0;
        long favoriteId = favoriteRepository.save(favorite);
        favoriteRepository.incrementUseCount(favoriteId);
        TestProductDetailActivity.favoriteItemRepositoryOverride = favoriteRepository;

        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME, "Toast");
        intent.putExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 45.0);
        intent.putExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 40.0);
        intent.putExtra(ProductDetailActivity.EXTRA_FAVORITE_ID, favoriteId);
        intent.putExtra(ProductDetailActivity.EXTRA_FAVORITE_USE_COUNT, 1);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        TextView usedCount = activity.findViewById(R.id.favoriteUseCountTextView);
        assertEquals(android.view.View.VISIBLE, usedCount.getVisibility());
        activity.findViewById(R.id.resetFavoriteUseCountButton).performClick();

        assertEquals(android.view.View.GONE, usedCount.getVisibility());
        assertEquals(0, favoriteRepository.getAll().get(0).useCount);
    }

    @Test
    public void manualInvalidCarbsInput_clearsCalculatedCarbsAndBlocksResult() {
        TestProductDetailActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void fetchByBarcode(String barcode, ProductCallback callback) {
                callback.onNotFound(barcode);
            }
        };
        TestProductDetailActivity.cacheRepositoryOverride = createIsolatedCacheRepository();

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        buildIntent("1234567890"))
                .create()
                .start()
                .resume()
                .get();

        LinearLayout manualLayout = activity.findViewById(R.id.manualEntryLayout);
        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        EditText manualCarbs = activity.findViewById(R.id.manualCarbsPer100gEditText);
        TextView carbsResult = activity.findViewById(R.id.carbsResultTextView);
        Button addToMeal = activity.findViewById(R.id.addToMealButton);

        assertEquals(android.view.View.VISIBLE, manualLayout.getVisibility());

        portion.setText("50");
        manualCarbs.setText("10");
        assertEquals("Carbs: 5.0g", carbsResult.getText().toString());

        manualCarbs.setText("oops");
        assertEquals("Carbs: 0.0g", carbsResult.getText().toString());

        addToMeal.performClick();

        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).getResultCode());
        assertFalse(activity.isFinishing());
    }

    @Test
    public void validInput_returnsExpectedExtras() {
        final ProductData.Product product = new ProductData.Product();
        product.productName = "Weetabix";
        product.brands = "Weetabix Ltd";
        product.nutriments = new ProductData.Nutriments();
        product.nutriments.carbohydrates100g = 68.4;
        product.servingSize = "37.5g";

        TestProductDetailActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void fetchByBarcode(String barcode, ProductCallback callback) {
                callback.onSuccess(product);
            }
        };
        TestProductDetailActivity.cacheRepositoryOverride = createIsolatedCacheRepository();

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        buildIntent("5000169105306"))
                .create()
                .start()
                .resume()
                .get();

        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        Button addToMeal = activity.findViewById(R.id.addToMealButton);

        portion.setText("37.5");
        addToMeal.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals("Weetabix", result.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals("Weetabix Ltd", result.getStringExtra(ProductDetailActivity.EXTRA_BRAND));
        assertEquals("5000169105306", result.getStringExtra(ProductDetailActivity.EXTRA_BARCODE));
        assertEquals(68.4, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 0.0), 0.01);
        assertEquals(37.5, result.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(25.7, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 0.0), 0.01);
        assertTrue(activity.isFinishing());
    }

    @Test
    public void lookedUpProduct_displaysPortionsAndDefaultsPortionSize() {
        final ProductData.Product product = new ProductData.Product();
        product.productName = "Crackers";
        product.brands = "Snack Co";
        product.quantity = "450 g";
        product.servingSize = "45g";
        product.nutriments = new ProductData.Nutriments();
        product.nutriments.carbohydrates100g = 20.0;

        TestProductDetailActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void fetchByBarcode(String barcode, ProductCallback callback) {
                callback.onSuccess(product);
            }
        };
        TestProductDetailActivity.cacheRepositoryOverride = createIsolatedCacheRepository();

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        buildIntent("1234567890123"))
                .create()
                .start()
                .resume()
                .get();

        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        TextView servingSize = activity.findViewById(R.id.servingSizeTextView);
        TextView carbsResult = activity.findViewById(R.id.carbsResultTextView);
        TextView source = activity.findViewById(R.id.lookupSourceTextView);

        assertEquals("45", portion.getText().toString());
        assertEquals("Serving size: 45g | Portions: 10.0", servingSize.getText().toString());
        assertEquals("Carbs: 9.0g", carbsResult.getText().toString());
        assertEquals("Source: Open Food Facts", source.getText().toString());
    }

    @Test
    public void standardCarbItem_showsRegionalPortionSuggestionsAndReturnsResult() {
        StandardCarbItem rice = StandardCarbRepository.forSource(FoodDbSource.US).get(0);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        ProductDetailContract.forStandardCarbItem(
                                RuntimeEnvironment.getApplication(), rice))
                .create()
                .start()
                .resume()
                .get();

        TextView productName = activity.findViewById(R.id.productNameTextView);
        TextView source = activity.findViewById(R.id.lookupSourceTextView);
        LinearLayout portionSuggestions = activity.findViewById(R.id.portionSuggestionLayout);
        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        Button small = activity.findViewById(R.id.smallPortionButton);
        Button medium = activity.findViewById(R.id.mediumPortionButton);
        Button addToMeal = activity.findViewById(R.id.addToMealButton);

        assertEquals("Rice, white, cooked", productName.getText().toString());
        assertEquals("Source: USDA FoodData Central", source.getText().toString());
        assertEquals(android.view.View.VISIBLE, portionSuggestions.getVisibility());
        assertEquals("158", portion.getText().toString());
        assertEquals("Small 100g", small.getText().toString());
        assertEquals("Medium 158g", medium.getText().toString());
        assertEquals("Add to Meal", addToMeal.getText().toString());

        small.performClick();
        assertEquals("100", portion.getText().toString());
        addToMeal.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals("Rice, white, cooked", result.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals(28.2, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 0.0), 0.01);
        assertEquals(100.0, result.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(28.2, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 0.0), 0.01);
    }

    @Test
    public void standardLiquidCarbItem_showsMlAndReturnsConvertedGrams() {
        StandardCarbItem honey = findStandardItem(FoodDbSource.US, "Honey");

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        ProductDetailContract.forStandardCarbItem(
                                RuntimeEnvironment.getApplication(), honey))
                .create()
                .start()
                .resume()
                .get();

        TextView carbsPer100 = activity.findViewById(R.id.carbsPer100gTextView);
        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        Button medium = activity.findViewById(R.id.mediumPortionButton);
        Button addToMeal = activity.findViewById(R.id.addToMealButton);

        assertEquals("Carbs per 100ml: 115.4g", carbsPer100.getText().toString());
        assertEquals("Portion size (ml)", portion.getHint().toString());
        assertEquals("15", portion.getText().toString());
        assertEquals("Medium 15ml", medium.getText().toString());

        addToMeal.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals("Honey", result.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals(21.0, result.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(17.3, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 0.0), 0.01);
    }

    @Test
    public void lookedUpProduct_zeroCarbsShowsWarning() {
        final ProductData.Product product = new ProductData.Product();
        product.productName = "Mystery Drink";
        product.brands = "Drink Co";
        product.nutriments = new ProductData.Nutriments();
        product.nutriments.carbohydrates100g = 0.0;

        TestProductDetailActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void fetchByBarcode(String barcode, ProductCallback callback) {
                callback.onSuccess(product);
            }
        };
        TestProductDetailActivity.cacheRepositoryOverride = createIsolatedCacheRepository();

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        buildIntent("9999999999999"))
                .create()
                .start()
                .resume()
                .get();

        TextView warning = activity.findViewById(R.id.nutritionWarningTextView);
        assertEquals(android.view.View.VISIBLE, warning.getVisibility());
        assertEquals("Nutrition data says 0g carbs per 100g. Check before logging.", warning.getText().toString());
    }

    @Test
    public void existingManualItem_prefillsAndReturnsUpdatedValues() {
        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, true);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME, "Rice bowl");
        intent.putExtra(ProductDetailActivity.EXTRA_BRAND, "Kitchen");
        intent.putExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 25.0);
        intent.putExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 120.0);

        TestProductDetailActivity activity = Robolectric.buildActivity(
                        TestProductDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        EditText name = activity.findViewById(R.id.manualProductNameEditText);
        EditText brand = activity.findViewById(R.id.manualBrandEditText);
        EditText portion = activity.findViewById(R.id.portionGramsEditText);
        EditText manualCarbs = activity.findViewById(R.id.manualCarbsPer100gEditText);
        TextView carbsResult = activity.findViewById(R.id.carbsResultTextView);
        Button addToMeal = activity.findViewById(R.id.addToMealButton);

        assertEquals("Rice bowl", name.getText().toString());
        assertEquals("Kitchen", brand.getText().toString());
        assertEquals("25", manualCarbs.getText().toString());
        assertEquals("120", portion.getText().toString());
        assertEquals("Update Item", addToMeal.getText().toString());
        assertEquals("Carbs: 30.0g", carbsResult.getText().toString());

        portion.setText("200");
        addToMeal.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals("Rice bowl", result.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals("Kitchen", result.getStringExtra(ProductDetailActivity.EXTRA_BRAND));
        assertEquals(200.0, result.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
        assertEquals(50.0, result.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_FOR_PORTION, 0.0), 0.01);
    }

    private Intent buildIntent(String barcode) {
        Intent intent = new Intent();
        intent.putExtra(ProductDetailActivity.EXTRA_BARCODE, barcode);
        return intent;
    }

    private ProductCacheRepository createIsolatedCacheRepository() {
        CarbLookupDatabase database = CarbLookupDatabase.createInMemoryInstance(
            RuntimeEnvironment.getApplication());
        testDatabases.add(database);
        return new ProductCacheRepository(database);
    }

    private FavoriteItemRepository createIsolatedFavoriteRepository() {
        CarbLookupDatabase database = CarbLookupDatabase.createInMemoryInstance(
            RuntimeEnvironment.getApplication());
        testDatabases.add(database);
        return new FavoriteItemRepository(database);
    }

    private StandardCarbItem findStandardItem(FoodDbSource source, String name) {
        for (StandardCarbItem item : StandardCarbRepository.forSource(source)) {
            if (name.equals(item.name)) {
                return item;
            }
        }
        throw new AssertionError("Missing standard carb item: " + name);
    }

    public static class TestProductDetailActivity extends ProductDetailActivity {
        static OpenFoodFactsClient clientOverride;
        static ProductCacheRepository cacheRepositoryOverride;
        static FavoriteItemRepository favoriteItemRepositoryOverride;
        static FoodDbSource sourceOverride;
        static boolean throwOnCacheRepositoryCreation;

        @Override
        protected OpenFoodFactsClient createOpenFoodFactsClient() {
            return clientOverride != null ? clientOverride : super.createOpenFoodFactsClient();
        }

        @Override
        protected FoodDbSource createFoodDbSource() {
            return sourceOverride != null ? sourceOverride : super.createFoodDbSource();
        }

        @Override
        protected ProductCacheRepository createProductCacheRepository() {
            if (throwOnCacheRepositoryCreation) {
                throw new IllegalStateException("cache unavailable");
            }
            return cacheRepositoryOverride != null ? cacheRepositoryOverride : super.createProductCacheRepository();
        }

        @Override
        protected FavoriteItemRepository createFavoriteItemRepository() {
            return favoriteItemRepositoryOverride != null
                    ? favoriteItemRepositoryOverride : super.createFavoriteItemRepository();
        }
    }
}