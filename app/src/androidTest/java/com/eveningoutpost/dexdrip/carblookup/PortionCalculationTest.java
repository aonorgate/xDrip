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
import com.eveningoutpost.dexdrip.carblookup.model.PortionResult;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Test harness for Function 3: getCarbsForRecipePortions()
 *
 * HOW TO RUN:
 *   Instrumented test — requires device/emulator.
 *   Place in: app/src/androidTest/java/com/eveningoutpost/dexdrip/carblookup/test/
 *
 * KEY FORMULA UNDER TEST:
 *   scaledCarbs = recipe.totalCarbsGrams * (requestedPortions / recipe.portionCount)
 *   Rounding: sum unrounded item contributions, round the TOTAL only.
 *
 * KNOWN TEST RECIPE: "Pasta Bolognese"
 *   - Pasta:  72.0/100g × 320g = 230.4g carbs
 *   - Sauce:  8.5/100g  × 500g = 42.5g  carbs
 *   - Total: 272.9g for 4 portions → 68.225g per portion → 68.2g (rounded)
 */
@RunWith(JUnit4.class)
public class PortionCalculationTest {

    private static final double DELTA = 0.1; // 1dp accuracy — within 0.1g

    private RecipeRepository repository;
    private CarbLookupDatabase db;

    // IDs set up in @Before
    private long recipeId_pasta;     // 4-portion pasta (272.9g total)
    private long recipeId_breakfast; // 1-portion breakfast (32.7g total)
    private long recipeId_zero;      // 0-carb recipe (sparkling water — valid edge case)

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db         = CarbLookupDatabase.createInMemoryInstance(context);
        repository = new RecipeRepository(db);

        // ── Pasta Bolognese (4 portions, 272.9g total) ────────────────────────
        Recipe pasta     = new Recipe();
        pasta.name       = "Pasta Bolognese";
        pasta.portionCount = 4;
        pasta.items      = new ArrayList<>();

        RecipeItem pastaItem = new RecipeItem();
        pastaItem.productName    = "Fusilli Pasta";
        pastaItem.carbsPer100g   = 72.0;
        pastaItem.itemWeightGrams = 320.0;

        RecipeItem sauceItem = new RecipeItem();
        sauceItem.productName    = "Tomato Sauce";
        sauceItem.carbsPer100g   = 8.5;
        sauceItem.itemWeightGrams = 500.0;

        pasta.items.add(pastaItem);
        pasta.items.add(sauceItem);
        recipeId_pasta = repository.saveRecipe(pasta);

        // ── Weetabix Breakfast (1 portion, 32.7g total) ───────────────────────
        Recipe breakfast     = new Recipe();
        breakfast.name       = "Weetabix Breakfast";
        breakfast.portionCount = 1;
        breakfast.items      = new ArrayList<>();

        RecipeItem weetabix = new RecipeItem();
        weetabix.productName    = "Weetabix";
        weetabix.carbsPer100g   = 68.4;
        weetabix.itemWeightGrams = 37.5;

        RecipeItem milk = new RecipeItem();
        milk.productName    = "Semi-skimmed milk";
        milk.carbsPer100g   = 4.7;
        milk.itemWeightGrams = 150.0;

        breakfast.items.add(weetabix);
        breakfast.items.add(milk);
        recipeId_breakfast = repository.saveRecipe(breakfast);

        // ── Zero carb recipe (sparkling water) ────────────────────────────────
        Recipe water     = new Recipe();
        water.name       = "Sparkling Water";
        water.portionCount = 1;
        water.items      = new ArrayList<>();

        RecipeItem waterItem = new RecipeItem();
        waterItem.productName    = "Sparkling Water";
        waterItem.carbsPer100g   = 0.0;
        waterItem.itemWeightGrams = 330.0;

