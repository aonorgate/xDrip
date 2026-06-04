package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.models.Treatments;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MealLogServiceTest {

    @Test
    public void logMeal_savesCopiedItemsNotesAndRefreshes() {
        MealRepository repository = mock(MealRepository.class);
        TestMealLogService service = new TestMealLogService(repository);
        List<MealItem> items = new ArrayList<>();
        MealItem item = new MealItem();
        item.productName = "Toast";
        item.portionGrams = 42.0;
        item.carbsForPortion = 18.5;
        items.add(item);
        long timestamp = 1_800_000_054_000L;

        service.logMeal("Breakfast", 18.5, items, "estimated", "breakfast", timestamp);

        ArgumentCaptor<MealSummary> captor = ArgumentCaptor.forClass(MealSummary.class);
        verify(repository).saveMeal(captor.capture());
        MealSummary saved = captor.getValue();
        assertEquals("Breakfast", saved.name);
        assertEquals(18.5, saved.totalCarbs, 0.01);
        assertEquals(timestamp, saved.savedAt);
        assertEquals("estimated", saved.notes);
        assertEquals("breakfast", saved.mealTime);
        assertEquals(1, saved.itemCount);
        assertEquals("Toast", saved.items.get(0).productName);
        assertNotSame(item, saved.items.get(0));
        assertNull(saved.treatmentUuid);
        assertEquals(timestamp, service.lastTreatmentTimestamp);
        assertEquals(1, service.refreshCalls);
        assertEquals(1, service.treatmentCalls);
    }

    private static class TestMealLogService extends MealLogService {
        int treatmentCalls;
        int refreshCalls;
        long lastTreatmentTimestamp;

        TestMealLogService(MealRepository mealRepository) {
            super(mealRepository);
        }

        @Override
        protected Treatments createTreatment(double carbs, long timestamp) {
            treatmentCalls++;
            lastTreatmentTimestamp = timestamp;
            return null;
        }

        @Override
        protected void refreshCharts() {
            refreshCalls++;
        }
    }
}