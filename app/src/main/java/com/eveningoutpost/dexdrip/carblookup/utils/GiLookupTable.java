package com.eveningoutpost.dexdrip.carblookup.utils;

/**
 * Simple keyword-based Glycaemic Index lookup table.
 * Returns 0 when the product name does not match any known keyword (unknown GI).
 * Longer / more specific keywords take priority over shorter ones.
 */
public class GiLookupTable {

    private static final String[][] ENTRIES = {
            // More-specific entries first so they win over shorter matches
            {"orange juice",   "57"},
            {"white bread",    "75"},
            {"sweet potato",   "63"},
            {"ice cream",      "57"},
            {"corn flake",     "81"},
            {"cornflake",      "81"},
            {"brown rice",     "55"},
            {"white rice",     "73"},
            {"basmati",        "57"},
            {"wholemeal",      "69"},
            {"whole grain",    "55"},
            {"sourdough",      "54"},
            // General entries
            {"bread",       "70"},
            {"oat",         "55"},
            {"porridge",    "55"},
            {"muesli",      "57"},
            {"rice",        "64"},
            {"pasta",       "49"},
            {"noodle",      "49"},
            {"potato",      "78"},
            {"apple",       "36"},
            {"banana",      "51"},
            {"orange",      "43"},
            {"grape",       "46"},
            {"mango",       "51"},
            {"watermelon",  "76"},
            {"strawberr",   "41"},
            {"berry",       "40"},
            {"milk",        "31"},
            {"yogurt",      "35"},
            {"yoghurt",     "35"},
            {"cheese",      "0"},
            {"chocolate",   "40"},
            {"biscuit",     "70"},
            {"cookie",      "55"},
            {"cake",        "56"},
            {"muffin",      "62"},
            {"cracker",     "65"},
            {"crisp",       "75"},
            {"chip",        "75"},
            {"sugar",       "65"},
            {"honey",       "61"},
            {"jam",         "51"},
            {"pizza",       "60"},
            {"lentil",      "29"},
            {"chickpea",    "28"},
            {"bean",        "28"},
            {"pea",         "48"},
            {"carrot",      "47"},
            {"corn",        "52"},
            {"cereal",      "70"},
    };

    /**
     * Looks up the GI for a product by scanning its name for known keywords.
     * Returns 0 if no match is found (GI unknown).
     */
    public static int lookupGi(String productName) {
        if (productName == null || productName.isEmpty()) return 0;
        String lower = productName.toLowerCase();
        for (String[] entry : ENTRIES) {
            if (lower.contains(entry[0])) {
                int gi = Integer.parseInt(entry[1]);
                return gi; // first (most-specific) match wins
            }
        }
        return 0;
    }
}