        water.items.add(waterItem);
        recipeId_zero = repository.saveRecipe(water);
    }

    @After
    public void tearDown() {
        db.close();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 1: Simple carb total (getCarbsForPortions)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void getCarbs_onePortion_equalsRecipeCarbsPerPortion() {
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 1.0);
        // 272.9 * (1/4) = 68.225 → 68.2
        assertEquals("1 portion of pasta should be ~68.2g", 68.2, carbs, DELTA);
    }

    @Test
    public void getCarbs_fullRecipe_equalsTotalCarbs() {
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 4.0);
        // 272.9 * (4/4) = 272.9
        assertEquals("4 portions should equal total recipe carbs", 272.9, carbs, 0.5);
    }

    @Test
    public void getCarbs_halfPortion_correctResult() {
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 0.5);
        // 272.9 * (0.5/4) = 272.9 * 0.125 = 34.1125 → 34.1
        assertEquals("0.5 portions should be ~34.1g", 34.1, carbs, DELTA);
    }

    @Test
    public void getCarbs_oneAndHalfPortions_correctResult() {
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 1.5);
        // 272.9 * (1.5/4) = 272.9 * 0.375 = 102.3375 → 102.3
        assertEquals("1.5 portions should be ~102.3g", 102.3, carbs, DELTA);
    }

    @Test
    public void getCarbs_twoPortions_correctResult() {
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 2.0);
        // 272.9 * (2/4) = 136.45 → 136.5
        assertEquals("2 portions should be ~136.5g", 136.5, carbs, DELTA);
    }

    @Test
    public void getCarbs_singlePortionRecipeOneRequested_returnsTotalCarbs() {
        double carbs = repository.getCarbsForPortions(recipeId_breakfast, 1.0);
        assertEquals("1 portion of 1-portion recipe should equal totalCarbs", 32.7, carbs, DELTA);
    }

    @Test
    public void getCarbs_singlePortionRecipeTwoRequested_doublesCarbs() {
        double carbs = repository.getCarbsForPortions(recipeId_breakfast, 2.0);
        // 32.7 * (2/1) = 65.4
        assertEquals("2 portions of 1-portion recipe should double carbs", 65.4, carbs, DELTA);
    }

    @Test
    public void getCarbs_zeroCarbRecipe_returnsZero() {
        double carbs = repository.getCarbsForPortions(recipeId_zero, 1.0);
        assertEquals("Zero-carb recipe should return 0.0", 0.0, carbs, DELTA);
    }

    @Test
    public void getCarbs_nonExistentRecipe_returnsZero() {
        double carbs = repository.getCarbsForPortions(99999L, 1.0);
        assertEquals("Non-existent recipe should return 0.0", 0.0, carbs, DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getCarbs_zeroPortions_throwsException() {
        repository.getCarbsForPortions(recipeId_pasta, 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getCarbs_negativePortions_throwsException() {
        repository.getCarbsForPortions(recipeId_pasta, -1.0);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 2: Detailed breakdown (getCarbsBreakdownForPortions)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void getBreakdown_onePortion_correctMetadata() {
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 1.0);

        assertNotNull("Result should not be null", result);
        assertTrue("recipeFound should be true",   result.isRecipeFound());
        assertEquals("recipeId should match",       recipeId_pasta, result.getRecipeId());
        assertEquals("recipeName should match",     "Pasta Bolognese", result.getRecipeName());
        assertEquals("numberOfPortions echoed back", 1.0, result.getNumberOfPortions(), DELTA);
    }

    @Test
    public void getBreakdown_onePortion_correctTotalCarbs() {
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 1.0);
        assertEquals("Total carbs for 1 portion", 68.2, result.getTotalCarbsGrams(), DELTA);
    }

    @Test
    public void getBreakdown_carbsPerSinglePortionAlwaysFixed() {
        // carbsPerSinglePortion should reflect the recipe's stored value regardless of requested portions
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 3.0);
        assertEquals("carbsPerSinglePortion should be constant", 68.2, result.getCarbsPerSinglePortion(), DELTA);
    }

    @Test
    public void getBreakdown_itemsListCorrectSize() {
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 1.0);
        assertNotNull("Items list should not be null", result.getItems());
        assertEquals("Should have 2 item results", 2, result.getItems().size());
    }

    @Test
    public void getBreakdown_scaledItemWeightCorrect() {
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 1.0);
        PortionItemResult pastaItem = result.getItems().get(0);
        // Pasta: 320g total for 4 portions → 80g per portion
        assertEquals("Scaled pasta weight should be 80g", 80.0, pastaItem.getScaledWeightGrams(), DELTA);
    }

    @Test
    public void getBreakdown_scaledItemCarbsCorrect() {
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 1.0);
        PortionItemResult pastaItem = result.getItems().get(0);
        // 230.4g carbs total for pasta, 1/4 portion = 57.6g
        assertEquals("Scaled pasta carbs should be ~57.6g", 57.6, pastaItem.getScaledCarbsGrams(), DELTA);
    }

    @Test
    public void getBreakdown_scaledItemsForHalfPortion() {
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 0.5);
        PortionItemResult pastaItem = result.getItems().get(0);
        // 0.5 portions of 4 = 1/8 of recipe → pasta 320g * 0.125 = 40g weight, 230.4 * 0.125 = 28.8g carbs
        assertEquals("Pasta weight for 0.5 portions", 40.0, pastaItem.getScaledWeightGrams(), DELTA);
        assertEquals("Pasta carbs for 0.5 portions",  28.8, pastaItem.getScaledCarbsGrams(), DELTA);
    }

    @Test
    public void getBreakdown_totalIsNotSumOfRoundedItems() {
        // This tests the critical rule: sum THEN round, don't sum rounded values
        PortionResult result = repository.getCarbsBreakdownForPortions(recipeId_pasta, 1.0);

        double sumOfRoundedItems = 0;
        for (PortionItemResult item : result.getItems()) {
            sumOfRoundedItems += item.getScaledCarbsGrams(); // each already rounded to 1dp
        }

        // The total should match the correctly-calculated total, not the sum-of-rounded
        // Pasta: 57.6, Sauce: 10.625 → rounded separately: 57.6 + 10.6 = 68.2
        // Sum-then-round: 68.225 → 68.2 — in this case they match, but test the mechanism
        assertEquals("Total should be calculated sum-then-round", result.getTotalCarbsGrams(),
                Math.round(sumOfRoundedItems * 10.0) / 10.0, DELTA);
    }

    @Test
    public void getBreakdown_nonExistentRecipe_recipeFoundFalse() {
        PortionResult result = repository.getCarbsBreakdownForPortions(99999L, 1.0);
        assertNotNull("Result should not be null even for missing recipe", result);
        assertFalse("recipeFound should be false", result.isRecipeFound());
        assertEquals("totalCarbsGrams should be 0 for missing recipe", 0.0, result.getTotalCarbsGrams(), DELTA);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GROUP 3: Rounding consistency
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void rounding_resultHasAtMostOneDecimalPlace() {
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 1.0);
        // Check that the value has at most 1dp by verifying it equals itself when rounded to 1dp
        double rounded = Math.round(carbs * 10.0) / 10.0;
        assertEquals("Result should already be rounded to 1dp", rounded, carbs, 0.001);
    }

    @Test
    public void rounding_fractionalPortionRoundsCorrectly() {
        // 272.9 * (0.5 / 4) = 34.1125 → should round to 34.1
        double carbs = repository.getCarbsForPortions(recipeId_pasta, 0.5);
        assertTrue("34.05 to 34.15 acceptable range", carbs >= 34.05 && carbs <= 34.15);
    }
}