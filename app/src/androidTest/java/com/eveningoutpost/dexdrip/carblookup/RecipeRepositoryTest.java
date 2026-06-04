package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeSummary;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test harness for Function 2: saveRecipe()
 *
 * HOW TO RUN:
 *   These are instrumented tests (they access SQLite on a real device/emulator).
 *   Place in: app/src/androidTest/java/com/eveningoutpost/dexdrip/carblookup/test/
 *   Run via Android Studio: right-click → Run 'RecipeRepositoryTest'
 *
 *   Each test uses a FRESH in-memory database — no data bleeds between tests.
 *   In-memory SQLite: `SQLiteDatabase.create(null)` — no file written to disk.
 */
@RunWith(JUnit4.class)
public class RecipeRepositoryTest {

    private static final double DELTA = 0.05;

    private RecipeRepository repository;
    private CarbLookupDatabase db;

    // ── Convenience builder for test recipes ─────────────────────────────────

    private Recipe buildWeetabixBreakfast() {
        Recipe recipe = new Recipe();
        recipe.name         = "Weetabix Breakfast";
        recipe.portionCount = 1;

        RecipeItem weetabix = new RecipeItem();
        weetabix.barcode       = "5000169105306";
        weetabix.productName   = "Weetabix";
        weetabix.brand         = "Weetabix";
        weetabix.carbsPer100g  = 68.4;
        weetabix.itemWeightGrams = 37.5; // 2 biscuits
        // itemCarbsGrams = 68.4 * 37.5 / 100 = 25.65 → 25.7 (rounded by repo)

        RecipeItem milk = new RecipeItem();
        milk.barcode       = null; // manually entered
        milk.productName   = "Semi-skimmed milk";
        milk.brand         = null;
        milk.carbsPer100g  = 4.7;
        milk.itemWeightGrams = 150.0;
        // itemCarbsGrams = 4.7 * 150 / 100 = 7.05 → 7.1

        recipe.items = new ArrayList<>();
        recipe.items.add(weetabix);
        recipe.items.add(milk);
        // Total: 25.7 + 7.1 = 32.8g (approx — exact rounding tested below)
        return recipe;
    }

    private Recipe buildPastaBolognese() {
        Recipe recipe = new Recipe();
        recipe.name         = "Pasta Bolognese";
        recipe.portionCount = 4;

        RecipeItem pasta = new RecipeItem();
        pasta.productName    = "Fusilli Pasta (dry)";
        pasta.carbsPer100g   = 72.0;
        pasta.itemWeightGrams = 320.0; // 80g per person × 4
        // = 72 * 320 / 100 = 230.4g carbs

        RecipeItem sauce = new RecipeItem();
        sauce.productName    = "Tomato Sauce";
        sauce.carbsPer100g   = 8.5;
        sauce.itemWeightGrams = 500.0;
        // = 8.5 * 500 / 100 = 42.5g carbs

        recipe.items = new ArrayList<>();
        recipe.items.add(pasta);
        recipe.items.add(sauce);
        // Total: 230.4 + 42.5 = 272.9g, perPortion = 272.9 / 4 = 68.2g (approx)
        return recipe;
    }

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        // Use an in-memory database for tests — no file written, auto-destroyed after test
        db         = CarbLookupDatabase.createInMemoryInstance(context);
        repository = new RecipeRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 1: Basic save and retrieve
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void saveRecipe_simpleRecipe_returnsPositiveId() {
        Recipe recipe = buildWeetabixBreakfast();
        long id = repository.saveRecipe(recipe);
        assertTrue("saveRecipe should return a positive ID", id > 0);
    }

    @Test
    public void saveRecipe_assignsIdToRecipeObject() {
        Recipe recipe = buildWeetabixBreakfast();
        long id = repository.saveRecipe(recipe);
        assertEquals("recipe.id should be updated after save", id, recipe.id);
    }

    @Test
    public void saveRecipe_retrievedRecipeMatchesInput() {
        Recipe original = buildWeetabixBreakfast();
        long id = repository.saveRecipe(original);

        Recipe retrieved = repository.getRecipeById(id);

        assertNotNull("Retrieved recipe should not be null", retrieved);
        assertEquals("Name should match",         original.name, retrieved.name);
        assertEquals("Portion count should match", original.portionCount, retrieved.portionCount);
        assertEquals("Item count should match",    original.items.size(), retrieved.items.size());
    }

