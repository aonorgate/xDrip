package com.eveningoutpost.dexdrip.carblookup.utils;

import org.junit.Test;

import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pure JVM unit tests for CarbLookupCalculator.
 * No Android framework required — runs on the local JVM.
 */
public class CarbLookupCalculatorTest {

    private static final double DELTA = 0.01;

    // ═══════════════════════════════════════════════════════════════════════
    // calculateCarbs() — basic calculations
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void calculateCarbs_standardPortion_correctResult() {
        // Weetabix: 68.4g/100g, 37.5g portion → 25.65 → rounds to 25.7
        double result = CarbLookupCalculator.calculateCarbs(68.4, 37.5);
        assertEquals(25.7, result, DELTA);
    }

    @Test
    public void calculateCarbs_100gPortion_equalsCarbsPer100g() {
        double result = CarbLookupCalculator.calculateCarbs(68.4, 100.0);
        assertEquals(68.4, result, DELTA);
    }

    @Test
    public void calculateCarbs_zeroCarbFood_returnsZero() {
        double result = CarbLookupCalculator.calculateCarbs(0.0, 250.0);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    public void calculateCarbs_smallPortion_correctResult() {
        // 4.7g/100g, 20g portion → 0.94 → rounds to 0.9
        double result = CarbLookupCalculator.calculateCarbs(4.7, 20.0);
        assertEquals(0.9, result, DELTA);
    }

    @Test
    public void calculateCarbs_largePortion_correctResult() {
        // 72g/100g, 500g portion → 360g
        double result = CarbLookupCalculator.calculateCarbs(72.0, 500.0);
        assertEquals(360.0, result, DELTA);
    }

    @Test
    public void calculateCarbs_roundsUpAt5() {
        // 13.3 * 75 / 100 = 9.975 → rounds to 10.0
        double result = CarbLookupCalculator.calculateCarbs(13.3, 75.0);
        assertEquals(10.0, result, DELTA);
    }

    @Test
    public void calculateCarbs_roundsDownBelow5() {
        // 10.0 * 44 / 100 = 4.4 → stays 4.4
        double result = CarbLookupCalculator.calculateCarbs(10.0, 44.0);
        assertEquals(4.4, result, DELTA);
    }

    @Test
    public void calculateCarbs_fractionalCarbs_handledCorrectly() {
        // 3.8 * 125 / 100 = 4.75 → rounds to 4.8
        double result = CarbLookupCalculator.calculateCarbs(3.8, 125.0);
        assertEquals(4.8, result, DELTA);
    }

    @Test
    public void calculateCarbs_verySmallCarbs_roundedCorrectly() {
        // 0.1 * 10 / 100 = 0.01 → rounds to 0.0
        double result = CarbLookupCalculator.calculateCarbs(0.1, 10.0);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    public void calculateCarbs_resultAlwaysHasAtMostOneDecimal() {
        double result = CarbLookupCalculator.calculateCarbs(33.3, 77.7);
        double reRounded = Math.round(result * 10.0) / 10.0;
        assertEquals(reRounded, result, 0.001);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // calculateCarbs() — validation
    // ═══════════════════════════════════════════════════════════════════════

    @Test(expected = IllegalArgumentException.class)
    public void calculateCarbs_zeroPortion_throws() {
        CarbLookupCalculator.calculateCarbs(10.0, 0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculateCarbs_negativePortion_throws() {
        CarbLookupCalculator.calculateCarbs(10.0, -50.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculateCarbs_negativeCarbs_throws() {
        CarbLookupCalculator.calculateCarbs(-5.0, 100.0);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // totalMealCarbs() — summation
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    public void totalMealCarbs_nullList_returnsZero() {
        assertEquals(0.0, CarbLookupCalculator.totalMealCarbs(null), DELTA);
    }

    @Test
    public void totalMealCarbs_emptyList_returnsZero() {
        assertEquals(0.0, CarbLookupCalculator.totalMealCarbs(new ArrayList<>()), DELTA);
    }

    @Test
    public void totalMealCarbs_singleItem_returnsThatItemCarbs() {
        MealItem item = new MealItem();
        item.carbsForPortion = 25.7;
        List<MealItem> items = Collections.singletonList(item);
        assertEquals(25.7, CarbLookupCalculator.totalMealCarbs(items), DELTA);
    }

    @Test
    public void totalMealCarbs_multipleItems_sumsCorrectly() {
        MealItem item1 = new MealItem();
        item1.carbsForPortion = 25.7;
        MealItem item2 = new MealItem();
        item2.carbsForPortion = 7.1;
        MealItem item3 = new MealItem();
        item3.carbsForPortion = 42.5;

        List<MealItem> items = Arrays.asList(item1, item2, item3);
        // 25.7 + 7.1 + 42.5 = 75.3
        assertEquals(75.3, CarbLookupCalculator.totalMealCarbs(items), DELTA);
    }

    @Test
    public void totalMealCarbs_resultRoundedToOneDecimal() {
        MealItem item1 = new MealItem();
        item1.carbsForPortion = 1.11;
        MealItem item2 = new MealItem();
        item2.carbsForPortion = 2.22;
        MealItem item3 = new MealItem();
        item3.carbsForPortion = 3.33;

        List<MealItem> items = Arrays.asList(item1, item2, item3);
        double result = CarbLookupCalculator.totalMealCarbs(items);
        // 1.11 + 2.22 + 3.33 = 6.66 → 6.7
        double reRounded = Math.round(result * 10.0) / 10.0;
        assertEquals(reRounded, result, 0.001);
    }

    @Test
    public void totalMealCarbs_zeroItemCarbs_handledCorrectly() {
        MealItem item1 = new MealItem();
        item1.carbsForPortion = 0.0;
        MealItem item2 = new MealItem();
        item2.carbsForPortion = 15.3;

        List<MealItem> items = Arrays.asList(item1, item2);
        assertEquals(15.3, CarbLookupCalculator.totalMealCarbs(items), DELTA);
    }
}
