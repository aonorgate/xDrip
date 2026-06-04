package com.eveningoutpost.dexdrip.carblookup.api;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

public enum FoodDbSource {
    WORLD("off_world", "Open Food Facts", "https://world.openfoodfacts.org/api/v2/product/", StandardFoodRegion.EUROPE),
    US("off_us", "Open Food Facts US", "https://us.openfoodfacts.org/api/v2/product/", StandardFoodRegion.US),
    UK("off_uk", "Open Food Facts UK", "https://uk.openfoodfacts.org/api/v2/product/", StandardFoodRegion.UK),
    FRANCE("off_fr", "Open Food Facts France", "https://fr.openfoodfacts.org/api/v2/product/", StandardFoodRegion.EUROPE),
    GERMANY("off_de", "Open Food Facts Germany", "https://de.openfoodfacts.org/api/v2/product/", StandardFoodRegion.EUROPE);

    public static final String PREF_KEY = "carblookup_food_db_source";
    public static final String DEFAULT_KEY = "off_world";

    private final String key;
    private final String label;
    private final String baseUrl;
    private final StandardFoodRegion standardFoodRegion;

    FoodDbSource(String key, String label, String baseUrl, StandardFoodRegion standardFoodRegion) {
        this.key = key;
        this.label = label;
        this.baseUrl = baseUrl;
        this.standardFoodRegion = standardFoodRegion;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String sourceLabel() {
        return "Source: " + label;
    }

    public String cachedSourceLabel() {
        return "Source: cached product - " + label;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public StandardFoodRegion standardFoodRegion() {
        return standardFoodRegion;
    }

    public static FoodDbSource current() {
        return fromKey(Pref.getString(PREF_KEY, DEFAULT_KEY));
    }

    public static FoodDbSource fromKey(String key) {
        for (FoodDbSource source : values()) {
            if (source.key.equals(key)) {
                return source;
            }
        }
        return WORLD;
    }

    public enum StandardFoodRegion {
        US,
        UK,
        EUROPE
    }
}