    @Test
    public void saveRecipe_itemsPersistedWithCorrectValues() {
        Recipe original = buildWeetabixBreakfast();
        long id = repository.saveRecipe(original);

        Recipe retrieved = repository.getRecipeById(id);
        RecipeItem weetabixItem = retrieved.items.get(0);

        assertEquals("Product name", "Weetabix", weetabixItem.productName);
        assertEquals("Barcode",      "5000169105306", weetabixItem.barcode);
        assertEquals("carbsPer100g", 68.4, weetabixItem.carbsPer100g, DELTA);
        assertEquals("itemWeightGrams", 37.5, weetabixItem.itemWeightGrams, DELTA);
    }

    @Test
    public void saveRecipe_nullBarcodeItemPersistsCorrectly() {
        Recipe original = buildWeetabixBreakfast();
        long id = repository.saveRecipe(original);

        Recipe retrieved   = repository.getRecipeById(id);
        RecipeItem milkItem = retrieved.items.get(1);

        assertNull("Null barcode should be stored and retrieved as null", milkItem.barcode);
        assertEquals("Manually entered name should persist", "Semi-skimmed milk", milkItem.productName);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 2: Carb calculation on save
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void saveRecipe_totalCarbsCalculatedCorrectly() {
        // Weetabix: 68.4 * 37.5 / 100 = 25.65
        // Milk:     4.7  * 150  / 100 = 7.05
        // Total:    25.65 + 7.05 = 32.7 (sum unrounded, then round)
        Recipe original = buildWeetabixBreakfast();
        long id = repository.saveRecipe(original);
        Recipe retrieved = repository.getRecipeById(id);

        assertEquals("Total carbs should be 32.7g", 32.7, retrieved.totalCarbsGrams, DELTA);
    }

    @Test
    public void saveRecipe_carbsPerPortionCalculatedCorrectly() {
        Recipe pasta = buildPastaBolognese();
        long id = repository.saveRecipe(pasta);
        Recipe retrieved = repository.getRecipeById(id);

        // 272.9 / 4 = 68.225 → rounds to 68.2
        assertEquals("Carbs per portion should be ~68.2g", 68.2, retrieved.carbsPerPortion, 0.5);
    }

    @Test
    public void saveRecipe_itemCarbsCalculatedByRepository() {
        // The caller does NOT pre-calculate itemCarbsGrams — repo does it
        Recipe recipe = buildWeetabixBreakfast();
        recipe.items.get(0).itemCarbsGrams = 0; // deliberately wrong — repo should override
        long id = repository.saveRecipe(recipe);
        Recipe retrieved = repository.getRecipeById(id);

        // Repo should have calculated: 68.4 * 37.5 / 100 = 25.65
        assertTrue("Repo should calculate itemCarbsGrams, not use caller's value",
                retrieved.items.get(0).itemCarbsGrams >= 25.5);
    }

    @Test
    public void saveRecipe_singlePortionRecipe_carbsPerPortionEqualsTotalCarbs() {
        Recipe recipe = buildWeetabixBreakfast(); // portionCount=1
        long id = repository.saveRecipe(recipe);
        Recipe retrieved = repository.getRecipeById(id);

        assertEquals("For portionCount=1, carbsPerPortion should equal totalCarbs",
                retrieved.totalCarbsGrams, retrieved.carbsPerPortion, DELTA);
    }

    @Test
    public void updateRecipe_totalCarbsUsesSumThenRound() {
        Recipe recipe = buildWeetabixBreakfast();
        long id = repository.saveRecipe(recipe);

        Recipe updated = repository.getRecipeById(id);
        updated.items.get(0).itemWeightGrams = 37.5;
        updated.items.get(1).itemWeightGrams = 150.0;
        repository.updateRecipe(updated);

        Recipe retrieved = repository.getRecipeById(id);
        assertEquals("Updated recipe total carbs should stay sum-then-round", 32.7, retrieved.totalCarbsGrams, DELTA);
        assertEquals("Updated recipe carbs per portion should match total for one-portion recipe", 32.7, retrieved.carbsPerPortion, DELTA);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 3: Timestamps
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void saveRecipe_createdAtTimestampSet() {
        long beforeSave = System.currentTimeMillis();
        Recipe recipe   = buildWeetabixBreakfast();
        long id         = repository.saveRecipe(recipe);
        long afterSave  = System.currentTimeMillis();

        Recipe retrieved = repository.getRecipeById(id);
        assertTrue("createdAt should be set to approximately now",
                retrieved.createdAt >= beforeSave && retrieved.createdAt <= afterSave);
    }

    @Test
    public void saveRecipe_updatedAtTimestampSet() {
        Recipe recipe    = buildWeetabixBreakfast();
        long id          = repository.saveRecipe(recipe);
        Recipe retrieved = repository.getRecipeById(id);

        assertTrue("updatedAt should be > 0", retrieved.updatedAt > 0);
        assertEquals("updatedAt should equal createdAt on first save",
                retrieved.createdAt, retrieved.updatedAt, 100); // within 100ms
    }

    @Test
    public void updateRecipe_updatedAtChanges() throws InterruptedException {
        Recipe recipe = buildWeetabixBreakfast();
        long id       = repository.saveRecipe(recipe);

        Thread.sleep(10); // ensure time has passed

        Recipe updated = repository.getRecipeById(id);
        updated.name   = "Updated Weetabix Breakfast";
        repository.updateRecipe(updated);

        Recipe retrieved = repository.getRecipeById(id);
        assertTrue("updatedAt should increase after update",
                retrieved.updatedAt > retrieved.createdAt);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 4: Validation / rejection
    // ═════════════════════════════════════════════════════════════════════════

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_nullName_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.name = null;
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_blankName_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.name = "   ";
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_zeroPortion_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.portionCount = 0;
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_emptyItemsList_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.items = new ArrayList<>();
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_nullItemsList_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.items = null;
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_itemWithZeroWeight_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.items.get(0).itemWeightGrams = 0;
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_itemCarbsOver100_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.items.get(0).carbsPer100g = 101.0;
        repository.saveRecipe(recipe);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRecipe_itemNegativeCarbs_throwsException() {
        Recipe recipe = buildWeetabixBreakfast();
        recipe.items.get(0).carbsPer100g = -1.0;
        repository.saveRecipe(recipe);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 5: Transaction integrity
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void saveRecipe_transactionRollsBackOnItemFailure() {
        Recipe recipe = buildWeetabixBreakfast();
        // Corrupt the second item to cause a DB constraint failure
        recipe.items.get(1).productName = null; // should violate NOT NULL constraint

        long resultId = -1;
        try {
            resultId = repository.saveRecipe(recipe);
        } catch (Exception e) {
            // Expected — exception acceptable here
        }

        // Even if no exception (soft fail), the recipe should NOT be in the DB
        List<RecipeSummary> all = repository.getAllRecipes();
        assertTrue("No recipe should be saved after a partial failure", all.isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 6: List and delete
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void getAllRecipes_afterSavingTwo_returnsBoth() {
        repository.saveRecipe(buildWeetabixBreakfast());
        repository.saveRecipe(buildPastaBolognese());

        List<RecipeSummary> all = repository.getAllRecipes();
        assertEquals("Should return 2 recipes", 2, all.size());
    }

    @Test
    public void getAllRecipes_summaryContainsCorrectItemCount() {
        repository.saveRecipe(buildWeetabixBreakfast()); // 2 items

        List<RecipeSummary> all = repository.getAllRecipes();
        assertEquals("Item count in summary should be 2", 2, all.get(0).itemCount);
    }

    @Test
    public void deleteRecipe_removesRecipeAndItems() {
        long id = repository.saveRecipe(buildWeetabixBreakfast());
        repository.deleteRecipe(id);

        Recipe retrieved = repository.getRecipeById(id);
        assertNull("Deleted recipe should return null", retrieved);

        List<RecipeSummary> all = repository.getAllRecipes();
        assertTrue("Deleted recipe should not appear in list", all.isEmpty());
    }

    @Test
    public void deleteRecipe_cascadeDeletesItems() {
        long id = repository.saveRecipe(buildWeetabixBreakfast());
        repository.deleteRecipe(id);

        // Directly query orphaned items — there should be none
        int orphanCount = repository.getOrphanItemCount(); // helper method for testing
        assertEquals("No orphan items should remain after cascade delete", 0, orphanCount);
    }

    @Test
    public void duplicateRecipeName_allowedByRepository() {
        repository.saveRecipe(buildWeetabixBreakfast());
        long id2 = repository.saveRecipe(buildWeetabixBreakfast()); // same name

        assertTrue("Duplicate name should be allowed — returns valid ID", id2 > 0);
        assertEquals("Both recipes should exist", 2, repository.getAllRecipes().size());
    }
}