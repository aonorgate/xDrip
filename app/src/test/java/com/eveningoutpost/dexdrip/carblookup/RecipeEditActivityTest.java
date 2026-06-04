package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.db.ProductCacheRepository;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowActivity.IntentForResult;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class RecipeEditActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetOverrides() {
        TestRecipeEditActivity.recipeRepositoryOverride = null;
        TestRecipeEditActivity.cacheRepositoryOverride = null;
        TestRecipeEditActivity.sourceOverride = FoodDbSource.WORLD;
    }

    @Test
    public void newRecipe_defaultsPortionCountToOne() {
        RecipeEditActivity activity = Robolectric.buildActivity(RecipeEditActivity.class)
                .create()
                .start()
                .resume()
                .get();

        EditText portions = activity.findViewById(R.id.portionCountEditText);
        TextView totalCarbs = activity.findViewById(R.id.totalCarbsTextView);

        assertEquals("1", portions.getText().toString());
        assertTrue(totalCarbs.getText().toString().contains("Per portion: 0.0g"));
    }

    @Test
    public void newRecipeWithSeedItems_prefillsIngredientList() {
        RecipeItem seedItem = new RecipeItem();
        seedItem.productName = "Toast";
        seedItem.brand = "Bakery";
        seedItem.barcode = "12345";
        seedItem.carbsPer100g = 45.0;
        seedItem.itemWeightGrams = 41.1;
        seedItem.itemCarbsGrams = 18.5;
        seedItem.giEstimate = 70;
        Intent intent = RecipeEditActivity.newRecipeWithItems(RuntimeEnvironment.getApplication(),
                java.util.Collections.singletonList(seedItem));

        TestRecipeEditActivity activity = Robolectric.buildActivity(TestRecipeEditActivity.class, intent)
                .create()
                .start()
                .resume()
                .get();

        ListView listView = activity.findViewById(R.id.recipeItemsListView);
        RecipeItem copied = (RecipeItem) listView.getAdapter().getItem(0);
        TextView totalCarbs = activity.findViewById(R.id.totalCarbsTextView);

        assertEquals(1, listView.getAdapter().getCount());
        assertEquals("Toast", copied.productName);
        assertEquals("Bakery", copied.brand);
        assertEquals("12345", copied.barcode);
        assertEquals(45.0, copied.carbsPer100g, 0.01);
        assertEquals(41.1, copied.itemWeightGrams, 0.01);
        assertEquals(18.5, copied.itemCarbsGrams, 0.01);
        assertEquals(70, copied.giEstimate);
        assertTrue(totalCarbs.getText().toString().contains("Per portion: 18.5g"));
    }

    @Test
    public void editIngredient_launchesManualEntryWithExistingValues() {
        RecipeRepository repository = mock(RecipeRepository.class);
        TestRecipeEditActivity.recipeRepositoryOverride = repository;
        TestRecipeEditActivity.cacheRepositoryOverride = mock(ProductCacheRepository.class);
        when(repository.getRecipeById(42L)).thenReturn(buildRecipe());

        Intent intent = new Intent();
        intent.putExtra(RecipeEditActivity.EXTRA_RECIPE_ID, 42L);
        TestRecipeEditActivity activity = Robolectric.buildActivity(
                        TestRecipeEditActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        ListView listView = activity.findViewById(R.id.recipeItemsListView);
        View row = listView.getAdapter().getView(0, null, listView);
        ImageButton editButton = row.findViewById(R.id.editItemButton);
        editButton.performClick();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(ProductDetailActivity.class.getName(), started.getComponent().getClassName());
        assertTrue(started.getBooleanExtra(ProductDetailActivity.EXTRA_MANUAL_ENTRY, false));
        assertEquals("Oats", started.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertEquals("Kitchen", started.getStringExtra(ProductDetailActivity.EXTRA_BRAND));
        assertEquals("12345", started.getStringExtra(ProductDetailActivity.EXTRA_BARCODE));
        assertEquals(61.2, started.getDoubleExtra(ProductDetailActivity.EXTRA_CARBS_PER_100G, 0.0), 0.01);
        assertEquals(40.0, started.getDoubleExtra(ProductDetailActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
    }

    @Test
    public void existingRecipe_deleteTemplateButtonConfirmsAndDeletesRecipe() {
        RecipeRepository repository = mock(RecipeRepository.class);
        TestRecipeEditActivity.recipeRepositoryOverride = repository;
        TestRecipeEditActivity.cacheRepositoryOverride = mock(ProductCacheRepository.class);
        when(repository.getRecipeById(42L)).thenReturn(buildRecipe());

        Intent intent = new Intent();
        intent.putExtra(RecipeEditActivity.EXTRA_RECIPE_ID, 42L);
        TestRecipeEditActivity activity = Robolectric.buildActivity(
                        TestRecipeEditActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        ImageButton deleteButton = activity.findViewById(R.id.deleteTemplateButton);
        assertEquals(View.VISIBLE, deleteButton.getVisibility());
        assertNotNull(deleteButton.getDrawable());
        assertEquals(activity.getString(R.string.carblookup_context_delete),
                deleteButton.getContentDescription().toString());

        deleteButton.performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Delete recipe?", shadowOf(dialog).getTitle().toString());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        verify(repository).deleteRecipe(42L);
        assertTrue(activity.isFinishing());
        assertEquals("Recipe deleted", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void itemSearchButton_startsFoodSearchPicker() {
        TestRecipeEditActivity activity = Robolectric.buildActivity(TestRecipeEditActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.findViewById(R.id.itemSearchButton).performClick();

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(FoodSearchActivity.class.getName(), started.intent.getComponent().getClassName());
        assertEquals(RecipeEditActivity.REQUEST_ITEM_SEARCH, started.requestCode);
    }

    @Test
    public void itemSearchResult_launchesProductDetailForBarcode() {
        TestRecipeEditActivity activity = Robolectric.buildActivity(TestRecipeEditActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Intent data = new Intent();
        data.putExtra(FoodSearchActivity.EXTRA_BARCODE, "5000169105306");
        activity.onActivityResult(RecipeEditActivity.REQUEST_ITEM_SEARCH, Activity.RESULT_OK, data);

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(ProductDetailActivity.class.getName(), started.intent.getComponent().getClassName());
        assertEquals("5000169105306", started.intent.getStringExtra(ProductDetailActivity.EXTRA_BARCODE));
    }

    @Test
    public void commonFoodsPicker_startsProductDetailForRecipeIngredient() {
        TestRecipeEditActivity.sourceOverride = FoodDbSource.US;
        TestRecipeEditActivity activity = Robolectric.buildActivity(TestRecipeEditActivity.class)
                .create()
                .start()
                .resume()
                .get();

        activity.findViewById(R.id.commonFoodsButton).performClick();
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Common foods - Open Food Facts US", shadowOf(dialog).getTitle().toString());
        View categoryRow = dialog.getListView().getAdapter().getView(0, null, dialog.getListView());

        dialog.getListView().performItemClick(categoryRow, 0, 0L);
        dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Starches, grains & breads", shadowOf(dialog).getTitle().toString());
        View foodRow = dialog.getListView().getAdapter().getView(0, null, dialog.getListView());
        assertEquals("Back", dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());

        dialog.getListView().performItemClick(foodRow, 0, 0L);

        IntentForResult started = shadowOf(activity).getNextStartedActivityForResult();
        assertNotNull(started);
        assertEquals(ProductDetailActivity.class.getName(), started.intent.getComponent().getClassName());
        assertEquals("Rice, white, cooked", started.intent.getStringExtra(ProductDetailActivity.EXTRA_PRODUCT_NAME));
        assertTrue(started.intent.getBooleanExtra(ProductDetailActivity.EXTRA_STANDARD_CARB_ITEM, false));
    }

    private Recipe buildRecipe() {
        Recipe recipe = new Recipe();
        recipe.id = 42L;
        recipe.name = "Breakfast";
        recipe.portionCount = 1.0;
        recipe.mealTime = "breakfast";
        RecipeItem item = new RecipeItem();
        item.productName = "Oats";
        item.brand = "Kitchen";
        item.barcode = "12345";
        item.carbsPer100g = 61.2;
        item.itemWeightGrams = 40.0;
        item.itemCarbsGrams = 24.5;
        recipe.items.add(item);
        return recipe;
    }

    public static class TestRecipeEditActivity extends RecipeEditActivity {
        static RecipeRepository recipeRepositoryOverride;
        static ProductCacheRepository cacheRepositoryOverride;
        static FoodDbSource sourceOverride;

        @Override
        protected RecipeRepository createRecipeRepository() {
            return recipeRepositoryOverride != null ? recipeRepositoryOverride : super.createRecipeRepository();
        }

        @Override
        protected ProductCacheRepository createProductCacheRepository() {
            return cacheRepositoryOverride != null ? cacheRepositoryOverride : super.createProductCacheRepository();
        }

        @Override
        protected FoodDbSource createFoodDbSource() {
            return sourceOverride != null ? sourceOverride : super.createFoodDbSource();
        }
    }
}