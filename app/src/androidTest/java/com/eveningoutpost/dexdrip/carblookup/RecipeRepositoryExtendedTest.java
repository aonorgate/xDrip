package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;
import com.eveningoutpost.dexdrip.carblookup.model.PortionResult;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeSummary;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Extended instrumented tests for RecipeRepository.
 * Covers: search, meal-time filter, sort, duplicate, notes, GL calculation,
 * use-count tracking, and update flows.
 */
@RunWith(JUnit4.class)
public class RecipeRepositoryExtendedTest {

    private static final double DELTA = 0.1;

    private RecipeRepository repo;
    private CarbLookupDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = CarbLookupDatabase.createInMemoryInstance(context);
        repo = new RecipeRepository(db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Meal-time tagging
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void saveRecipe_mealTimePersisted() {
        Recipe recipe = buildRecipe("Porridge", 1, "breakfast");
        long id = repo.saveRecipe(recipe);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("breakfast", retrieved.mealTime);
    }

    @Test
    public void saveRecipe_dinnerTag_persisted() {
        Recipe recipe = buildRecipe("Pasta", 4, "dinner");
        long id = repo.saveRecipe(recipe);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("dinner", retrieved.mealTime);
    }

    @Test
    public void saveRecipe_nullMealTime_defaultsToAny() {
        Recipe recipe = buildRecipe("Snack Mix", 1, null);
        long id = repo.saveRecipe(recipe);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("any", retrieved.mealTime);
    }

    @Test
    public void filterByMealTime_breakfastOnly() {
        repo.saveRecipe(buildRecipe("Porridge", 1, "breakfast"));
        repo.saveRecipe(buildRecipe("Pasta", 4, "dinner"));
        repo.saveRecipe(buildRecipe("Toast", 1, "breakfast"));

        List<RecipeSummary> results = repo.getAllRecipes("name", "breakfast");
        assertEquals(2, results.size());
        for (RecipeSummary r : results) {
            assertTrue(r.mealTime.equals("breakfast") || r.mealTime.equals("any"));
        }
    }

    @Test
    public void filterByMealTime_dinnerOnly() {
        repo.saveRecipe(buildRecipe("Porridge", 1, "breakfast"));
        repo.saveRecipe(buildRecipe("Pasta", 4, "dinner"));
        repo.saveRecipe(buildRecipe("Risotto", 2, "dinner"));

        List<RecipeSummary> results = repo.getAllRecipes("name", "dinner");
        assertEquals(2, results.size());
    }

    @Test
    public void filterByMealTime_anyReturnsAll() {
        repo.saveRecipe(buildRecipe("A", 1, "breakfast"));
        repo.saveRecipe(buildRecipe("B", 4, "dinner"));
        repo.saveRecipe(buildRecipe("C", 1, "snack"));

        List<RecipeSummary> results = repo.getAllRecipes("name", "any");
        assertEquals(3, results.size());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Search
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void searchRecipes_byName_findsMatch() {
        repo.saveRecipe(buildRecipe("Porridge", 1, "breakfast"));
        repo.saveRecipe(buildRecipe("Pasta Bolognese", 4, "dinner"));
        repo.saveRecipe(buildRecipe("Pasta Carbonara", 2, "dinner"));

        List<RecipeSummary> results = repo.searchRecipes("Pasta");
        assertEquals(2, results.size());
    }

    @Test
    public void searchRecipes_caseInsensitive() {
        repo.saveRecipe(buildRecipe("Weetabix Breakfast", 1, "breakfast"));

        List<RecipeSummary> results = repo.searchRecipes("weetabix");
        assertEquals(1, results.size());
        assertEquals("Weetabix Breakfast", results.get(0).name);
    }

    @Test
    public void searchRecipes_noMatch_returnsEmpty() {
        repo.saveRecipe(buildRecipe("Porridge", 1, "breakfast"));

        List<RecipeSummary> results = repo.searchRecipes("pizza");
        assertTrue(results.isEmpty());
    }

    @Test
    public void searchRecipes_byNotes_findsMatch() {
        Recipe recipe = buildRecipe("My Breakfast", 1, "breakfast");
        recipe.notes = "Quick weekday option with fruit";
        repo.saveRecipe(recipe);

        List<RecipeSummary> results = repo.searchRecipes("weekday");
        assertEquals(1, results.size());
    }

    @Test
    public void searchRecipes_byIngredientName_findsRecipe() {
        Recipe recipe = buildRecipe("Lunch Bowl", 1, "lunch");
        addItem(recipe, "Blueberries", 14.5, 80.0, 53);
        repo.saveRecipe(recipe);
        repo.saveRecipe(buildRecipe("Plain Toast", 1, "breakfast"));

        List<RecipeSummary> results = repo.searchRecipes("Blueberries");
        assertEquals(1, results.size());
        assertEquals("Lunch Bowl", results.get(0).name);
        assertEquals(3, results.get(0).itemCount);
    }

    @Test
    public void searchRecipes_byIngredientBrandOrBarcode_findsRecipe() {
        Recipe recipe = buildRecipe("Breakfast Pot", 1, "breakfast");
        recipe.items.get(0).brand = "Morning Foods";
        recipe.items.get(0).barcode = "1234567890";
        repo.saveRecipe(recipe);

        assertEquals(1, repo.searchRecipes("Morning Foods").size());
        assertEquals(1, repo.searchRecipes("345678").size());
    }

    @Test
    public void searchRecipes_withMealTimeFilter() {
        repo.saveRecipe(buildRecipe("Pasta Lunch", 2, "lunch"));
        repo.saveRecipe(buildRecipe("Pasta Dinner", 4, "dinner"));

        List<RecipeSummary> results = repo.searchRecipes("Pasta", "name", "lunch");
        assertEquals(1, results.size());
        assertEquals("Pasta Lunch", results.get(0).name);
    }

    @Test
    public void searchRecipes_escapesSpecialSqlChars() {
        Recipe recipe = buildRecipe("100% Wheat Toast", 1, "breakfast");
        repo.saveRecipe(recipe);

        // Percent and underscore in search shouldn't be interpreted as SQL wildcards
        List<RecipeSummary> results = repo.searchRecipes("100%");
        assertEquals(1, results.size());
    }

    @Test
    public void searchRecipes_literalUnderscore_matchesLiteralUnderscore() {
        repo.saveRecipe(buildRecipe("Low_Carb Bowl", 1, "lunch"));
        repo.saveRecipe(buildRecipe("LowXCarb Bowl", 1, "lunch"));

        List<RecipeSummary> results = repo.searchRecipes("Low_Carb");
        assertEquals(1, results.size());
        assertEquals("Low_Carb Bowl", results.get(0).name);
    }

    @Test
    public void searchRecipes_invalidSortKey_fallsBackToNameOrdering() {
        repo.saveRecipe(buildRecipe("Zebra Toast", 1, "breakfast"));
        repo.saveRecipe(buildRecipe("Apple Toast", 1, "breakfast"));
        repo.saveRecipe(buildRecipe("Mango Toast", 1, "breakfast"));

        List<RecipeSummary> results = repo.searchRecipes("Toast", "not_a_real_sort", "any");
        assertEquals(3, results.size());
        assertEquals("Apple Toast", results.get(0).name);
        assertEquals("Mango Toast", results.get(1).name);
        assertEquals("Zebra Toast", results.get(2).name);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Sort order
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void sort_byName_alphabetical() {
        repo.saveRecipe(buildRecipe("Zebra Cake", 1, "snack"));
        repo.saveRecipe(buildRecipe("Apple Crumble", 2, "dinner"));
        repo.saveRecipe(buildRecipe("Muesli", 1, "breakfast"));

        List<RecipeSummary> results = repo.getAllRecipes("name", "any");
        assertEquals("Apple Crumble", results.get(0).name);
        assertEquals("Muesli", results.get(1).name);
        assertEquals("Zebra Cake", results.get(2).name);
    }

    @Test
    public void sort_byMostUsed() {
        long id1 = repo.saveRecipe(buildRecipe("Rarely Used", 1, "any"));
        long id2 = repo.saveRecipe(buildRecipe("Often Used", 1, "any"));
        long id3 = repo.saveRecipe(buildRecipe("Sometimes Used", 1, "any"));

        // Simulate usage
        repo.incrementUseCount(id2);
        repo.incrementUseCount(id2);
        repo.incrementUseCount(id2);
        repo.incrementUseCount(id3);

        List<RecipeSummary> results = repo.getAllRecipes("most_used", "any");
        assertEquals("Often Used", results.get(0).name);
        assertEquals("Sometimes Used", results.get(1).name);
        assertEquals("Rarely Used", results.get(2).name);
    }

    @Test
    public void sort_byRecent() {
        long id1 = repo.saveRecipe(buildRecipe("Old", 1, "any"));
        long id2 = repo.saveRecipe(buildRecipe("Recent", 1, "any"));

        repo.incrementUseCount(id1); // sets last_used_at for id1
        // Small delay to ensure different timestamp
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        repo.incrementUseCount(id2); // sets last_used_at for id2 (more recent)

        List<RecipeSummary> results = repo.getAllRecipes("recent", "any");
        assertEquals("Recent", results.get(0).name);
    }

    @Test
    public void getAllRecipes_invalidSortKey_fallsBackToNameOrdering() {
        repo.saveRecipe(buildRecipe("Zebra Cake", 1, "snack"));
        repo.saveRecipe(buildRecipe("Apple Crumble", 2, "dinner"));
        repo.saveRecipe(buildRecipe("Muesli", 1, "breakfast"));

        List<RecipeSummary> results = repo.getAllRecipes("bad_sort_key", "any");
        assertEquals(3, results.size());
        assertEquals("Apple Crumble", results.get(0).name);
        assertEquals("Muesli", results.get(1).name);
        assertEquals("Zebra Cake", results.get(2).name);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Duplicate
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void duplicateRecipe_createsNewRecipe() {
        long originalId = repo.saveRecipe(buildRecipe("Original", 2, "lunch"));

        long dupeId = repo.duplicateRecipe(originalId, "Copy of Original");

        assertTrue(dupeId > 0);
        assertNotEquals(originalId, dupeId);
    }

    @Test
    public void duplicateRecipe_hasNewName() {
        long originalId = repo.saveRecipe(buildRecipe("Original", 2, "lunch"));

        long dupeId = repo.duplicateRecipe(originalId, "My Copy");

        Recipe dupe = repo.getRecipeById(dupeId);
        assertEquals("My Copy", dupe.name);
    }

    @Test
    public void duplicateRecipe_copiesAllItems() {
        Recipe original = buildRecipe("Rich Recipe", 3, "dinner");
        addItem(original, "Pasta", 72.0, 300.0, 55);
        addItem(original, "Sauce", 8.0, 400.0, 0);
        addItem(original, "Cheese", 1.5, 50.0, 0);
        long originalId = repo.saveRecipe(original);

        long dupeId = repo.duplicateRecipe(originalId, "Copy");

        Recipe dupe = repo.getRecipeById(dupeId);
        assertEquals(5, dupe.items.size());
        assertEquals("Weetabix", dupe.items.get(0).productName);
        assertEquals("Milk", dupe.items.get(1).productName);
        assertEquals("Pasta", dupe.items.get(2).productName);
        assertEquals("Sauce", dupe.items.get(3).productName);
        assertEquals("Cheese", dupe.items.get(4).productName);
    }

    @Test
    public void duplicateRecipe_resetsUseCount() {
        long originalId = repo.saveRecipe(buildRecipe("Frequent", 1, "any"));
        repo.incrementUseCount(originalId);
        repo.incrementUseCount(originalId);

        long dupeId = repo.duplicateRecipe(originalId, "Fresh Copy");

        Recipe dupe = repo.getRecipeById(dupeId);
        assertEquals(0, dupe.useCount);
    }

    @Test
    public void duplicateRecipe_preservesMealTime() {
        long originalId = repo.saveRecipe(buildRecipe("Brekkie", 1, "breakfast"));

        long dupeId = repo.duplicateRecipe(originalId, "Brekkie Copy");

        Recipe dupe = repo.getRecipeById(dupeId);
        assertEquals("breakfast", dupe.mealTime);
    }

    @Test
    public void duplicateRecipe_preservesNotes() {
        Recipe original = buildRecipe("Noted", 1, "snack");
        original.notes = "Add extra cinnamon";
        long originalId = repo.saveRecipe(original);

        long dupeId = repo.duplicateRecipe(originalId, "Noted Copy");

        Recipe dupe = repo.getRecipeById(dupeId);
        assertEquals("Add extra cinnamon", dupe.notes);
    }

    @Test
    public void duplicateRecipe_nonExistentSource_returnsNegative() {
        long result = repo.duplicateRecipe(99999L, "Ghost Copy");
        assertEquals(-1, result);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Notes
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void saveRecipe_notesPersisted() {
        Recipe recipe = buildRecipe("Noted Recipe", 2, "lunch");
        recipe.notes = "Best served warm. Use fresh basil.";
        long id = repo.saveRecipe(recipe);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("Best served warm. Use fresh basil.", retrieved.notes);
    }

    @Test
    public void saveRecipe_nullNotes_persistedAsNull() {
        Recipe recipe = buildRecipe("No Notes", 1, "any");
        recipe.notes = null;
        long id = repo.saveRecipe(recipe);

        Recipe retrieved = repo.getRecipeById(id);
        assertNull(retrieved.notes);
    }

    @Test
    public void updateRecipe_notesCanBeAdded() {
        Recipe recipe = buildRecipe("Plain", 1, "any");
        long id = repo.saveRecipe(recipe);

        Recipe toUpdate = repo.getRecipeById(id);
        toUpdate.notes = "Added later";
        repo.updateRecipe(toUpdate);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("Added later", retrieved.notes);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Use count and last_used_at
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void incrementUseCount_incrementsBy1() {
        long id = repo.saveRecipe(buildRecipe("Counter", 1, "any"));

        repo.incrementUseCount(id);
        repo.incrementUseCount(id);
        repo.incrementUseCount(id);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals(3, retrieved.useCount);
    }

    @Test
    public void incrementUseCount_updatesLastUsedAt() {
        long id = repo.saveRecipe(buildRecipe("Timer", 1, "any"));

        long before = System.currentTimeMillis();
        repo.incrementUseCount(id);
        long after = System.currentTimeMillis();

        Recipe retrieved = repo.getRecipeById(id);
        assertTrue(retrieved.lastUsedAt >= before && retrieved.lastUsedAt <= after);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GL calculation
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void glCalculation_allItemsHaveGi_totalGlCorrect() {
        Recipe recipe = new Recipe();
        recipe.name = "GL Test";
        recipe.portionCount = 1;
        recipe.items = new ArrayList<>();

        // Item 1: 50g carbs, GI 60 → GL = 60 * 50 / 100 = 30
        RecipeItem item1 = new RecipeItem();
        item1.productName = "Bread";
        item1.carbsPer100g = 50.0;
        item1.itemWeightGrams = 100.0;
        item1.giEstimate = 60;
        recipe.items.add(item1);

        // Item 2: 20g carbs, GI 40 → GL = 40 * 20 / 100 = 8
        RecipeItem item2 = new RecipeItem();
        item2.productName = "Apple";
        item2.carbsPer100g = 10.0;
        item2.itemWeightGrams = 200.0;
        item2.giEstimate = 40;
        recipe.items.add(item2);

        long id = repo.saveRecipe(recipe);

        PortionResult result = repo.getCarbsBreakdownForPortions(id, 1.0);
        // Total GL = 30 + 8 = 38
        assertEquals(38.0, result.getTotalGlEstimate(), DELTA);
        assertFalse("All items have GI, so GL should not be partial", result.isGlPartial());
    }

    @Test
    public void glCalculation_someItemsMissingGi_partialFlag() {
        Recipe recipe = new Recipe();
        recipe.name = "Partial GL";
        recipe.portionCount = 1;
        recipe.items = new ArrayList<>();

        RecipeItem item1 = new RecipeItem();
        item1.productName = "Bread";
        item1.carbsPer100g = 50.0;
        item1.itemWeightGrams = 100.0;
        item1.giEstimate = 70;
        recipe.items.add(item1);

        RecipeItem item2 = new RecipeItem();
        item2.productName = "Unknown";
        item2.carbsPer100g = 30.0;
        item2.itemWeightGrams = 100.0;
        item2.giEstimate = 0; // unknown
        recipe.items.add(item2);

        long id = repo.saveRecipe(recipe);

        PortionResult result = repo.getCarbsBreakdownForPortions(id, 1.0);
        assertTrue("Should be marked as partial GL", result.isGlPartial());
        // GL from first item only: 70 * 50 / 100 = 35
        assertEquals(35.0, result.getTotalGlEstimate(), DELTA);
    }

    @Test
    public void glCalculation_noItemsHaveGi_zeroGl() {
        Recipe recipe = new Recipe();
        recipe.name = "No GI";
        recipe.portionCount = 1;
        recipe.items = new ArrayList<>();

        RecipeItem item = new RecipeItem();
        item.productName = "Mystery Food";
        item.carbsPer100g = 40.0;
        item.itemWeightGrams = 200.0;
        item.giEstimate = 0;
        recipe.items.add(item);

        long id = repo.saveRecipe(recipe);

        PortionResult result = repo.getCarbsBreakdownForPortions(id, 1.0);
        assertEquals(0.0, result.getTotalGlEstimate(), DELTA);
        assertTrue(result.isGlPartial());
    }

    @Test
    public void glCalculation_scalesWithPortions() {
        Recipe recipe = new Recipe();
        recipe.name = "Scale GL";
        recipe.portionCount = 2;
        recipe.items = new ArrayList<>();

        // Total: 60g carbs, GI 50 → GL for full recipe = 50 * 60 / 100 = 30
        RecipeItem item = new RecipeItem();
        item.productName = "Oats";
        item.carbsPer100g = 60.0;
        item.itemWeightGrams = 100.0;
        item.giEstimate = 50;
        recipe.items.add(item);

        long id = repo.saveRecipe(recipe);

        // 1 portion = half the recipe: 30g carbs, GL = 50 * 30 / 100 = 15
        PortionResult result1 = repo.getCarbsBreakdownForPortions(id, 1.0);
        assertEquals(15.0, result1.getTotalGlEstimate(), DELTA);

        // 2 portions = full recipe: GL = 30
        PortionResult result2 = repo.getCarbsBreakdownForPortions(id, 2.0);
        assertEquals(30.0, result2.getTotalGlEstimate(), DELTA);
    }

    @Test
    public void glCalculation_perItemGlInBreakdown() {
        Recipe recipe = new Recipe();
        recipe.name = "Item GL";
        recipe.portionCount = 1;
        recipe.items = new ArrayList<>();

        RecipeItem item = new RecipeItem();
        item.productName = "Rice";
        item.carbsPer100g = 28.0;
        item.itemWeightGrams = 200.0; // 56g carbs
        item.giEstimate = 73;
        recipe.items.add(item);

        long id = repo.saveRecipe(recipe);

        PortionResult result = repo.getCarbsBreakdownForPortions(id, 1.0);
        PortionItemResult itemResult = result.getItems().get(0);

        assertEquals(73, itemResult.getGiEstimate());
        // GL = 73 * 56 / 100 = 40.88 → 40.9
        assertEquals(40.9, itemResult.getScaledGlEstimate(), DELTA);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update recipe
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void updateRecipe_nameChangePersists() {
        long id = repo.saveRecipe(buildRecipe("Old Name", 2, "lunch"));

        Recipe toUpdate = repo.getRecipeById(id);
        toUpdate.name = "New Name";
        repo.updateRecipe(toUpdate);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("New Name", retrieved.name);
    }

    @Test
    public void updateRecipe_addingItems_recalculatesCarbs() {
        Recipe recipe = buildRecipe("Grow", 1, "any");
        long id = repo.saveRecipe(recipe);

        Recipe toUpdate = repo.getRecipeById(id);
        RecipeItem extraItem = new RecipeItem();
        extraItem.productName = "Extra";
        extraItem.carbsPer100g = 50.0;
        extraItem.itemWeightGrams = 100.0;
        toUpdate.items.add(extraItem);
        repo.updateRecipe(toUpdate);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals(3, retrieved.items.size());
        // Original: 68.4*37.5/100 + 4.7*150/100 = 25.65 + 7.05 = 32.7
        // Plus new: 50*100/100 = 50
        // Total: 82.7
        assertEquals(82.7, retrieved.totalCarbsGrams, DELTA);
    }

    @Test
    public void updateRecipe_removingItems_recalculatesCarbs() {
        Recipe recipe = buildRecipe("Shrink", 1, "any");
        long id = repo.saveRecipe(recipe);

        Recipe toUpdate = repo.getRecipeById(id);
        toUpdate.items.remove(1); // remove milk
        repo.updateRecipe(toUpdate);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals(1, retrieved.items.size());
        // Only Weetabix: 68.4*37.5/100 = 25.65 → 25.7
        assertEquals(25.7, retrieved.totalCarbsGrams, DELTA);
    }

    @Test
    public void updateRecipe_changingPortionCount_recalculatesCarbsPerPortion() {
        Recipe recipe = buildRecipe("Resize", 1, "any");
        long id = repo.saveRecipe(recipe);

        Recipe toUpdate = repo.getRecipeById(id);
        toUpdate.portionCount = 2;
        repo.updateRecipe(toUpdate);

        Recipe retrieved = repo.getRecipeById(id);
        // Total carbs ~32.7, 2 portions → 16.4 per portion
        assertEquals(16.4, retrieved.carbsPerPortion, DELTA);
    }

    @Test
    public void updateRecipe_mealTimeChange() {
        long id = repo.saveRecipe(buildRecipe("Flexible", 1, "breakfast"));

        Recipe toUpdate = repo.getRecipeById(id);
        toUpdate.mealTime = "snack";
        repo.updateRecipe(toUpdate);

        Recipe retrieved = repo.getRecipeById(id);
        assertEquals("snack", retrieved.mealTime);
    }

    @Test
    public void updateRecipe_itemInsertFailure_rollsBackOriginalRecipeState() {
        long id = repo.saveRecipe(buildRecipe("Rollback Candidate", 1, "lunch"));

        Recipe original = repo.getRecipeById(id);
        assertNotNull(original);
        assertEquals("Rollback Candidate", original.name);
        assertEquals(2, original.items.size());

        Recipe brokenUpdate = repo.getRecipeById(id);
        brokenUpdate.name = "Broken Update";
        brokenUpdate.items.get(1).productName = null;

        try {
            repo.updateRecipe(brokenUpdate);
            fail("Expected updateRecipe to fail when an item has a null product name");
        } catch (Exception expected) {
            Recipe afterFailure = repo.getRecipeById(id);
            assertNotNull(afterFailure);
            assertEquals("Rollback Candidate", afterFailure.name);
            assertEquals(2, afterFailure.items.size());
            assertEquals("Weetabix", afterFailure.items.get(0).productName);
            assertEquals("Milk", afterFailure.items.get(1).productName);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Delete
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void deleteRecipe_removesFromList() {
        long id1 = repo.saveRecipe(buildRecipe("Keep", 1, "any"));
        long id2 = repo.saveRecipe(buildRecipe("Delete", 1, "any"));

        repo.deleteRecipe(id2);

        List<RecipeSummary> all = repo.getAllRecipes();
        assertEquals(1, all.size());
        assertEquals("Keep", all.get(0).name);
    }

    @Test
    public void deleteRecipe_cleansUpOrphanItems() {
        long id = repo.saveRecipe(buildRecipe("Doomed", 1, "any"));
        repo.deleteRecipe(id);

        assertEquals(0, repo.getOrphanItemCount());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private Recipe buildRecipe(String name, int portions, String mealTime) {
        Recipe recipe = new Recipe();
        recipe.name = name;
        recipe.portionCount = portions;
        recipe.mealTime = mealTime;
        recipe.items = new ArrayList<>();

        RecipeItem item1 = new RecipeItem();
        item1.productName = "Weetabix";
        item1.carbsPer100g = 68.4;
        item1.itemWeightGrams = 37.5;
        item1.giEstimate = 70;
        recipe.items.add(item1);

        RecipeItem item2 = new RecipeItem();
        item2.productName = "Milk";
        item2.carbsPer100g = 4.7;
        item2.itemWeightGrams = 150.0;
        item2.giEstimate = 31;
        recipe.items.add(item2);

        return recipe;
    }

    private void addItem(Recipe recipe, String name, double carbsPer100g,
                         double weightGrams, int gi) {
        RecipeItem item = new RecipeItem();
        item.productName = name;
        item.carbsPer100g = carbsPer100g;
        item.itemWeightGrams = weightGrams;
        item.giEstimate = gi;
        recipe.items.add(item);
    }
}
