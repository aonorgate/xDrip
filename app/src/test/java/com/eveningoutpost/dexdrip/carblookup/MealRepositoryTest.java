package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MealRepositoryTest extends RobolectricTestWithConfig {
    private final List<CarbLookupDatabase> testDatabases = new ArrayList<>();

    @After
    public void closeTestDatabases() {
        for (CarbLookupDatabase database : testDatabases) {
            database.close();
        }
        testDatabases.clear();
    }

    @Test
    public void savedMeals_preserveMealTimeAndRetentionFiltersBySavedAt() {
        MealRepository repository = createRepository();
        long now = 1_800_000_000_000L;
        long oldMealId = repository.saveMeal(meal("Old meal", now - days(100), "Toast", "breakfast"));
        long recentMealId = repository.saveMeal(meal("Recent meal", now - days(10), "Cornflakes with milk", "lunch"));

        List<MealSummary> allMeals = repository.getAllMeals();
        assertEquals(2, allMeals.size());
        assertEquals(recentMealId, allMeals.get(0).id);
        assertEquals("lunch", allMeals.get(0).mealTime);
        assertEquals("Cornflakes with milk", allMeals.get(0).notes);

        List<MealSummary> recentMeals = repository.getMealsSince(now - days(30));
        assertEquals(1, recentMeals.size());
        assertEquals(recentMealId, recentMeals.get(0).id);

        MealSummary oldMeal = repository.getMealById(oldMealId);
        assertEquals("breakfast", oldMeal.mealTime);
        assertEquals(1, oldMeal.items.size());
    }

    private MealRepository createRepository() {
        CarbLookupDatabase database = CarbLookupDatabase.createInMemoryInstance(
                RuntimeEnvironment.getApplication());
        testDatabases.add(database);
        return new MealRepository(database);
    }

    private MealSummary meal(String name, long savedAt, String notes, String mealTime) {
        MealSummary meal = new MealSummary(name, 18.5);
        meal.savedAt = savedAt;
        meal.notes = notes;
        meal.mealTime = mealTime;

        MealItem item = new MealItem();
        item.productName = name;
        item.carbsPer100g = 45.0;
        item.portionGrams = 40.0;
        item.carbsForPortion = 18.5;
        meal.items.add(item);
        return meal;
    }

    private long days(int days) {
        return days * 24L * 60L * 60L * 1000L;
    }
}