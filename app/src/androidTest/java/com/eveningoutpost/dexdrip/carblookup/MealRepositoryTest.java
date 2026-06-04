package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented tests for MealRepository.
 * Verifies meal save, retrieve, item persistence, and ordering.
 */
@RunWith(JUnit4.class)
public class MealRepositoryTest {

    private static final double DELTA = 0.01;

    private MealRepository repository;
    private CarbLookupDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = CarbLookupDatabase.createInMemoryInstance(context);
        repository = new MealRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Save and retrieve
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void saveMeal_returnsPositiveId() {
        MealSummary meal = buildSimpleMeal();
        long id = repository.saveMeal(meal);
        assertTrue("saveMeal should return a positive ID", id > 0);
    }

    @Test
    public void saveMeal_updatesObjectId() {
        MealSummary meal = buildSimpleMeal();
        long id = repository.saveMeal(meal);
        assertEquals("meal.id should be updated after save", id, meal.id);
    }

    @Test
    public void getMealById_retrievesSavedMeal() {
        MealSummary original = buildSimpleMeal();
        long id = repository.saveMeal(original);

        MealSummary retrieved = repository.getMealById(id);

        assertNotNull("Retrieved meal should not be null", retrieved);
        assertEquals("Name should match", original.name, retrieved.name);
        assertEquals("Total carbs should match", original.totalCarbs, retrieved.totalCarbs, DELTA);
    }

    @Test
    public void getMealById_nonExistent_returnsNull() {
        assertNull(repository.getMealById(99999L));
    }

    @Test
    public void saveMeal_itemsPersisted() {
        MealSummary meal = buildMealWithItems();
        long id = repository.saveMeal(meal);

        MealSummary retrieved = repository.getMealById(id);

        assertNotNull(retrieved);
        assertEquals("Item count should match", 2, retrieved.items.size());
    }

    @Test
    public void saveMeal_itemFieldsCorrect() {
        MealSummary meal = buildMealWithItems();
        long id = repository.saveMeal(meal);

        MealSummary retrieved = repository.getMealById(id);
        MealItem item = retrieved.items.get(0);

        assertEquals("Weetabix", item.productName);
        assertEquals("Weetabix Ltd", item.brand);
        assertEquals("5000169105306", item.barcode);
        assertEquals(68.4, item.carbsPer100g, DELTA);
        assertEquals(37.5, item.portionGrams, DELTA);
        assertEquals(25.7, item.carbsForPortion, DELTA);
    }

    @Test
    public void saveMeal_nullBarcodeAndBrand_persisted() {
        MealSummary meal = buildMealWithItems();
        long id = repository.saveMeal(meal);

        MealSummary retrieved = repository.getMealById(id);
        MealItem milkItem = retrieved.items.get(1);

        assertNull("Null barcode should persist as null", milkItem.barcode);
        assertNull("Null brand should persist as null", milkItem.brand);
        assertEquals("Semi-skimmed Milk", milkItem.productName);
    }

    @Test
    public void saveMeal_giAndGlPersisted() {
        MealSummary meal = buildMealWithItems();
        meal.items.get(0).giEstimate = 70;
        meal.items.get(0).glEstimate = 18.0;
        long id = repository.saveMeal(meal);

        MealSummary retrieved = repository.getMealById(id);
        MealItem item = retrieved.items.get(0);

        assertEquals(70, item.giEstimate);
        assertEquals(18.0, item.glEstimate, DELTA);
    }

    @Test
    public void saveMeal_transactionRollsBackOnItemFailure() {
        MealSummary meal = buildMealWithItems();
        meal.items.get(1).productName = null;

        try {
            repository.saveMeal(meal);
            fail("Expected saveMeal to fail when an item has a null product name");
        } catch (Exception expected) {
            List<MealSummary> meals = repository.getAllMeals();
            assertTrue("No meal should be persisted after a failed transaction", meals.isEmpty());
        }
    }

