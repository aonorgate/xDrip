package com.eveningoutpost.dexdrip.carblookup.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure JVM unit tests for GiLookupTable.
 * Tests keyword matching, specificity ordering, and edge cases.
 */
public class GiLookupTableTest {

    // ═══════════════════════════════════════════════════════════════════════
    // Known product matches
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void lookup_whiteRice_returns73() {
        assertEquals(73, GiLookupTable.lookupGi("White Rice"));
    }

    @Test
    public void lookup_brownRice_returns55() {
        assertEquals(55, GiLookupTable.lookupGi("Brown Rice Organic"));
    }

    @Test
    public void lookup_pasta_returns49() {
        assertEquals(49, GiLookupTable.lookupGi("Fusilli Pasta (dry)"));
    }

    @Test
    public void lookup_potato_returns78() {
        assertEquals(78, GiLookupTable.lookupGi("Maris Piper Potatoes"));
    }

    @Test
    public void lookup_sweetPotato_returns63() {
        // More-specific "sweet potato" should win over generic "potato"
        assertEquals(63, GiLookupTable.lookupGi("Sweet Potato Wedges"));
    }

    @Test
    public void lookup_whiteBread_returns75() {
        assertEquals(75, GiLookupTable.lookupGi("Kingsmill White Bread"));
    }

    @Test
    public void lookup_genericBread_returns70() {
        // No "white" qualifier, falls through to generic "bread"
        assertEquals(70, GiLookupTable.lookupGi("Seeded Bread Rolls"));
    }

    @Test
    public void lookup_orangeJuice_returns57() {
        // "orange juice" should match before generic "orange" (43)
        assertEquals(57, GiLookupTable.lookupGi("Tropicana Orange Juice"));
    }

    @Test
    public void lookup_orange_returns43() {
        assertEquals(43, GiLookupTable.lookupGi("Navel Oranges"));
    }

    @Test
    public void lookup_milk_returns31() {
        assertEquals(31, GiLookupTable.lookupGi("Semi-skimmed milk"));
    }

    @Test
    public void lookup_oats_returns55() {
        assertEquals(55, GiLookupTable.lookupGi("Oat So Simple"));
    }

    @Test
    public void lookup_lentil_returns29() {
        assertEquals(29, GiLookupTable.lookupGi("Red Lentils"));
    }

    @Test
    public void lookup_chickpea_returns28() {
        assertEquals(28, GiLookupTable.lookupGi("Tinned Chickpeas"));
    }

    @Test
    public void lookup_banana_returns51() {
        assertEquals(51, GiLookupTable.lookupGi("Banana"));
    }

    @Test
    public void lookup_cornflakes_returns81() {
        assertEquals(81, GiLookupTable.lookupGi("Kellogg's Corn Flakes"));
    }

    @Test
    public void lookup_basmati_returns57() {
        assertEquals(57, GiLookupTable.lookupGi("Tilda Basmati Rice"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Case insensitivity
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void lookup_caseInsensitive_uppercase() {
        assertEquals(49, GiLookupTable.lookupGi("PASTA SHELLS"));
    }

    @Test
    public void lookup_caseInsensitive_mixedCase() {
        assertEquals(78, GiLookupTable.lookupGi("Baked POTATO"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // No match → returns 0
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void lookup_unknownProduct_returnsZero() {
        assertEquals(0, GiLookupTable.lookupGi("Chicken Breast"));
    }

    @Test
    public void lookup_emptyString_returnsZero() {
        assertEquals(0, GiLookupTable.lookupGi(""));
    }

    @Test
    public void lookup_null_returnsZero() {
        assertEquals(0, GiLookupTable.lookupGi(null));
    }

    @Test
    public void lookup_numbersOnly_returnsZero() {
        assertEquals(0, GiLookupTable.lookupGi("5000169105306"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Specificity — more-specific keywords win
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void specificity_whiteRiceBeforeGenericRice() {
        // "white rice" (73) should match before "rice" (64)
        int gi = GiLookupTable.lookupGi("Long Grain White Rice");
        assertEquals(73, gi);
    }

    @Test
    public void specificity_iceCreamBeforeGenericCream() {
        // "ice cream" has its own entry
        assertEquals(57, GiLookupTable.lookupGi("Vanilla Ice Cream"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Partial keyword matching
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void partialMatch_strawberry_matchesStrawberr() {
        // Table entry is "strawberr" to match both "strawberry" and "strawberries"
        assertEquals(41, GiLookupTable.lookupGi("Strawberry Jam"));
    }

    @Test
    public void partialMatch_strawberries_matchesStrawberr() {
        assertEquals(41, GiLookupTable.lookupGi("Fresh Strawberries"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GI value range validation
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void allResults_areInValidGiRange() {
        // All returned GI values should be between 0 and 100
        String[] testProducts = {
            "White Bread", "Pasta", "Rice", "Potato", "Apple", "Banana",
            "Milk", "Oat", "Lentil", "Chickpea", "Sugar", "Honey"
        };
        for (String product : testProducts) {
            int gi = GiLookupTable.lookupGi(product);
            assertTrue("GI for '" + product + "' should be 0-100, was " + gi,
                    gi >= 0 && gi <= 100);
        }
    }
}
