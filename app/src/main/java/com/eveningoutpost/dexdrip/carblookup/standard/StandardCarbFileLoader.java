package com.eveningoutpost.dexdrip.carblookup.standard;

import android.os.Environment;

import com.eveningoutpost.dexdrip.models.UserError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads user-customised common foods from /sdcard/xdrip/common_foods.json.
 * If the file is absent the hardcoded defaults are used and a seed file is written.
 * If the file is present its items are merged with the defaults (user items first per category,
 * allowing overrides by matching name+category).
 */
public class StandardCarbFileLoader {
    private static final String TAG = "StandardCarbFileLoader";
    private static final String FILE_NAME = "common_foods.json";

    private static volatile List<JsonCarbItem> cachedUserItems;
    private static volatile long cachedFileModified = -1;

    private StandardCarbFileLoader() {
    }

    /**
     * Merges user JSON items with the provided built-in defaults for the given source.
     * User items override built-in items with the same name+category; additional user items are appended.
     */
    public static List<StandardCarbItem> mergeWithDefaults(List<StandardCarbItem> defaults) {
        List<JsonCarbItem> userItems = loadUserItems();
        if (userItems == null || userItems.isEmpty()) {
            return defaults;
        }
        List<StandardCarbItem> merged = new ArrayList<>(defaults.size() + userItems.size());
        // Build set of user-override keys (category+name lowercase)
        List<String> userKeys = new ArrayList<>(userItems.size());
        for (JsonCarbItem ui : userItems) {
            userKeys.add(overrideKey(ui.category, ui.name));
        }
        // Add defaults that are not overridden
        for (StandardCarbItem def : defaults) {
            String key = overrideKey(def.category.name(), def.name);
            if (!userKeys.contains(key)) {
                merged.add(def);
            }
        }
        // Add all user items
        for (JsonCarbItem ui : userItems) {
            StandardCarbCategory cat = parseCategoryLenient(ui.category);
            if (cat == null) {
                UserError.Log.w(TAG, "Ignoring unknown category: " + ui.category + " for item: " + ui.name);
                continue;
            }
            StandardCarbItem item;
            if (ui.portionUnit != null && ui.portionUnit.equalsIgnoreCase("ml")) {
                double gPerMl = ui.gramsPerMl > 0 ? ui.gramsPerMl : 1.0;
                item = new StandardCarbItem(cat, ui.name, ui.carbsPer100g,
                        ui.smallPortion, ui.mediumPortion, ui.largePortion,
                        ui.source != null ? ui.source : "User",
                        StandardCarbItem.UNIT_MILLILITERS, gPerMl);
            } else {
                item = new StandardCarbItem(cat, ui.name, ui.carbsPer100g,
                        ui.smallPortion, ui.mediumPortion, ui.largePortion,
                        ui.source != null ? ui.source : "User");
            }
            merged.add(item);
        }
        return Collections.unmodifiableList(merged);
    }

    /**
     * Writes common_foods.json with all built-in items so users have a template to edit.
     * Overwrites any existing file — intended to be called from an explicit user action (export).
     */
    public static void writeSeedFile(List<StandardCarbItem> defaults) {
        File file = getFile();
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            List<JsonCarbItem> jsonItems = new ArrayList<>(defaults.size());
            for (StandardCarbItem item : defaults) {
                JsonCarbItem ji = new JsonCarbItem();
                ji.category = item.category.name();
                ji.name = item.name;
                ji.carbsPer100g = item.carbsPer100g;
                ji.smallPortion = item.smallPortionGrams;
                ji.mediumPortion = item.mediumPortionGrams;
                ji.largePortion = item.largePortionGrams;
                ji.source = item.source;
                if (!StandardCarbItem.UNIT_GRAMS.equals(item.portionUnit)) {
                    ji.portionUnit = item.portionUnit;
                    ji.gramsPerMl = item.gramsPerPortionUnit;
                }
                jsonItems.add(ji);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(jsonItems);
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
            UserError.Log.d(TAG, "Wrote seed common_foods.json with " + jsonItems.size() + " items");
        } catch (Exception e) {
            UserError.Log.e(TAG, "Failed to write seed file: " + e.getMessage());
        }
    }

    /** Clears cached user items so the next call re-reads from disk. */
    public static void invalidateCache() {
        cachedUserItems = null;
        cachedFileModified = -1;
    }

    private static List<JsonCarbItem> loadUserItems() {
        File file = getFile();
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        long lastMod = file.lastModified();
        if (cachedUserItems != null && lastMod == cachedFileModified) {
            return cachedUserItems;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            Type listType = new TypeToken<List<JsonCarbItem>>() {}.getType();
            List<JsonCarbItem> items = new Gson().fromJson(reader, listType);
            if (items != null) {
                cachedUserItems = items;
                cachedFileModified = lastMod;
                UserError.Log.d(TAG, "Loaded " + items.size() + " user common foods from JSON");
            }
            return items;
        } catch (JsonSyntaxException e) {
            UserError.Log.e(TAG, "Invalid JSON in common_foods.json: " + e.getMessage());
            return null;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error reading common_foods.json: " + e.getMessage());
            return null;
        }
    }

    private static File getFile() {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/xdrip";
        return new File(dir, FILE_NAME);
    }

    private static String overrideKey(String category, String name) {
        return (category + "|" + name).toLowerCase();
    }

    private static StandardCarbCategory parseCategoryLenient(String value) {
        if (value == null) return null;
        try {
            return StandardCarbCategory.valueOf(value.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try matching by label
            for (StandardCarbCategory cat : StandardCarbCategory.values()) {
                if (cat.label().equalsIgnoreCase(value) || cat.name().equalsIgnoreCase(value)) {
                    return cat;
                }
            }
            return null;
        }
    }

    /** JSON model for items in common_foods.json */
    @SuppressWarnings("unused")
    static class JsonCarbItem {
        String category;
        String name;
        double carbsPer100g;
        double smallPortion;
        double mediumPortion;
        double largePortion;
        String source;
        String portionUnit; // "g" or "ml", defaults to "g" if absent
        double gramsPerMl;  // only used when portionUnit is "ml"
    }
}
