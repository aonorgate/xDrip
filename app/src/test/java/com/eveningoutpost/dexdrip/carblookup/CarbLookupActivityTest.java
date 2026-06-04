package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbCategory;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowActivity.IntentForResult;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class CarbLookupActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetOverrides() {
        TestCarbLookupActivity.mealRepositoryOverride = null;
        TestCarbLookupActivity.recipeRepositoryOverride = null;
        TestCarbLookupActivity.mealLogServiceOverride = null;
        TestCarbLookupActivity.sourceOverride = FoodDbSource.WORLD;
        TestCarbLookupActivity.hourOverride = 12;
        TestCarbLookupActivity.currentTimeOverride = null;
    }

    @Test
    public void itemSearchButton_startsFoodSearchPicker() {
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);

        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.findViewById(R.id.itemSearchButton).performClick();

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(FoodSearchActivity.class.getName(), started.intent.getComponent().getClassName());
        assertEquals(CarbLookupActivity.REQUEST_ITEM_SEARCH, started.requestCode);
    }

    @Test
    public void openingCarbLookup_focusesMealNotes() {
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);

        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();
        shadowOf(Looper.getMainLooper()).idle();

        EditText mealNotes = activity.findViewById(R.id.mealNotesEditText);
        assertTrue(mealNotes.hasFocus());
    }

    @Test
    public void itemSearchResult_launchesProductDetailForBarcode() {
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);
        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Intent data = new Intent();
        data.putExtra(FoodSearchActivity.EXTRA_BARCODE, "5000169105306");
        activity.onActivityResult(CarbLookupActivity.REQUEST_ITEM_SEARCH, Activity.RESULT_OK, data);

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(ProductDetailActivity.class.getName(), started.intent.getComponent().getClassName());
        assertEquals("5000169105306", started.intent.getStringExtra(ProductDetailActivity.EXTRA_BARCODE));
    }

    @Test
    public void commonFoodsPicker_hasGroupedRowsBackButtonAndStartsProductDetail() {
        TestCarbLookupActivity.sourceOverride = FoodDbSource.US;
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);
        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.findViewById(R.id.commonFoodsButton).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Common foods - Open Food Facts US", shadowOf(dialog).getTitle().toString());
        View categoryRow = dialog.getListView().getAdapter().getView(0, null, dialog.getListView());
        assertNotNull(categoryRow.getBackground());
        assertEquals("Starches, grains & breads",
                ((TextView) categoryRow.findViewById(R.id.standardFoodCategoryNameTextView)).getText().toString());
        assertEquals("20 items",
                ((TextView) categoryRow.findViewById(R.id.standardFoodCategoryCountTextView)).getText().toString());

        dialog.getListView().performItemClick(categoryRow, 0, 0L);
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Starches, grains & breads", shadowOf(dialog).getTitle().toString());
        View foodRow = dialog.getListView().getAdapter().getView(0, null, dialog.getListView());
        assertNotNull(foodRow.getBackground());
        assertEquals("Rice, white, cooked",
                ((TextView) foodRow.findViewById(R.id.standardFoodNameTextView)).getText().toString());
        assertEquals("Back", dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick();
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("Common foods - Open Food Facts US", shadowOf(dialog).getTitle().toString());

        dialog.getListView().performItemClick(categoryRow, 0, 0L);
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        foodRow = dialog.getListView().getAdapter().getView(0, null, dialog.getListView());
        dialog.getListView().performItemClick(foodRow, 0, 0L);

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(ProductDetailActivity.class.getName(), started.intent.getComponent().getClassName());
        assertEquals("Rice, white, cooked", started.intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertTrue(started.intent.getBooleanExtra(ProductDetailActivity.EXTRA_STANDARD_CARB_ITEM, false));
    }

    @Test
    public void commonFoodsPicker_displaysLiquidPortionsInMilliliters() {
        TestCarbLookupActivity.sourceOverride = FoodDbSource.US;
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);
        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.findViewById(R.id.commonFoodsButton).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        int dairyPosition = findCategoryPosition(dialog, StandardCarbCategory.DAIRY);
        View dairyRow = dialog.getListView().getAdapter().getView(dairyPosition, null, dialog.getListView());
        dialog.getListView().performItemClick(dairyRow, dairyPosition, dairyPosition);

        dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Dairy & alternatives", shadowOf(dialog).getTitle().toString());
        int milkPosition = findFoodPosition(dialog, "2% milk");
        View milkRow = dialog.getListView().getAdapter().getView(milkPosition, null, dialog.getListView());

        assertEquals("2% milk",
                ((TextView) milkRow.findViewById(R.id.standardFoodNameTextView)).getText().toString());
        assertEquals("4.8g carbs per 100ml",
                ((TextView) milkRow.findViewById(R.id.standardFoodCarbsTextView)).getText().toString());
        assertEquals("Small 100ml",
                ((TextView) milkRow.findViewById(R.id.standardFoodSmallTextView)).getText().toString());
        assertEquals("Medium 200ml",
                ((TextView) milkRow.findViewById(R.id.standardFoodMediumTextView)).getText().toString());
        assertEquals("Large 300ml",
                ((TextView) milkRow.findViewById(R.id.standardFoodLargeTextView)).getText().toString());

        dialog.getListView().performItemClick(milkRow, milkPosition, milkPosition);

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals("2% milk", started.intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals("ml", started.intent.getStringExtra(ProductDetailActivity.EXTRA_PORTION_UNIT));
        assertEquals(200.0, started.intent.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
    }

    @Test
    public void mealHistoryReuseResult_addsSavedMealItemsToCurrentMeal() {
        MealRepository mealRepository = mock(MealRepository.class);
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        when(mealRepository.getMealById(99L)).thenReturn(buildMeal(99L));
        TestCarbLookupActivity.mealRepositoryOverride = mealRepository;
        TestCarbLookupActivity.recipeRepositoryOverride = recipeRepository;

        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.handleMealHistoryReuseResult(99L);

        ListView mealList = activity.findViewById(R.id.currentMealListView);
        TextView carbsTotal = activity.findViewById(R.id.carbsResultTextView);
        assertEquals(1, mealList.getAdapter().getCount());
        assertEquals("Carbs: 18.5g", carbsTotal.getText().toString());
    }

    @Test
    public void favoriteResult_addsFavoriteItemToCurrentMeal() {
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);
        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Intent favoriteResult = new Intent();
        favoriteResult.putExtra(FavoriteItemsActivity.EXTRA_FAVORITE_ID, 7L);
        favoriteResult.putExtra(FavoriteItemsActivity.EXTRA_PRODUCT_NAME, "Toast");
        favoriteResult.putExtra(FavoriteItemsActivity.EXTRA_BRAND, "Bakery");
        favoriteResult.putExtra(FavoriteItemsActivity.EXTRA_BARCODE, "12345");
        favoriteResult.putExtra(FavoriteItemsActivity.EXTRA_CARBS_PER_100G, 45.0);
        favoriteResult.putExtra(FavoriteItemsActivity.EXTRA_PORTION_GRAMS, 40.0);

        activity.handleFavoriteResult(favoriteResult);

        ListView mealList = activity.findViewById(R.id.currentMealListView);
        TextView carbsTotal = activity.findViewById(R.id.carbsResultTextView);
        MealItem item = (MealItem) mealList.getAdapter().getItem(0);
        assertEquals(1, mealList.getAdapter().getCount());
        assertEquals("Toast", item.productName);
        assertEquals(7L, item.favoriteId);
        assertEquals("Carbs: 18.0g", carbsTotal.getText().toString());
    }

    @Test
    public void addTemplateButton_passesCurrentMealItemsToRecipeEditor() {
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);
        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Intent productResult = new Intent();
        ProductDetailContract.putProductResult(productResult,
                "Toast", "Bakery", "12345", 45.0, 41.1, 18.5);
        activity.onActivityResult(1002, Activity.RESULT_OK, productResult);

        activity.findViewById(R.id.addTemplateButton).performClick();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(RecipeEditActivity.class.getName(), started.getComponent().getClassName());
        java.util.List<com.eveningoutpost.dexdrip.carblookup.model.RecipeItem> seedItems =
            RecipeEditActivity.seedItemsFromIntent(started);
        assertEquals(1, seedItems.size());
        assertEquals("Toast", seedItems.get(0).productName);
        assertEquals("Bakery", seedItems.get(0).brand);
        assertEquals("12345", seedItems.get(0).barcode);
        assertEquals(45.0, seedItems.get(0).carbsPer100g, 0.01);
        assertEquals(41.1, seedItems.get(0).itemWeightGrams, 0.01);
        assertEquals(18.5, seedItems.get(0).itemCarbsGrams, 0.01);
    }

    @Test
    public void mealTimeSpinner_defaultsToSnackBetweenMealWindows() {
        TestCarbLookupActivity.hourOverride = 15;
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);

        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Spinner mealTime = activity.findViewById(R.id.currentMealTimeSpinner);
        assertEquals("Snack", mealTime.getSelectedItem().toString());
    }

    @Test
    public void mealClockTime_defaultsToCurrentTime() {
        TestCarbLookupActivity.currentTimeOverride = timestampAt(2026, Calendar.MAY, 31, 14, 7);
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);

        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        EditText mealClockTime = activity.findViewById(R.id.currentMealClockTimeEditText);
        assertEquals("14:07", mealClockTime.getText().toString());
    }

    @Test
    public void logMeal_usesEditedClockTimeForSavedTimestamp() {
        CapturingMealLogService mealLogService = new CapturingMealLogService(mock(MealRepository.class));
        TestCarbLookupActivity.currentTimeOverride = timestampAt(2026, Calendar.MAY, 31, 14, 7);
        TestCarbLookupActivity.mealRepositoryOverride = mock(MealRepository.class);
        TestCarbLookupActivity.recipeRepositoryOverride = mock(RecipeRepository.class);
        TestCarbLookupActivity.mealLogServiceOverride = mealLogService;
        TestCarbLookupActivity activity = Robolectric.buildActivity(TestCarbLookupActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Intent productResult = new Intent();
        ProductDetailContract.putProductResult(productResult,
                "Toast", "Bakery", "12345", 45.0, 41.1, 18.5);
        activity.onActivityResult(1002, Activity.RESULT_OK, productResult);
        ((Spinner) activity.findViewById(R.id.currentMealTimeSpinner)).setSelection(1);
        ((EditText) activity.findViewById(R.id.currentMealClockTimeEditText)).setText("9:15");

        activity.findViewById(R.id.logMealButton).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, mealLogService.logCalls);
        assertEquals(timestampAt(2026, Calendar.MAY, 31, 9, 15), mealLogService.loggedTimestamp);
        assertTrue(mealLogService.loggedMealName.endsWith("09:15"));
        assertEquals("breakfast", mealLogService.loggedMealTime);
    }

    private MealSummary buildMeal(long mealId) {
        MealSummary meal = new MealSummary();
        meal.id = mealId;
        meal.name = "Reusable Meal";
        meal.savedAt = 1000L;
        meal.totalCarbs = 18.5;
        meal.itemCount = 1;

        MealItem item = new MealItem();
        item.productName = "Toast";
        item.carbsPer100g = 45.0;
        item.portionGrams = 41.1;
        item.carbsForPortion = 18.5;
        meal.items.add(item);
        return meal;
    }

    private int findCategoryPosition(AlertDialog dialog, StandardCarbCategory category) {
        for (int i = 0; i < dialog.getListView().getAdapter().getCount(); i++) {
            if (category == dialog.getListView().getAdapter().getItem(i)) {
                return i;
            }
        }
        throw new AssertionError("Missing category: " + category);
    }

    private int findFoodPosition(AlertDialog dialog, String foodName) {
        for (int i = 0; i < dialog.getListView().getAdapter().getCount(); i++) {
            Object item = dialog.getListView().getAdapter().getItem(i);
            if (item instanceof StandardCarbItem && foodName.equals(((StandardCarbItem) item).name)) {
                return i;
            }
        }
        throw new AssertionError("Missing food: " + foodName);
    }

    private long timestampAt(int year, int month, int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static class CapturingMealLogService extends MealLogService {
        int logCalls;
        long loggedTimestamp;
        String loggedMealName;
        String loggedMealTime;

        CapturingMealLogService(MealRepository mealRepository) {
            super(mealRepository);
        }

        @Override
        MealSummary logMeal(String mealName, double totalCarbs, List<MealItem> items, String notes,
                String mealTime, long timestamp) {
            logCalls++;
            loggedTimestamp = timestamp;
            loggedMealName = mealName;
            loggedMealTime = mealTime;
            return buildMeal(mealName, totalCarbs, timestamp, items, notes, mealTime);
        }
    }

    public static class TestCarbLookupActivity extends CarbLookupActivity {
        static MealRepository mealRepositoryOverride;
        static RecipeRepository recipeRepositoryOverride;
        static MealLogService mealLogServiceOverride;
        static FoodDbSource sourceOverride;
        static int hourOverride;
        static Long currentTimeOverride;

        void handleMealHistoryReuseResult(long mealId) {
            Intent data = new Intent();
            data.putExtra(MealDetailActivity.EXTRA_REUSE_MEAL_ID, mealId);
            onActivityResult(1005, Activity.RESULT_OK, data);
        }

        void handleFavoriteResult(Intent data) {
            onActivityResult(1004, Activity.RESULT_OK, data);
        }

        @Override
        protected MealRepository createMealRepository() {
            return mealRepositoryOverride != null ? mealRepositoryOverride : super.createMealRepository();
        }

        @Override
        protected RecipeRepository createRecipeRepository() {
            return recipeRepositoryOverride != null ? recipeRepositoryOverride : super.createRecipeRepository();
        }

        @Override
        protected MealLogService createMealLogService(MealRepository mealRepository) {
            return mealLogServiceOverride != null ? mealLogServiceOverride : super.createMealLogService(mealRepository);
        }

        @Override
        protected FoodDbSource createFoodDbSource() {
            return sourceOverride != null ? sourceOverride : super.createFoodDbSource();
        }

        @Override
        protected int getCurrentHourOfDay() {
            return hourOverride;
        }

        @Override
        protected long getCurrentTimeMillis() {
            return currentTimeOverride != null ? currentTimeOverride : super.getCurrentTimeMillis();
        }
    }
}
