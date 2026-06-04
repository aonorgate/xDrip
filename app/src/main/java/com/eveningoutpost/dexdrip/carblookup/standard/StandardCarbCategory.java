package com.eveningoutpost.dexdrip.carblookup.standard;

public enum StandardCarbCategory {
    STARCHES_GRAINS("Starches, grains & breads"),
    BEANS_PULSES("Beans & pulses"),
    VEGETABLES("Vegetables"),
    FRUIT("Fruit"),
    DAIRY("Dairy & alternatives"),
    HOT_DRINKS("Hot drinks"),
    COFFEE_SHOP("Coffee shop items"),
    SNACKS_DESSERTS("Snacks & desserts"),
    BAKING_INGREDIENTS("Baking & cooking ingredients"),
    NUTS_SEEDS("Nuts, seeds & spreads"),
    SAUCES_CONDIMENTS("Sauces & condiments");

    private final String label;

    StandardCarbCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}