    @Test
    public void saveMeal_nullItemsList_persistsHeaderOnly() {
        MealSummary meal = buildSimpleMeal();
        meal.items = null;

        long id = repository.saveMeal(meal);

        MealSummary retrieved = repository.getMealById(id);
        assertNotNull(retrieved);
        assertEquals("Test Meal", retrieved.name);
        assertEquals(0, retrieved.itemCount);
        assertTrue(retrieved.items.isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // getAllMeals
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void getAllMeals_emptyDb_returnsEmptyList() {
        List<MealSummary> meals = repository.getAllMeals();
        assertNotNull(meals);
        assertTrue(meals.isEmpty());
    }

    @Test
    public void getAllMeals_afterSaving_returnsAll() {
        repository.saveMeal(buildSimpleMeal());
        repository.saveMeal(buildSimpleMeal());
        repository.saveMeal(buildSimpleMeal());

        List<MealSummary> meals = repository.getAllMeals();
        assertEquals(3, meals.size());
    }

    @Test
    public void getAllMeals_orderedByMostRecentFirst() throws InterruptedException {
        MealSummary meal1 = buildSimpleMeal();
        meal1.name = "First";
        meal1.savedAt = 1000L;
        repository.saveMeal(meal1);

        MealSummary meal2 = buildSimpleMeal();
        meal2.name = "Second";
        meal2.savedAt = 2000L;
        repository.saveMeal(meal2);

        MealSummary meal3 = buildSimpleMeal();
        meal3.name = "Third";
        meal3.savedAt = 3000L;
        repository.saveMeal(meal3);

        List<MealSummary> meals = repository.getAllMeals();
        assertEquals("Third", meals.get(0).name);
        assertEquals("Second", meals.get(1).name);
        assertEquals("First", meals.get(2).name);
    }

    @Test
    public void getAllMeals_includesItemCount() {
        repository.saveMeal(buildMealWithItems()); // 2 items

        List<MealSummary> meals = repository.getAllMeals();
        assertEquals(2, meals.get(0).itemCount);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Item ordering
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void saveMeal_itemOrderPreserved() {
        MealSummary meal = new MealSummary("Order Test", 0);
        MealItem a = new MealItem();
        a.productName = "AAA";
        a.portionGrams = 100;
        a.carbsForPortion = 10;
        MealItem b = new MealItem();
        b.productName = "BBB";
        b.portionGrams = 100;
        b.carbsForPortion = 20;
        MealItem c = new MealItem();
        c.productName = "CCC";
        c.portionGrams = 100;
        c.carbsForPortion = 30;

        meal.items.add(a);
        meal.items.add(b);
        meal.items.add(c);
        meal.itemCount = 3;
        long id = repository.saveMeal(meal);

        MealSummary retrieved = repository.getMealById(id);
        assertEquals("AAA", retrieved.items.get(0).productName);
        assertEquals("BBB", retrieved.items.get(1).productName);
        assertEquals("CCC", retrieved.items.get(2).productName);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private MealSummary buildSimpleMeal() {
        return new MealSummary("Test Meal", 32.7);
    }

    private MealSummary buildMealWithItems() {
        MealSummary meal = new MealSummary("Breakfast", 32.7);

        MealItem weetabix = new MealItem();
        weetabix.productName = "Weetabix";
        weetabix.brand = "Weetabix Ltd";
        weetabix.barcode = "5000169105306";
        weetabix.carbsPer100g = 68.4;
        weetabix.portionGrams = 37.5;
        weetabix.carbsForPortion = 25.7;

        MealItem milk = new MealItem();
        milk.productName = "Semi-skimmed Milk";
        milk.brand = null;
        milk.barcode = null;
        milk.carbsPer100g = 4.7;
        milk.portionGrams = 150.0;
        milk.carbsForPortion = 7.1;

        meal.items.add(weetabix);
        meal.items.add(milk);
        meal.itemCount = 2;
        return meal;
    }
}
