package com.eveningoutpost.dexdrip.carblookup;

import android.content.SharedPreferences;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class MealHistoryActivityTest extends RobolectricTestWithConfig {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long NOW = 1_800_000_000_000L;

    @Before
    public void resetOverrides() {
        TestMealHistoryActivity.mealRepositoryOverride = null;
        TestMealHistoryActivity.nowOverride = NOW;
        SharedPreferences preferences = Pref.getInstance();
        if (preferences != null) {
            preferences.edit().remove(MealHistoryActivity.PREF_HISTORY_RETENTION_DAYS).commit();
        }
    }

    @Test
    public void mealRowsShowNotesFollowedByMealTime() {
        MealRepository repository = mock(MealRepository.class);
        when(repository.getMealsSince(anyLong())).thenReturn(Collections.singletonList(buildMeal()));
        TestMealHistoryActivity.mealRepositoryOverride = repository;

        TestMealHistoryActivity activity = Robolectric.buildActivity(TestMealHistoryActivity.class)
                .create()
                .start()
                .resume()
                .get();

        ListView listView = activity.findViewById(R.id.mealListView);
        View row = listView.getAdapter().getView(0, null, listView);
        TextView detail = row.findViewById(R.id.mealDetailTextView);

        assertTrue(detail.getText().toString().contains("Cornflakes with milk - Breakfast"));
    }

    @Test
    public void historyRetentionDefaultsToNinetyDays() {
        MealRepository repository = mock(MealRepository.class);
        when(repository.getMealsSince(anyLong())).thenReturn(Collections.emptyList());
        TestMealHistoryActivity.mealRepositoryOverride = repository;

        TestMealHistoryActivity activity = Robolectric.buildActivity(TestMealHistoryActivity.class)
                .create()
                .start()
                .resume()
                .get();

        Spinner spinner = activity.findViewById(R.id.historyRetentionSpinner);
        assertEquals("90 days", spinner.getSelectedItem().toString());
        assertTrue(capturedCutoffs(repository).getAllValues().contains(NOW - 90L * DAY_MS));
    }

    @Test
    public void selectingRetentionWindowPersistsAndReloadsHistory() {
        MealRepository repository = mock(MealRepository.class);
        when(repository.getMealsSince(anyLong())).thenReturn(Collections.emptyList());
        TestMealHistoryActivity.mealRepositoryOverride = repository;

        TestMealHistoryActivity activity = Robolectric.buildActivity(TestMealHistoryActivity.class)
                .create()
                .start()
                .resume()
                .get();
        clearInvocations(repository);

        Spinner spinner = activity.findViewById(R.id.historyRetentionSpinner);
        spinner.setSelection(2);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("180", Pref.getString(MealHistoryActivity.PREF_HISTORY_RETENTION_DAYS, ""));
        assertTrue(capturedCutoffs(repository).getAllValues().contains(NOW - 180L * DAY_MS));
    }

    private ArgumentCaptor<Long> capturedCutoffs(MealRepository repository) {
        ArgumentCaptor<Long> cutoffCaptor = ArgumentCaptor.forClass(Long.class);
        verify(repository, atLeastOnce()).getMealsSince(cutoffCaptor.capture());
        return cutoffCaptor;
    }

    private MealSummary buildMeal() {
        MealSummary meal = new MealSummary("Breakfast 08:00", 32.7);
        meal.savedAt = NOW;
        meal.notes = "Cornflakes with milk";
        meal.mealTime = "breakfast";
        meal.itemCount = 1;
        return meal;
    }

    public static class TestMealHistoryActivity extends MealHistoryActivity {
        static MealRepository mealRepositoryOverride;
        static long nowOverride;

        @Override
        protected MealRepository createMealRepository() {
            return mealRepositoryOverride != null ? mealRepositoryOverride : super.createMealRepository();
        }

        @Override
        protected long getCurrentTimeMillis() {
            return nowOverride;
        }
    }
}