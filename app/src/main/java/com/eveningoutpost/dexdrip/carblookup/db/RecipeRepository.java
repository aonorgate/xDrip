package com.eveningoutpost.dexdrip.carblookup.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;
import com.eveningoutpost.dexdrip.carblookup.model.PortionResult;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeRepository {

    private final CarbLookupDatabase dbHelper;

    public RecipeRepository(CarbLookupDatabase dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long saveRecipe(Recipe recipe) {
        validateRecipe(recipe);

        double totalCarbs = 0.0;
        for (RecipeItem item : recipe.items) {
            double rawItemCarbs = item.carbsPer100g * item.itemWeightGrams / 100.0;
            item.itemCarbsGrams = Math.round(rawItemCarbs * 10.0) / 10.0;
            totalCarbs += rawItemCarbs;
        }
        recipe.totalCarbsGrams = Math.round(totalCarbs * 10.0) / 10.0;
        recipe.carbsPerPortion = Math.round((recipe.totalCarbsGrams / recipe.portionCount) * 10.0) / 10.0;

        long now = System.currentTimeMillis();
        recipe.createdAt = now;
        recipe.updatedAt = now;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues recipeValues = new ContentValues();
            recipeValues.put("name", recipe.name);
            recipeValues.put("portion_count", recipe.portionCount);
            recipeValues.put("created_at", recipe.createdAt);
            recipeValues.put("updated_at", recipe.updatedAt);
            recipeValues.put("last_used_at", recipe.lastUsedAt);
            recipeValues.put("total_carbs_grams", recipe.totalCarbsGrams);
            recipeValues.put("carbs_per_portion", recipe.carbsPerPortion);
            recipeValues.put("meal_time", recipe.mealTime != null ? recipe.mealTime : "any");
            recipeValues.put("use_count", 0);
            recipeValues.put("notes", recipe.notes);

            long recipeId = db.insertOrThrow("recipes", null, recipeValues);
            recipe.id = recipeId;

            for (RecipeItem item : recipe.items) {
                ContentValues itemValues = new ContentValues();
                itemValues.put("recipe_id", recipeId);
                itemValues.put("barcode", item.barcode);
                itemValues.put("product_name", item.productName);
                itemValues.put("brand", item.brand);
                itemValues.put("carbs_per_100g", item.carbsPer100g);
                itemValues.put("item_weight_grams", item.itemWeightGrams);
                itemValues.put("item_carbs_grams", item.itemCarbsGrams);
                itemValues.put("gi_estimate", item.giEstimate);
                db.insertOrThrow("recipe_items", null, itemValues);
            }

            db.setTransactionSuccessful();
            return recipeId;
        } finally {
            db.endTransaction();
        }
    }

    public Recipe getRecipeById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("recipes", null, "id = ?",
                new String[]{String.valueOf(id)}, null, null, null);

        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return null;
        }

        Recipe recipe = cursorToRecipe(cursor);
        cursor.close();

        Cursor itemCursor = db.query("recipe_items", null, "recipe_id = ?",
                new String[]{String.valueOf(id)}, null, null, "id ASC");
        recipe.items = new ArrayList<>();
        if (itemCursor != null) {
            while (itemCursor.moveToNext()) {
                recipe.items.add(cursorToRecipeItem(itemCursor));
            }
            itemCursor.close();
        }

        return recipe;
    }

    public void updateRecipe(Recipe recipe) {
        validateRecipe(recipe);

        double totalCarbs = 0.0;
        for (RecipeItem item : recipe.items) {
            double rawItemCarbs = item.carbsPer100g * item.itemWeightGrams / 100.0;
            item.itemCarbsGrams = Math.round(rawItemCarbs * 10.0) / 10.0;
            totalCarbs += rawItemCarbs;
        }
        recipe.totalCarbsGrams = Math.round(totalCarbs * 10.0) / 10.0;
        recipe.carbsPerPortion = Math.round((recipe.totalCarbsGrams / recipe.portionCount) * 10.0) / 10.0;
        recipe.updatedAt = System.currentTimeMillis();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues recipeValues = new ContentValues();
            recipeValues.put("name", recipe.name);
            recipeValues.put("portion_count", recipe.portionCount);
            recipeValues.put("updated_at", recipe.updatedAt);
            recipeValues.put("last_used_at", recipe.lastUsedAt);
            recipeValues.put("total_carbs_grams", recipe.totalCarbsGrams);
            recipeValues.put("carbs_per_portion", recipe.carbsPerPortion);
            recipeValues.put("meal_time", recipe.mealTime != null ? recipe.mealTime : "any");
            recipeValues.put("notes", recipe.notes);

            db.update("recipes", recipeValues, "id = ?", new String[]{String.valueOf(recipe.id)});
            db.delete("recipe_items", "recipe_id = ?", new String[]{String.valueOf(recipe.id)});

            for (RecipeItem item : recipe.items) {
                ContentValues itemValues = new ContentValues();
                itemValues.put("recipe_id", recipe.id);
                itemValues.put("barcode", item.barcode);
                itemValues.put("product_name", item.productName);
                itemValues.put("brand", item.brand);
                itemValues.put("carbs_per_100g", item.carbsPer100g);
                itemValues.put("item_weight_grams", item.itemWeightGrams);
                itemValues.put("item_carbs_grams", item.itemCarbsGrams);
                itemValues.put("gi_estimate", item.giEstimate);
                db.insertOrThrow("recipe_items", null, itemValues);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<RecipeSummary> getAllRecipes() {
        return getAllRecipes("name", null);
    }

    public List<RecipeSummary> getAllRecipes(String sortOrder, String mealTimeFilter) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<RecipeSummary> summaries = new ArrayList<>();

        String orderBy;
        switch (sortOrder != null ? sortOrder : "name") {
            case "recent": orderBy = "r.last_used_at DESC"; break;
            case "most_used": orderBy = "r.use_count DESC"; break;
            default: orderBy = "r.name ASC"; break;
        }

        String whereClause = "";
        String[] args = null;
        if (mealTimeFilter != null && !mealTimeFilter.equals("any")) {
            whereClause = "WHERE r.meal_time = ? OR r.meal_time = 'any' ";
            args = new String[]{mealTimeFilter};
        }

        String sql = "SELECT r.id, r.name, r.portion_count, r.carbs_per_portion, r.total_carbs_grams, "
                + "r.last_used_at, r.meal_time, r.use_count, COUNT(i.id) as item_count "
                + "FROM recipes r LEFT JOIN recipe_items i ON r.id = i.recipe_id "
                + whereClause
                + "GROUP BY r.id ORDER BY " + orderBy;

        Cursor cursor = db.rawQuery(sql, args);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                RecipeSummary summary = new RecipeSummary();
                summary.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                summary.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                summary.portionCount = cursor.getDouble(cursor.getColumnIndexOrThrow("portion_count"));
                summary.carbsPerPortion = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_portion"));
                summary.totalCarbsGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("total_carbs_grams"));
                summary.lastUsedAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_used_at"));
                summary.itemCount = cursor.getInt(cursor.getColumnIndexOrThrow("item_count"));
                summary.mealTime = cursor.getString(cursor.getColumnIndexOrThrow("meal_time"));
                summary.useCount = cursor.getInt(cursor.getColumnIndexOrThrow("use_count"));
                summaries.add(summary);
            }
            cursor.close();
        }
        return summaries;
    }

    public List<RecipeSummary> searchRecipes(String query) {
        return searchRecipes(query, "name", null);
    }

    public List<RecipeSummary> searchRecipes(String query, String sortOrder, String mealTimeFilter) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<RecipeSummary> summaries = new ArrayList<>();
        // Escape LIKE wildcards so searching for literal values such as "100%" or "Low_Carb"
        // does not accidentally broaden the result set.
        String like = "%" + query.replace("%", "\\%").replace("_", "\\_") + "%";
        // Keep ingredient matching in EXISTS so the outer LEFT JOIN still counts every
        // item in the recipe, not only the ingredient rows that matched the search term.
        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.name, r.portion_count, r.carbs_per_portion, r.total_carbs_grams, "
                        + "r.last_used_at, r.meal_time, r.use_count, COUNT(i.id) as item_count "
                        + "FROM recipes r LEFT JOIN recipe_items i ON r.id = i.recipe_id "
                + "WHERE (r.name LIKE ? ESCAPE '\\' "
                + "OR COALESCE(r.notes, '') LIKE ? ESCAPE '\\' "
                + "OR EXISTS (SELECT 1 FROM recipe_items si WHERE si.recipe_id = r.id "
                + "AND (COALESCE(si.product_name, '') LIKE ? ESCAPE '\\' "
                + "OR COALESCE(si.brand, '') LIKE ? ESCAPE '\\' "
                + "OR COALESCE(si.barcode, '') LIKE ? ESCAPE '\\'))) ");
        List<String> args = new ArrayList<>();
        args.add(like);
        args.add(like);
        args.add(like);
        args.add(like);
        args.add(like);
        if (mealTimeFilter != null && !mealTimeFilter.equals("any")) {
            sql.append("AND (r.meal_time = ? OR r.meal_time = 'any') ");
            args.add(mealTimeFilter);
        }
        sql.append("GROUP BY r.id ORDER BY ").append(buildSortOrder(sortOrder));
        Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        if (cursor != null) {
            while (cursor.moveToNext()) {
                RecipeSummary summary = new RecipeSummary();
                summary.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                summary.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                summary.portionCount = cursor.getDouble(cursor.getColumnIndexOrThrow("portion_count"));
                summary.carbsPerPortion = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_portion"));
                summary.totalCarbsGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("total_carbs_grams"));
                summary.lastUsedAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_used_at"));
                summary.itemCount = cursor.getInt(cursor.getColumnIndexOrThrow("item_count"));
                summary.mealTime = cursor.getString(cursor.getColumnIndexOrThrow("meal_time"));
                summary.useCount = cursor.getInt(cursor.getColumnIndexOrThrow("use_count"));
                summaries.add(summary);
            }
            cursor.close();
        }
        return summaries;
    }

    private String buildSortOrder(String sortOrder) {
        String safeSort = sortOrder == null ? "name" : sortOrder.toLowerCase(Locale.US);
        switch (safeSort) {
            case "recent":
                return "r.last_used_at DESC, r.updated_at DESC, r.name ASC";
            case "most_used":
                return "r.use_count DESC, r.name ASC";
            default:
                return "r.name ASC";
        }
    }

    public void incrementUseCount(long recipeId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE recipes SET use_count = use_count + 1, last_used_at = ? WHERE id = ?",
                new Object[]{System.currentTimeMillis(), recipeId});
    }

    public long duplicateRecipe(long sourceId, String newName) {
        Recipe source = getRecipeById(sourceId);
        if (source == null) return -1;
        source.id = 0;
        source.name = newName;
        long now = System.currentTimeMillis();
        source.createdAt = now;
        source.updatedAt = now;
        source.lastUsedAt = 0;
        source.useCount = 0;
        for (RecipeItem item : source.items) {
            item.id = 0;
            item.recipeId = 0;
        }
        return saveRecipe(source);
    }

    public void deleteRecipe(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("recipe_items", "recipe_id = ?", new String[]{String.valueOf(id)});
            db.delete("recipes", "id = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public int getOrphanItemCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM recipe_items WHERE recipe_id NOT IN (SELECT id FROM recipes)", null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public int getRecipeCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM recipes", null);
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }

    public double getCarbsForPortions(long recipeId, double numberOfPortions) {
        if (numberOfPortions <= 0) {
            throw new IllegalArgumentException("Number of portions must be greater than 0");
        }
        Recipe recipe = getRecipeById(recipeId);
        if (recipe == null) {
            return 0.0;
        }
        return Math.round((recipe.totalCarbsGrams * (numberOfPortions / recipe.portionCount)) * 10.0) / 10.0;
    }

    public PortionResult getCarbsBreakdownForPortions(long recipeId, double numberOfPortions) {
        if (numberOfPortions <= 0) {
            throw new IllegalArgumentException("Number of portions must be greater than 0");
        }
        Recipe recipe = getRecipeById(recipeId);
        PortionResult result = new PortionResult();
        result.setRecipeFound(recipe != null);

        if (recipe == null) {
            result.setTotalCarbsGrams(0.0);
            return result;
        }

        result.setRecipeId(recipeId);
        result.setRecipeName(recipe.name);
        result.setNumberOfPortions(numberOfPortions);
        result.setCarbsPerSinglePortion(recipe.carbsPerPortion);

        double factor = numberOfPortions / recipe.portionCount;
        double totalCarbsGrams = 0.0;
        double totalGl = 0.0;
        boolean glIsPartial = false;
        List<PortionItemResult> items = new ArrayList<>();

        for (RecipeItem item : recipe.items) {
            PortionItemResult portionItem = new PortionItemResult();
            portionItem.setProductName(item.productName);
            portionItem.setBarcode(item.barcode);
            portionItem.setBrand(item.brand);
            portionItem.setCarbsPer100g(item.carbsPer100g);
            portionItem.setScaledWeightGrams(item.itemWeightGrams * factor);
            double scaledCarbs = Math.round((item.itemCarbsGrams * factor) * 10.0) / 10.0;
            portionItem.setScaledCarbsGrams(scaledCarbs);
            portionItem.setGiEstimate(item.giEstimate);
            if (item.giEstimate > 0) {
                double itemGl = Math.round((item.giEstimate * scaledCarbs / 100.0) * 10.0) / 10.0;
                portionItem.setScaledGlEstimate(itemGl);
                totalGl += itemGl;
            } else {
                glIsPartial = true;
            }
            items.add(portionItem);
            totalCarbsGrams += item.itemCarbsGrams * factor;
        }

        result.setTotalCarbsGrams(Math.round(totalCarbsGrams * 10.0) / 10.0);
        result.setTotalGlEstimate(Math.round(totalGl * 10.0) / 10.0);
        result.setGlIsPartial(glIsPartial);
        result.setItems(items);
        return result;
    }

    private void validateRecipe(Recipe recipe) {
        if (recipe.name == null || recipe.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipe name cannot be null or blank");
        }
        if (recipe.portionCount <= 0) {
            throw new IllegalArgumentException("Portion count must be greater than 0");
        }
        if (recipe.items == null || recipe.items.isEmpty()) {
            throw new IllegalArgumentException("Recipe must have at least one item");
        }
        for (RecipeItem item : recipe.items) {
            if (item.itemWeightGrams <= 0) {
                throw new IllegalArgumentException("Item weight must be greater than 0");
            }
            if (item.carbsPer100g < 0 || item.carbsPer100g > 100) {
                throw new IllegalArgumentException("Item carbs per 100g must be between 0 and 100");
            }
        }
    }

    private Recipe cursorToRecipe(Cursor cursor) {
        Recipe recipe = new Recipe();
        recipe.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        recipe.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        recipe.portionCount = cursor.getDouble(cursor.getColumnIndexOrThrow("portion_count"));
        recipe.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        recipe.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));
        recipe.lastUsedAt = cursor.getLong(cursor.getColumnIndexOrThrow("last_used_at"));
        recipe.totalCarbsGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("total_carbs_grams"));
        recipe.carbsPerPortion = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_portion"));
        int mealTimeIdx = cursor.getColumnIndex("meal_time");
        recipe.mealTime = (mealTimeIdx != -1 && !cursor.isNull(mealTimeIdx)) ? cursor.getString(mealTimeIdx) : "any";
        int useCountIdx = cursor.getColumnIndex("use_count");
        recipe.useCount = (useCountIdx != -1) ? cursor.getInt(useCountIdx) : 0;
        int notesIdx = cursor.getColumnIndex("notes");
        recipe.notes = (notesIdx != -1 && !cursor.isNull(notesIdx)) ? cursor.getString(notesIdx) : null;
        return recipe;
    }

    private RecipeItem cursorToRecipeItem(Cursor cursor) {
        RecipeItem item = new RecipeItem();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.recipeId = cursor.getLong(cursor.getColumnIndexOrThrow("recipe_id"));
        int barcodeIdx = cursor.getColumnIndex("barcode");
        item.barcode = (barcodeIdx != -1 && !cursor.isNull(barcodeIdx)) ? cursor.getString(barcodeIdx) : null;
        item.productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
        int brandIdx = cursor.getColumnIndex("brand");
        item.brand = (brandIdx != -1 && !cursor.isNull(brandIdx)) ? cursor.getString(brandIdx) : null;
        item.carbsPer100g = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_100g"));
        item.itemWeightGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("item_weight_grams"));
        item.itemCarbsGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("item_carbs_grams"));
        int giIdx = cursor.getColumnIndex("gi_estimate");
        item.giEstimate = (giIdx != -1) ? cursor.getInt(giIdx) : 0;
        return item;
    }
}
