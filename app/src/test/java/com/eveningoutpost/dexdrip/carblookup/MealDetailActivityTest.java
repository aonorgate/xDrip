package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.widget.ImageButton;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class MealDetailActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetOverrides() {
        TestMealDetailActivity.mealRepositoryOverride = null;
        TestMealDetailActivity.treatmentDeleteCalls = 0;
    }

    @Test
    public void addToMealButton_returnsMealIdForReuse() {
        MealRepository repository = mock(MealRepository.class);
        when(repository.getMealById(42L)).thenReturn(buildMeal(42L));
        TestMealDetailActivity.mealRepositoryOverride = repository;

        TestMealDetailActivity activity = Robolectric.buildActivity(
                        TestMealDetailActivity.class,
                        buildIntent(42L))
                .create()
                .start()
                .resume()
                .get();

        ImageButton addToMeal = activity.findViewById(R.id.addToMealButton);
        addToMeal.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals(42L, result.getLongExtra(MealDetailActivity.EXTRA_REUSE_MEAL_ID, 0L));
    }

    @Test
    public void deleteMealButton_confirmsAndDeletesMeal() {
        MealRepository repository = mock(MealRepository.class);
        when(repository.getMealById(42L)).thenReturn(buildMeal(42L));
        TestMealDetailActivity.mealRepositoryOverride = repository;

        TestMealDetailActivity activity = Robolectric.buildActivity(
                        TestMealDetailActivity.class,
                        buildIntent(42L))
                .create()
                .start()
                .resume()
                .get();

        ImageButton deleteMeal = activity.findViewById(R.id.deleteMealButton);
        deleteMeal.performClick();

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertEquals("Delete meal?", shadowOf(dialog).getTitle().toString());

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        verify(repository).deleteMeal(42L);
        assertEquals(1, TestMealDetailActivity.treatmentDeleteCalls);
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).getResultCode());
        assertTrue(activity.isFinishing());
        assertEquals("Meal deleted", ShadowToast.getTextOfLatestToast());
    }

    private Intent buildIntent(long mealId) {
        Intent intent = new Intent();
        intent.putExtra(MealDetailActivity.EXTRA_MEAL_ID, mealId);
        return intent;
    }

    private MealSummary buildMeal(long mealId) {
        MealSummary meal = new MealSummary();
        meal.id = mealId;
        meal.name = "Breakfast";
        meal.savedAt = 1000L;
        meal.totalCarbs = 18.5;
        meal.itemCount = 1;

        MealItem item = new MealItem();
        item.productName = "Toast";
        item.carbsPer100g = 45.0;
        item.portionGrams = 40.0;
        item.carbsForPortion = 18.0;
        meal.items.add(item);
        return meal;
    }

    public static class TestMealDetailActivity extends MealDetailActivity {
        static MealRepository mealRepositoryOverride;
        static int treatmentDeleteCalls;

        @Override
        protected MealRepository createMealRepository() {
            return mealRepositoryOverride != null ? mealRepositoryOverride : super.createMealRepository();
        }

        @Override
        protected void deleteAssociatedTreatment(MealSummary meal) {
            treatmentDeleteCalls++;
        }
    }
}
