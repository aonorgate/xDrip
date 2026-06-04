package com.eveningoutpost.dexdrip.carblookup.standard;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StandardCarbRepositoryTest extends RobolectricTestWithConfig {

    @Test
    public void forSource_returnsRegionalPortions() {
        List<StandardCarbItem> usItems = StandardCarbRepository.forSource(FoodDbSource.US);
        List<StandardCarbItem> ukItems = StandardCarbRepository.forSource(FoodDbSource.UK);
        List<StandardCarbItem> franceItems = StandardCarbRepository.forSource(FoodDbSource.FRANCE);

        assertFalse(usItems.isEmpty());
        assertEquals(108, usItems.size());
        assertEquals(108, ukItems.size());
        assertEquals(108, franceItems.size());
        assertEquals("Rice, white, cooked", usItems.get(0).name);
        assertEquals(158.0, usItems.get(0).mediumPortionGrams, 0.01);
        assertEquals(150.0, ukItems.get(0).mediumPortionGrams, 0.01);
        assertEquals(150.0, franceItems.get(0).mediumPortionGrams, 0.01);
        assertTrue(usItems.get(0).source.contains("USDA"));
        assertTrue(ukItems.get(0).source.contains("UK"));
    }

    @Test
    public void forSource_includesCommonNonBarcodeMealFoods() {
        List<StandardCarbItem> usItems = StandardCarbRepository.forSource(FoodDbSource.US);
        List<StandardCarbItem> ukItems = StandardCarbRepository.forSource(FoodDbSource.UK);
        List<StandardCarbItem> europeItems = StandardCarbRepository.forSource(FoodDbSource.GERMANY);

        assertEquals(185.0, findByName(usItems, "Quinoa, cooked").mediumPortionGrams, 0.01);
        assertEquals(200.0, findByName(ukItems, "Baked beans").mediumPortionGrams, 0.01);
        assertEquals(160.0, findByName(europeItems, "White beans, cooked").mediumPortionGrams, 0.01);
        assertEquals(64.0, findByName(ukItems, "Tortilla wrap").mediumPortionGrams, 0.01);
        assertEquals(35.1, findByName(usItems, "French fries").carbsPer100g, 0.01);
        assertEquals(30.0, findByName(europeItems, "Cornflakes").mediumPortionGrams, 0.01);
    }

    @Test
    public void commonFoods_includeCheesesInDairyGroup() {
        List<StandardCarbItem> dairyItems = StandardCarbRepository.forSourceAndCategory(
                FoodDbSource.US, StandardCarbCategory.DAIRY);

        assertEquals(StandardCarbCategory.DAIRY, findByName(dairyItems, "Cheddar cheese").category);
        assertEquals(1.3, findByName(dairyItems, "Cheddar cheese").carbsPer100g, 0.01);
        assertEquals(30.0, findByName(dairyItems, "Cheddar cheese").mediumPortionGrams, 0.01);
        assertEquals(2.2, findByName(dairyItems, "Mozzarella cheese").carbsPer100g, 0.01);
        assertEquals(3.4, findByName(dairyItems, "Cottage cheese").carbsPer100g, 0.01);
        assertEquals(4.1, findByName(dairyItems, "Cream cheese").carbsPer100g, 0.01);
    }

    @Test
    public void categoriesForSource_returnsUsableGroupsAndFilteredItems() {
        List<StandardCarbCategory> categories = StandardCarbRepository.categoriesForSource(FoodDbSource.UK);

        assertEquals(StandardCarbCategory.STARCHES_GRAINS, categories.get(0));
        assertEquals(11, categories.size());
        assertEquals(20, StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.STARCHES_GRAINS).size());
        assertEquals(10, StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.FRUIT).size());
        assertEquals(8, StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.HOT_DRINKS).size());
        assertEquals(10, StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.COFFEE_SHOP).size());
        assertEquals(10, StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.SNACKS_DESSERTS).size());
        assertEquals("Banana", StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.FRUIT).get(1).name);
        assertEquals("Plain flour", findByName(StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.UK, StandardCarbCategory.BAKING_INGREDIENTS), "Plain flour").name);
        assertEquals("Tomato ketchup", findByName(StandardCarbRepository.forSourceAndCategory(
            FoodDbSource.US, StandardCarbCategory.SAUCES_CONDIMENTS), "Tomato ketchup").name);
    }

    @Test
    public void forSourceAndCategory_returnsOnlyItemsFromRequestedCategory() {
        for (FoodDbSource source : new FoodDbSource[] {FoodDbSource.US, FoodDbSource.UK, FoodDbSource.FRANCE}) {
            int filteredItemCount = 0;
            for (StandardCarbCategory category : StandardCarbCategory.values()) {
                List<StandardCarbItem> items = StandardCarbRepository.forSourceAndCategory(source, category);
                assertFalse(items.isEmpty());
                filteredItemCount += items.size();
                for (StandardCarbItem item : items) {
                    assertEquals(category, item.category);
                }
            }
            assertEquals(StandardCarbRepository.forSource(source).size(), filteredItemCount);
        }
    }

    @Test
    public void forSource_returnsValidDistinctItemsForAllRegions() {
        for (FoodDbSource source : new FoodDbSource[] {FoodDbSource.US, FoodDbSource.UK, FoodDbSource.GERMANY}) {
            Set<String> names = new HashSet<>();
            for (StandardCarbItem item : StandardCarbRepository.forSource(source)) {
                assertTrue("Duplicate standard carb item: " + item.name, names.add(item.name));
                assertTrue(item.name.length() > 0);
                assertTrue(item.carbsPer100g >= 0.0);
                assertTrue(item.smallPortionGrams > 0.0);
                assertTrue(item.mediumPortionGrams >= item.smallPortionGrams);
                assertTrue(item.largePortionGrams >= item.mediumPortionGrams);
                assertTrue(item.source.length() > 0);
                assertTrue(item.portionUnit.length() > 0);
                assertTrue(item.gramsPerPortionUnit > 0.0);
            }
        }
    }

    @Test
    public void liquidCommonFoods_useMillilitersForDisplayedPortions() {
        List<StandardCarbItem> usItems = StandardCarbRepository.forSource(FoodDbSource.US);

        StandardCarbItem milk = findByName(usItems, "2% milk");
        assertEquals(StandardCarbItem.UNIT_MILLILITERS, milk.portionUnit);
        assertEquals(200.0, milk.displayAmountForGrams(milk.mediumPortionGrams), 0.01);
        assertEquals(200.0, milk.gramsForDisplayAmount(200.0), 0.01);
        assertEquals(4.8, milk.carbsPer100PortionUnits(), 0.01);

        StandardCarbItem honey = findByName(usItems, "Honey");
        assertEquals(StandardCarbItem.UNIT_MILLILITERS, honey.portionUnit);
        assertEquals(15.0, honey.displayAmountForGrams(honey.mediumPortionGrams), 0.01);
        assertEquals(21.0, honey.gramsForDisplayAmount(15.0), 0.01);

        StandardCarbItem latte = findByName(usItems, "Latte");
        assertEquals(StandardCarbItem.UNIT_MILLILITERS, latte.portionUnit);
        assertEquals(355.0, latte.displayAmountForGrams(latte.mediumPortionGrams), 0.01);
        assertEquals(5.0, latte.carbsPer100PortionUnits(), 0.01);

        StandardCarbItem rice = usItems.get(0);
        assertEquals(StandardCarbItem.UNIT_GRAMS, rice.portionUnit);
        assertEquals(158.0, rice.displayAmountForGrams(rice.mediumPortionGrams), 0.01);
    }

    @Test
    public void commonFoods_includeHotDrinksCoffeeShopItemsAndSnacks() {
        List<StandardCarbItem> usItems = StandardCarbRepository.forSource(FoodDbSource.US);
        List<StandardCarbItem> ukItems = StandardCarbRepository.forSource(FoodDbSource.UK);

        assertEquals(StandardCarbCategory.HOT_DRINKS, findByName(usItems, "Black coffee, no sugar").category);
        assertEquals(StandardCarbCategory.HOT_DRINKS, findByName(usItems, "Hot chocolate").category);
        assertEquals(StandardCarbCategory.COFFEE_SHOP, findByName(usItems, "Blueberry muffin").category);
        assertEquals(StandardCarbCategory.COFFEE_SHOP, findByName(usItems, "Flat white").category);
        assertEquals(StandardCarbCategory.SNACKS_DESSERTS, findByName(usItems, "Ice cream, vanilla").category);
        assertEquals(StandardCarbCategory.SNACKS_DESSERTS, findByName(usItems, "Brownie").category);
        assertEquals(StandardCarbCategory.SNACKS_DESSERTS, findByName(usItems, "Chocolate chip cookie").category);
        assertEquals("Potato chips", findByName(usItems, "Potato chips").name);
        assertEquals("Crisps", findByName(ukItems, "Crisps").name);
    }

    @Test
    public void commonFoods_includeRegionalIngredientNamesAndNullSourceFallsBackToEurope() {
        List<StandardCarbItem> usItems = StandardCarbRepository.forSource(FoodDbSource.US);
        List<StandardCarbItem> ukItems = StandardCarbRepository.forSource(FoodDbSource.UK);
        List<StandardCarbItem> nullSourceItems = StandardCarbRepository.forSource(null);

        assertEquals("Zucchini", findByName(usItems, "Zucchini").name);
        assertEquals("All-purpose flour", findByName(usItems, "All-purpose flour").name);
        assertEquals("2% milk", findByName(usItems, "2% milk").name);
        assertEquals("Courgette", findByName(ukItems, "Courgette").name);
        assertEquals("Plain flour", findByName(ukItems, "Plain flour").name);
        assertEquals("Semi-skimmed milk", findByName(nullSourceItems, "Semi-skimmed milk").name);
        assertEquals("European standard portion estimate", nullSourceItems.get(0).source);
    }

    @Test
    public void standardCarbCategoryLabels_areUserFacingGroupNames() {
        assertEquals("Starches, grains & breads", StandardCarbCategory.STARCHES_GRAINS.label());
        assertEquals("Beans & pulses", StandardCarbCategory.BEANS_PULSES.label());
        assertEquals("Vegetables", StandardCarbCategory.VEGETABLES.label());
        assertEquals("Fruit", StandardCarbCategory.FRUIT.label());
        assertEquals("Dairy & alternatives", StandardCarbCategory.DAIRY.label());
        assertEquals("Hot drinks", StandardCarbCategory.HOT_DRINKS.label());
        assertEquals("Coffee shop items", StandardCarbCategory.COFFEE_SHOP.label());
        assertEquals("Snacks & desserts", StandardCarbCategory.SNACKS_DESSERTS.label());
        assertEquals("Baking & cooking ingredients", StandardCarbCategory.BAKING_INGREDIENTS.label());
        assertEquals("Nuts, seeds & spreads", StandardCarbCategory.NUTS_SEEDS.label());
        assertEquals("Sauces & condiments", StandardCarbCategory.SAUCES_CONDIMENTS.label());
    }

    @Test
    public void foodDbSource_unknownKeyDefaultsToWorld() {
        assertEquals(FoodDbSource.WORLD, FoodDbSource.fromKey("missing"));
        assertEquals(FoodDbSource.WORLD, FoodDbSource.fromKey(null));
    }

    @Test
    public void foodDbSource_exposesRegionalBarcodeEndpointsAndLabels() {
        assertEquals("https://us.openfoodfacts.org/api/v2/product/", FoodDbSource.US.baseUrl());
        assertEquals("Source: Open Food Facts US", FoodDbSource.US.sourceLabel());
        assertEquals("Source: cached product - Open Food Facts UK", FoodDbSource.UK.cachedSourceLabel());
        assertEquals(FoodDbSource.StandardFoodRegion.EUROPE, FoodDbSource.GERMANY.standardFoodRegion());
    }

    private StandardCarbItem findByName(List<StandardCarbItem> items, String name) {
        for (StandardCarbItem item : items) {
            if (name.equals(item.name)) {
                return item;
            }
        }
        throw new AssertionError("Missing standard carb item: " + name);
    }
}