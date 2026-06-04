package com.eveningoutpost.dexdrip.carblookup.standard;

import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StandardCarbRepository {
    private static final double GRAMS_PER_ML = 1.0;
    private static final double HONEY_GRAMS_PER_ML = 1.4;
    private static final double SYRUP_GRAMS_PER_ML = 1.33;

    private StandardCarbRepository() {
    }

    /** Forces re-read of common_foods.json on next access. Call after user edits the file. */
    public static void reloadUserFoods() {
        StandardCarbFileLoader.invalidateCache();
    }

    public static List<StandardCarbItem> forSource(FoodDbSource source) {
        FoodDbSource.StandardFoodRegion region = source != null
                ? source.standardFoodRegion() : FoodDbSource.StandardFoodRegion.EUROPE;
        List<StandardCarbItem> builtIn;
        switch (region) {
            case US:
                builtIn = usItems();
                break;
            case UK:
                builtIn = ukItems();
                break;
            case EUROPE:
            default:
                builtIn = europeanItems();
                break;
        }
        return StandardCarbFileLoader.mergeWithDefaults(builtIn);
    }

    /** Writes a seed common_foods.json template with the built-in items for the given source. */
    public static void exportDefaultsToFile(FoodDbSource source) {
        FoodDbSource.StandardFoodRegion region = source != null
                ? source.standardFoodRegion() : FoodDbSource.StandardFoodRegion.EUROPE;
        List<StandardCarbItem> builtIn;
        switch (region) {
            case US:
                builtIn = usItems();
                break;
            case UK:
                builtIn = ukItems();
                break;
            case EUROPE:
            default:
                builtIn = europeanItems();
                break;
        }
        StandardCarbFileLoader.writeSeedFile(builtIn);
    }

    public static List<StandardCarbCategory> categoriesForSource(FoodDbSource source) {
        List<StandardCarbItem> sourceItems = forSource(source);
        List<StandardCarbCategory> categories = new ArrayList<>();
        for (StandardCarbCategory category : StandardCarbCategory.values()) {
            if (!filterByCategory(sourceItems, category).isEmpty()) {
                categories.add(category);
            }
        }
        return Collections.unmodifiableList(categories);
    }

    public static List<StandardCarbItem> forSourceAndCategory(FoodDbSource source, StandardCarbCategory category) {
        return Collections.unmodifiableList(filterByCategory(forSource(source), category));
    }

    private static List<StandardCarbItem> filterByCategory(List<StandardCarbItem> items, StandardCarbCategory category) {
        List<StandardCarbItem> filtered = new ArrayList<>();
        for (StandardCarbItem item : items) {
            if (item.category == category) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private static List<StandardCarbItem> usItems() {
        List<StandardCarbItem> items = new ArrayList<>();
        String source = "USDA FoodData Central";
        addUsBaseItems(items, source);
        addUsExtraItems(items, source);
        return Collections.unmodifiableList(items);
    }

    private static List<StandardCarbItem> ukItems() {
        List<StandardCarbItem> items = new ArrayList<>();
        String source = "UK Composition of Foods Integrated Dataset";
        addUkBaseItems(items, source);
        addUkExtraItems(items, source);
        return Collections.unmodifiableList(items);
    }

    private static List<StandardCarbItem> europeanItems() {
        List<StandardCarbItem> items = new ArrayList<>();
        String source = "European standard portion estimate";
        addEuropeBaseItems(items, source);
        addEuropeExtraItems(items, source);
        return Collections.unmodifiableList(items);
    }

    private static void addUsBaseItems(List<StandardCarbItem> items, String source) {
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, white, cooked", 28.2, 100, 158, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pasta, cooked", 30.9, 100, 140, 280, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Potato, boiled", 20.1, 110, 170, 300, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Sweet potato, baked", 20.7, 100, 150, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bread, white", 49.4, 28, 56, 84, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Oats, rolled, dry", 66.3, 30, 40, 60, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Couscous, cooked", 23.2, 100, 157, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Noodles, egg, cooked", 25.2, 100, 160, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, brown, cooked", 25.6, 100, 158, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, basmati, cooked", 25.2, 100, 158, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Mashed potato", 16.9, 100, 180, 300, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "French fries", 35.1, 70, 117, 170, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Quinoa, cooked", 21.3, 90, 185, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bulgur wheat, cooked", 18.6, 90, 182, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pearled barley, cooked", 28.2, 90, 157, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Tortilla, flour", 49.3, 45, 70, 110, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pita bread", 55.7, 30, 60, 90, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Naan bread", 50.4, 50, 90, 130, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bagel, plain", 53.0, 45, 95, 140, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Cornflakes", 84.1, 25, 35, 50, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Lentils, cooked", 20.1, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Chickpeas, cooked", 27.4, 80, 164, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Kidney beans, cooked", 22.8, 80, 177, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Black beans, cooked", 23.7, 80, 172, 240, source);
        add(items, StandardCarbCategory.VEGETABLES, "Sweetcorn, kernels", 19.0, 80, 165, 240, source);
        add(items, StandardCarbCategory.VEGETABLES, "Peas, green, cooked", 15.6, 80, 160, 240, source);
    }

    private static void addUkBaseItems(List<StandardCarbItem> items, String source) {
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, white, cooked", 28.0, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pasta, cooked", 31.0, 75, 180, 250, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Potato, boiled", 17.2, 80, 180, 280, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Sweet potato, baked", 21.3, 80, 160, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bread, white", 46.0, 36, 72, 108, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Oats, porridge, dry", 60.0, 30, 40, 60, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Couscous, cooked", 23.0, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Noodles, cooked", 25.0, 75, 160, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, brown, cooked", 23.5, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, basmati, cooked", 27.0, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Mashed potato", 15.9, 80, 180, 280, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Chips, oven cooked", 31.0, 80, 150, 220, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Quinoa, cooked", 18.5, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bulgur wheat, cooked", 18.0, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pearl barley, cooked", 24.0, 75, 150, 225, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Tortilla wrap", 50.0, 40, 64, 100, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pitta bread", 52.0, 30, 60, 90, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Naan bread", 48.0, 50, 90, 130, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bagel, plain", 48.0, 45, 90, 130, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Cornflakes", 84.0, 25, 30, 45, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Lentils, boiled", 16.9, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Chickpeas, boiled", 18.6, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Kidney beans, boiled", 15.6, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Baked beans", 14.0, 100, 200, 300, source);
        add(items, StandardCarbCategory.VEGETABLES, "Sweetcorn", 18.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.VEGETABLES, "Peas, boiled", 10.0, 80, 160, 240, source);
    }

    private static void addEuropeBaseItems(List<StandardCarbItem> items, String source) {
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, white, cooked", 28.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pasta, cooked", 30.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Potato, boiled", 18.0, 80, 180, 280, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Sweet potato, baked", 21.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bread, white", 48.0, 40, 80, 120, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Oats, dry", 62.0, 30, 40, 60, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Couscous, cooked", 23.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Noodles, cooked", 25.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, brown, cooked", 24.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Rice, basmati, cooked", 26.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Mashed potato", 16.0, 80, 180, 280, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Potato fries", 33.0, 80, 150, 220, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Quinoa, cooked", 19.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bulgur wheat, cooked", 18.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pearl barley, cooked", 25.0, 80, 150, 230, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Tortilla wrap", 50.0, 40, 65, 100, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Pita bread", 52.0, 30, 60, 90, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Naan bread", 49.0, 50, 90, 130, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Bagel, plain", 50.0, 45, 90, 135, source);
        add(items, StandardCarbCategory.STARCHES_GRAINS, "Cornflakes", 84.0, 25, 30, 45, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Lentils, cooked", 18.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Chickpeas, cooked", 20.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "Kidney beans, cooked", 16.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.BEANS_PULSES, "White beans, cooked", 18.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.VEGETABLES, "Sweetcorn", 18.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.VEGETABLES, "Peas, cooked", 11.0, 80, 160, 240, source);
    }

    private static void addUsExtraItems(List<StandardCarbItem> items, String source) {
        addCommonExtraItems(items, source, "Zucchini", "2% milk", "Plain Greek yogurt", "All-purpose flour", "Cornstarch", "Maple syrup", "Tomato ketchup");
        addHotDrinkCoffeeShopAndSnackItems(items, source, "Granola bar", "Potato chips", "Doughnut, glazed");
    }

    private static void addUkExtraItems(List<StandardCarbItem> items, String source) {
        addCommonExtraItems(items, source, "Courgette", "Semi-skimmed milk", "Greek-style yogurt", "Plain flour", "Cornflour", "Golden syrup", "Tomato ketchup");
        addHotDrinkCoffeeShopAndSnackItems(items, source, "Cereal bar", "Crisps", "Doughnut, jam");
    }

    private static void addEuropeExtraItems(List<StandardCarbItem> items, String source) {
        addCommonExtraItems(items, source, "Courgette", "Semi-skimmed milk", "Plain Greek yogurt", "Plain flour", "Corn starch", "Maple syrup", "Tomato ketchup");
        addHotDrinkCoffeeShopAndSnackItems(items, source, "Cereal bar", "Crisps", "Doughnut, glazed");
    }

    private static void addCommonExtraItems(List<StandardCarbItem> items, String source, String courgetteName,
            String milkName, String greekYogurtName, String flourName, String cornflourName, String syrupName,
            String ketchupName) {
        add(items, StandardCarbCategory.VEGETABLES, "Carrot, raw", 9.6, 40, 80, 120, source);
        add(items, StandardCarbCategory.VEGETABLES, "Onion, raw", 9.3, 40, 80, 150, source);
        add(items, StandardCarbCategory.VEGETABLES, "Tomato, raw", 3.9, 80, 120, 180, source);
        add(items, StandardCarbCategory.VEGETABLES, "Broccoli, cooked", 7.2, 80, 150, 220, source);
        add(items, StandardCarbCategory.VEGETABLES, "Cauliflower, cooked", 4.1, 80, 150, 220, source);
        add(items, StandardCarbCategory.VEGETABLES, "Bell pepper, raw", 6.0, 50, 100, 150, source);
        add(items, StandardCarbCategory.VEGETABLES, "Mushrooms, raw", 3.3, 50, 100, 150, source);
        add(items, StandardCarbCategory.VEGETABLES, "Spinach, raw", 3.6, 30, 60, 100, source);
        add(items, StandardCarbCategory.VEGETABLES, courgetteName, 3.1, 80, 150, 220, source);
        add(items, StandardCarbCategory.VEGETABLES, "Butternut squash, cooked", 11.7, 80, 160, 240, source);
        add(items, StandardCarbCategory.VEGETABLES, "Parsnip, cooked", 18.0, 60, 120, 180, source);
        add(items, StandardCarbCategory.VEGETABLES, "Avocado", 8.5, 50, 100, 150, source);
        add(items, StandardCarbCategory.FRUIT, "Apple", 13.8, 80, 150, 220, source);
        add(items, StandardCarbCategory.FRUIT, "Banana", 22.8, 60, 118, 180, source);
        add(items, StandardCarbCategory.FRUIT, "Orange", 11.8, 80, 140, 200, source);
        add(items, StandardCarbCategory.FRUIT, "Pear", 15.2, 80, 160, 230, source);
        add(items, StandardCarbCategory.FRUIT, "Grapes", 18.1, 60, 120, 180, source);
        add(items, StandardCarbCategory.FRUIT, "Strawberries", 7.7, 80, 150, 220, source);
        add(items, StandardCarbCategory.FRUIT, "Blueberries", 14.5, 60, 100, 150, source);
        add(items, StandardCarbCategory.FRUIT, "Mango", 15.0, 80, 160, 240, source);
        add(items, StandardCarbCategory.FRUIT, "Pineapple", 13.1, 80, 160, 240, source);
        add(items, StandardCarbCategory.FRUIT, "Melon", 8.2, 100, 200, 300, source);
        addLiquid(items, StandardCarbCategory.DAIRY, milkName, 4.8, 100, 200, 300, source);
        addLiquid(items, StandardCarbCategory.DAIRY, "Whole milk", 4.7, 100, 200, 300, source);
        add(items, StandardCarbCategory.DAIRY, "Plain yogurt", 7.0, 80, 150, 250, source);
        add(items, StandardCarbCategory.DAIRY, greekYogurtName, 3.9, 80, 150, 250, source);
        addLiquid(items, StandardCarbCategory.DAIRY, "Oat milk", 6.7, 100, 200, 300, source);
        addLiquid(items, StandardCarbCategory.DAIRY, "Soya milk, unsweetened", 0.7, 100, 200, 300, source);
        add(items, StandardCarbCategory.DAIRY, "Cheddar cheese", 1.3, 20, 30, 50, source);
        add(items, StandardCarbCategory.DAIRY, "Mozzarella cheese", 2.2, 30, 60, 100, source);
        add(items, StandardCarbCategory.DAIRY, "Cottage cheese", 3.4, 50, 100, 150, source);
        add(items, StandardCarbCategory.DAIRY, "Cream cheese", 4.1, 15, 30, 60, source);
        add(items, StandardCarbCategory.BAKING_INGREDIENTS, flourName, 76.3, 15, 30, 60, source);
        add(items, StandardCarbCategory.BAKING_INGREDIENTS, "Wholemeal flour", 60.0, 15, 30, 60, source);
        add(items, StandardCarbCategory.BAKING_INGREDIENTS, "Caster sugar", 100.0, 5, 12, 25, source);
        add(items, StandardCarbCategory.BAKING_INGREDIENTS, "Brown sugar", 98.0, 5, 12, 25, source);
        addLiquid(items, StandardCarbCategory.BAKING_INGREDIENTS, "Honey", 82.4, 7, 21, 42, source, HONEY_GRAMS_PER_ML);
        addLiquid(items, StandardCarbCategory.BAKING_INGREDIENTS, syrupName, 67.0, 10, 20, 40, source, SYRUP_GRAMS_PER_ML);
        add(items, StandardCarbCategory.BAKING_INGREDIENTS, cornflourName, 91.0, 5, 10, 20, source);
        add(items, StandardCarbCategory.BAKING_INGREDIENTS, "Breadcrumbs", 72.0, 15, 30, 60, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Almonds", 21.6, 15, 30, 45, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Peanuts", 16.1, 15, 30, 45, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Cashews", 30.2, 15, 30, 45, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Peanut butter", 22.3, 16, 32, 48, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Chia seeds", 42.1, 10, 20, 30, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Sunflower seeds", 20.0, 10, 25, 40, source);
        add(items, StandardCarbCategory.NUTS_SEEDS, "Hummus", 14.3, 30, 60, 100, source);
        add(items, StandardCarbCategory.SAUCES_CONDIMENTS, "Tomato puree", 18.9, 15, 30, 60, source);
        add(items, StandardCarbCategory.SAUCES_CONDIMENTS, ketchupName, 27.4, 15, 30, 60, source);
        addLiquid(items, StandardCarbCategory.SAUCES_CONDIMENTS, "Passata", 5.0, 80, 150, 250, source);
        add(items, StandardCarbCategory.SAUCES_CONDIMENTS, "Salsa", 7.0, 30, 60, 100, source);
        add(items, StandardCarbCategory.SAUCES_CONDIMENTS, "Jam", 69.0, 10, 20, 40, source);
        addLiquid(items, StandardCarbCategory.SAUCES_CONDIMENTS, "Gravy", 6.0, 50, 100, 150, source);
        addLiquid(items, StandardCarbCategory.SAUCES_CONDIMENTS, "Curry sauce", 9.0, 80, 150, 250, source);
    }

    private static void addHotDrinkCoffeeShopAndSnackItems(List<StandardCarbItem> items, String source,
            String cerealBarName, String crispName, String doughnutName) {
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Black coffee, no sugar", 0.0, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Tea, no milk or sugar", 0.0, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Coffee with milk", 1.7, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Tea with milk", 1.0, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Hot chocolate", 10.5, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Chai latte mix", 9.5, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Matcha latte", 7.0, 200, 300, 450, source);
        addLiquid(items, StandardCarbCategory.HOT_DRINKS, "Malted hot drink", 11.0, 200, 300, 450, source);

        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Americano", 0.3, 240, 355, 475, source);
        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Latte", 5.0, 240, 355, 475, source);
        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Cappuccino", 4.0, 180, 300, 400, source);
        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Flat white", 4.8, 160, 220, 300, source);
        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Mocha", 9.0, 240, 355, 475, source);
        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Iced latte", 4.5, 240, 355, 475, source);
        addLiquid(items, StandardCarbCategory.COFFEE_SHOP, "Blended iced coffee", 12.0, 300, 450, 600, source);
        add(items, StandardCarbCategory.COFFEE_SHOP, "Blueberry muffin", 52.0, 60, 110, 150, source);
        add(items, StandardCarbCategory.COFFEE_SHOP, "Croissant", 45.8, 45, 70, 100, source);
        add(items, StandardCarbCategory.COFFEE_SHOP, "Cinnamon roll", 57.0, 60, 100, 140, source);

        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Ice cream, vanilla", 23.6, 60, 100, 150, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Brownie", 55.0, 40, 70, 100, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Chocolate chip cookie", 64.0, 25, 45, 70, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, doughnutName, 50.0, 45, 70, 100, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Sponge cake slice", 55.0, 50, 90, 130, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Chocolate bar", 60.0, 25, 45, 65, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, cerealBarName, 64.0, 25, 40, 60, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, crispName, 52.0, 25, 40, 60, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Popcorn, sweet", 63.0, 20, 40, 60, source);
        add(items, StandardCarbCategory.SNACKS_DESSERTS, "Rice pudding", 18.0, 100, 150, 250, source);
    }

    private static void add(List<StandardCarbItem> items, StandardCarbCategory category, String name,
            double carbsPer100g, double smallPortionGrams, double mediumPortionGrams,
            double largePortionGrams, String source) {
        items.add(new StandardCarbItem(category, name, carbsPer100g, smallPortionGrams,
                mediumPortionGrams, largePortionGrams, source));
    }

    private static void addLiquid(List<StandardCarbItem> items, StandardCarbCategory category, String name,
            double carbsPer100g, double smallPortionGrams, double mediumPortionGrams,
            double largePortionGrams, String source) {
        addLiquid(items, category, name, carbsPer100g, smallPortionGrams, mediumPortionGrams,
                largePortionGrams, source, GRAMS_PER_ML);
    }

    private static void addLiquid(List<StandardCarbItem> items, StandardCarbCategory category, String name,
            double carbsPer100g, double smallPortionGrams, double mediumPortionGrams,
            double largePortionGrams, String source, double gramsPerMilliliter) {
        items.add(new StandardCarbItem(category, name, carbsPer100g, smallPortionGrams,
                mediumPortionGrams, largePortionGrams, source,
                StandardCarbItem.UNIT_MILLILITERS, gramsPerMilliliter));
    }
}