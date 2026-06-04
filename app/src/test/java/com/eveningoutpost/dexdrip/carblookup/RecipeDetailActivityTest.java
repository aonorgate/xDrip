package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;
import com.eveningoutpost.dexdrip.carblookup.model.PortionResult;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecipeDetailActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetOverrides() {
        TestRecipeDetailActivity.recipeRepositoryOverride = null;
        TestRecipeDetailActivity.mealRepositoryOverride = null;
        TestRecipeDetailActivity.logCalls = 0;
    }

    @Test
    public void invalidPortionClearsStateAndPreventsLogging() {
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        MealRepository mealRepository = mock(MealRepository.class);
        Recipe recipe = buildRecipe();
        PortionResult validResult = buildResult(42L, 1.0, 32.7, 9.4, true);
        PortionResult scaledResult = buildResult(42L, 2.5, 81.8, 23.5, true);

        when(recipeRepository.getRecipeById(42L)).thenReturn(recipe);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(1.0))).thenReturn(validResult);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(2.5))).thenReturn(scaledResult);

        TestRecipeDetailActivity.recipeRepositoryOverride = recipeRepository;
        TestRecipeDetailActivity.mealRepositoryOverride = mealRepository;

        TestRecipeDetailActivity activity = Robolectric.buildActivity(
                        TestRecipeDetailActivity.class,
                        buildIntent(42L))
                .create()
                .start()
                .resume()
                .get();

        EditText portions = activity.findViewById(R.id.portionsEditText);
        TextView totalSummary = activity.findViewById(R.id.totalCarbsSummaryTextView);
        TextView glEstimate = activity.findViewById(R.id.glEstimateTextView);
        ListView items = activity.findViewById(R.id.detailItemListView);
        Button logMealButton = activity.findViewById(R.id.logMealButton);

        assertTrue(totalSummary.getText().toString().contains("32.7g"));
        // Probe: verify the adapter actually reflects items before clearing
        assertEquals("Adapter should show 1 item before clearing", 1, items.getAdapter().getCount());

        portions.setText("0");

        assertEquals("", totalSummary.getText().toString());
        assertEquals(View.GONE, glEstimate.getVisibility());
        assertEquals(0, items.getAdapter().getCount());

        logMealButton.performClick();

        verify(mealRepository, never()).saveMeal(org.mockito.ArgumentMatchers.any(MealSummary.class));
        verify(recipeRepository, never()).incrementUseCount(anyLong());
        assertEquals(0, TestRecipeDetailActivity.logCalls);
    }

    @Test
    public void zeroCarbResult_doesNotLogMeal() {
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        MealRepository mealRepository = mock(MealRepository.class);
        Recipe recipe = buildRecipe();
        PortionResult zeroCarbResult = buildResult(42L, 1.0, 0.0, 0.0, false);

        when(recipeRepository.getRecipeById(42L)).thenReturn(recipe);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(1.0))).thenReturn(zeroCarbResult);

        TestRecipeDetailActivity.recipeRepositoryOverride = recipeRepository;
        TestRecipeDetailActivity.mealRepositoryOverride = mealRepository;

        TestRecipeDetailActivity activity = Robolectric.buildActivity(
                        TestRecipeDetailActivity.class,
                        buildIntent(42L))
                .create()
                .start()
                .resume()
                .get();

        Button logMealButton = activity.findViewById(R.id.logMealButton);
        logMealButton.performClick();

        verify(mealRepository, never()).saveMeal(org.mockito.ArgumentMatchers.any(MealSummary.class));
        verify(recipeRepository, never()).incrementUseCount(anyLong());
        assertEquals(0, TestRecipeDetailActivity.logCalls);
        assertEquals("No carbs to log", ShadowToast.getTextOfLatestToast());
    }

    @Test
    public void pickMode_addToCurrentMealReturnsRecipeIdAndPortions() {
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        MealRepository mealRepository = mock(MealRepository.class);
        Recipe recipe = buildRecipe();
        PortionResult validResult = buildResult(42L, 1.0, 32.7, 9.4, true);
        PortionResult scaledResult = buildResult(42L, 2.5, 81.8, 23.5, true);

        when(recipeRepository.getRecipeById(42L)).thenReturn(recipe);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(1.0))).thenReturn(validResult);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(2.5))).thenReturn(scaledResult);

        TestRecipeDetailActivity.recipeRepositoryOverride = recipeRepository;
        TestRecipeDetailActivity.mealRepositoryOverride = mealRepository;

        Intent intent = buildIntent(42L);
        intent.putExtra(RecipeDetailActivity.EXTRA_PICK_MODE, true);
        TestRecipeDetailActivity activity = Robolectric.buildActivity(
                        TestRecipeDetailActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        Button addButton = activity.findViewById(R.id.addToCurrentMealButton);
        assertEquals(View.VISIBLE, addButton.getVisibility());
        EditText portionsInput = activity.findViewById(R.id.portionsEditText);
        portionsInput.setText("2.5");

        addButton.performClick();

        assertEquals(android.app.Activity.RESULT_OK, org.robolectric.Shadows.shadowOf(activity).getResultCode());
        assertEquals(42L, org.robolectric.Shadows.shadowOf(activity).getResultIntent()
                .getLongExtra(RecipeListActivity.EXTRA_RECIPE_ID, 0L));
        assertEquals(2.5, org.robolectric.Shadows.shadowOf(activity).getResultIntent()
            .getDoubleExtra(RecipeDetailActivity.EXTRA_PORTIONS, 0.0), 0.01);
        verify(recipeRepository, never()).incrementUseCount(anyLong());
        assertEquals(0, TestRecipeDetailActivity.logCalls);
    }

    @Test
    public void normalMode_addToCurrentMealReturnsRecipeIdAndPortions() {
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        MealRepository mealRepository = mock(MealRepository.class);
        Recipe recipe = buildRecipe();
        PortionResult validResult = buildResult(42L, 1.0, 32.7, 9.4, true);

        when(recipeRepository.getRecipeById(42L)).thenReturn(recipe);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(1.0))).thenReturn(validResult);

        TestRecipeDetailActivity.recipeRepositoryOverride = recipeRepository;
        TestRecipeDetailActivity.mealRepositoryOverride = mealRepository;

        TestRecipeDetailActivity activity = Robolectric.buildActivity(
                TestRecipeDetailActivity.class,
                buildIntent(42L))
            .create()
            .start()
            .resume()
            .get();

        Button addButton = activity.findViewById(R.id.addToCurrentMealButton);
        assertEquals(View.VISIBLE, addButton.getVisibility());

        addButton.performClick();

        assertEquals(android.app.Activity.RESULT_OK, org.robolectric.Shadows.shadowOf(activity).getResultCode());
        assertEquals(42L, org.robolectric.Shadows.shadowOf(activity).getResultIntent()
            .getLongExtra(RecipeListActivity.EXTRA_RECIPE_ID, 0L));
        assertEquals(1.0, org.robolectric.Shadows.shadowOf(activity).getResultIntent()
            .getDoubleExtra(RecipeDetailActivity.EXTRA_PORTIONS, 0.0), 0.01);
    }

    @Test
    public void deleteRecipeIcon_confirmsAndDeletesStoredRecipe() {
        RecipeRepository recipeRepository = mock(RecipeRepository.class);
        MealRepository mealRepository = mock(MealRepository.class);
        Recipe recipe = buildRecipe();
        PortionResult validResult = buildResult(42L, 1.0, 32.7, 9.4, true);

        when(recipeRepository.getRecipeById(42L)).thenReturn(recipe);
        when(recipeRepository.getCarbsBreakdownForPortions(eq(42L), eq(1.0))).thenReturn(validResult);

        TestRecipeDetailActivity.recipeRepositoryOverride = recipeRepository;
        TestRecipeDetailActivity.mealRepositoryOverride = mealRepository;

        TestRecipeDetailActivity activity = Robolectric.buildActivity(
                        TestRecipeDetailActivity.class,
                        buildIntent(42L))
                .create()
                .start()
                .resume()
                .get();

        ImageButton deleteButton = activity.findViewById(R.id.deleteRecipeButton);
        assertNotNull(deleteButton.getDrawable());
        assertEquals(activity.getString(R.string.carblookup_context_delete),
            deleteButton.getContentDescription().toString());
        deleteButton.performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Delete recipe?", org.robolectric.Shadows.shadowOf(dialog).getTitle().toString());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        verify(recipeRepository).deleteRecipe(42L);
        assertEquals(android.app.Activity.RESULT_CANCELED,
                org.robolectric.Shadows.shadowOf(activity).getResultCode());
        assertTrue(activity.isFinishing());
        assertEquals("Recipe deleted", ShadowToast.getTextOfLatestToast());
    }

    private Intent buildIntent(long recipeId) {
        Intent intent = new Intent();
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipeId);
        return intent;
    }

    private Recipe buildRecipe() {
        Recipe recipe = new Recipe();
        recipe.id = 42L;
        recipe.name = "Weetabix Breakfast";
        recipe.portionCount = 1;
        recipe.items = new ArrayList<>();

        RecipeItem item = new RecipeItem();
        item.productName = "Weetabix";
        item.itemWeightGrams = 37.5;
        item.itemCarbsGrams = 25.7;
        recipe.items.add(item);
        return recipe;
    }

    private PortionResult buildResult(long recipeId, double portions, double totalCarbs, double totalGl, boolean partialGl) {
        PortionResult result = new PortionResult();
        result.setRecipeFound(true);
        result.setRecipeId(recipeId);
        result.setRecipeName("Weetabix Breakfast");
        result.setNumberOfPortions(portions);
        result.setCarbsPerSinglePortion(totalCarbs);
        result.setTotalCarbsGrams(totalCarbs);
        result.setTotalGlEstimate(totalGl);
        result.setGlIsPartial(partialGl);

        ArrayList<PortionItemResult> items = new ArrayList<>();
        PortionItemResult item = new PortionItemResult();
        item.setProductName("Weetabix");
        item.setScaledWeightGrams(37.5);
        item.setScaledCarbsGrams(totalCarbs);
        item.setGiEstimate(70);
        item.setScaledGlEstimate(totalGl);
        items.add(item);
        result.setItems(items);
        return result;
    }

    public static class TestRecipeDetailActivity extends RecipeDetailActivity {
        static RecipeRepository recipeRepositoryOverride;
        static MealRepository mealRepositoryOverride;
        static int logCalls;

        @Override
        protected RecipeRepository createRecipeRepository() {
            return recipeRepositoryOverride != null ? recipeRepositoryOverride : super.createRecipeRepository();
        }

        @Override
        protected MealRepository createMealRepository() {
            return mealRepositoryOverride != null ? mealRepositoryOverride : super.createMealRepository();
        }

        @Override
        protected MealLogService createMealLogService(MealRepository mealRepository) {
            return new MealLogService(mealRepository) {
                @Override
                MealSummary logMeal(String mealName, double totalCarbs, List<com.eveningoutpost.dexdrip.carblookup.model.MealItem> items) {
                    logCalls++;
                    return buildMeal(mealName, totalCarbs, System.currentTimeMillis(), items);
                }
            };
        }
    }
}