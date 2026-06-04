package com.eveningoutpost.dexdrip.carblookup.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.eveningoutpost.dexdrip.carblookup.MealSummary;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import java.util.ArrayList;
import java.util.List;

public class MealRepository {
    private final CarbLookupDatabase db;

    public MealRepository(Context context) {
        db = CarbLookupDatabase.getInstance(context);
    }

    /** Constructor for testing with a provided database instance. */
    public MealRepository(CarbLookupDatabase database) {
        db = database;
    }

    public List<MealSummary> getAllMeals() {
        return getMealSummaries(null, null);
    }

    public List<MealSummary> getMealsSince(long earliestSavedAt) {
        return getMealSummaries("m.saved_at >= ?", new String[]{String.valueOf(earliestSavedAt)});
    }

    private List<MealSummary> getMealSummaries(String whereClause, String[] whereArgs) {
        List<MealSummary> meals = new ArrayList<>();
        SQLiteDatabase database = db.getReadableDatabase();
        String sql = "SELECT m.id, m.name, m.total_carbs_g, m.saved_at, m.treatment_uuid, " +
                "m.notes, m.meal_time, COUNT(i.id) AS item_count " +
                "FROM saved_meals m " +
                "LEFT JOIN saved_meal_items i ON m.id = i.meal_id " +
                (whereClause != null ? "WHERE " + whereClause + " " : "") +
                "GROUP BY m.id " +
                "ORDER BY m.saved_at DESC";
        Cursor cursor = database.rawQuery(sql, whereArgs);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                meals.add(cursorToMealSummary(cursor));
            }
            cursor.close();
        }
        return meals;
    }

    public MealSummary getMealById(long mealId) {
        SQLiteDatabase database = db.getReadableDatabase();
        Cursor mealCursor = database.query("saved_meals", null, "id = ?",
                new String[]{String.valueOf(mealId)}, null, null, null);
        if (mealCursor == null || !mealCursor.moveToFirst()) {
            if (mealCursor != null) {
                mealCursor.close();
            }
            return null;
        }

        MealSummary meal = cursorToMealSummary(mealCursor);
        mealCursor.close();

        Cursor itemCursor = database.query("saved_meal_items", null, "meal_id = ?",
                new String[]{String.valueOf(mealId)}, null, null, "item_order ASC, id ASC");
        if (itemCursor != null) {
            while (itemCursor.moveToNext()) {
                meal.items.add(cursorToMealItem(itemCursor));
            }
            meal.itemCount = meal.items.size();
            itemCursor.close();
        }
        return meal;
    }

    public long saveMeal(final MealSummary meal) {
        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("name", meal.name);
            values.put("total_carbs_g", meal.totalCarbs);
            values.put("saved_at", meal.savedAt > 0 ? meal.savedAt : System.currentTimeMillis());
            values.put("treatment_uuid", meal.treatmentUuid);
            values.put("notes", meal.notes);
            values.put("meal_time", normalizeMealTime(meal.mealTime));
            long mealId = database.insertOrThrow("saved_meals", null, values);
            meal.id = mealId;

            if (meal.items != null) {
                for (int i = 0; i < meal.items.size(); i++) {
                    MealItem item = meal.items.get(i);
                    ContentValues itemValues = new ContentValues();
                    itemValues.put("meal_id", mealId);
                    itemValues.put("item_order", i);
                    itemValues.put("barcode", item.barcode);
                    itemValues.put("product_name", item.productName);
                    itemValues.put("brand", item.brand);
                    itemValues.put("carbs_per_100g", item.carbsPer100g);
                    itemValues.put("portion_grams", item.portionGrams);
                    itemValues.put("carbs_for_portion", item.carbsForPortion);
                    itemValues.put("gi_estimate", item.giEstimate);
                    itemValues.put("gl_estimate", item.glEstimate);
                    database.insertOrThrow("saved_meal_items", null, itemValues);
                }
                meal.itemCount = meal.items.size();
            }

            database.setTransactionSuccessful();
            return mealId;
        } finally {
            database.endTransaction();
        }
    }

    public void deleteMeal(long mealId) {
        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();
        try {
            database.delete("saved_meal_items", "meal_id = ?", new String[]{String.valueOf(mealId)});
            database.delete("saved_meals", "id = ?", new String[]{String.valueOf(mealId)});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private MealSummary cursorToMealSummary(Cursor cursor) {
        MealSummary meal = new MealSummary();
        meal.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        meal.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        meal.totalCarbs = cursor.getDouble(cursor.getColumnIndexOrThrow("total_carbs_g"));
        meal.savedAt = cursor.getLong(cursor.getColumnIndexOrThrow("saved_at"));
        int treatmentUuidIndex = cursor.getColumnIndex("treatment_uuid");
        meal.treatmentUuid = treatmentUuidIndex != -1 && !cursor.isNull(treatmentUuidIndex)
            ? cursor.getString(treatmentUuidIndex) : null;
        int notesIndex = cursor.getColumnIndex("notes");
        meal.notes = notesIndex != -1 && !cursor.isNull(notesIndex)
            ? cursor.getString(notesIndex) : null;
        int mealTimeIndex = cursor.getColumnIndex("meal_time");
        meal.mealTime = mealTimeIndex != -1 && !cursor.isNull(mealTimeIndex)
            ? cursor.getString(mealTimeIndex) : "any";
        int itemCountIndex = cursor.getColumnIndex("item_count");
        if (itemCountIndex != -1) {
            meal.itemCount = cursor.getInt(itemCountIndex);
        }
        return meal;
    }

    private String normalizeMealTime(String mealTime) {
        return mealTime == null || mealTime.trim().isEmpty() ? "any" : mealTime;
    }

    private MealItem cursorToMealItem(Cursor cursor) {
        MealItem item = new MealItem();
        int barcodeIdx = cursor.getColumnIndex("barcode");
        item.barcode = barcodeIdx != -1 && !cursor.isNull(barcodeIdx)
                ? cursor.getString(barcodeIdx) : null;
        item.productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
        int brandIdx = cursor.getColumnIndex("brand");
        item.brand = brandIdx != -1 && !cursor.isNull(brandIdx)
                ? cursor.getString(brandIdx) : null;
        item.carbsPer100g = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_100g"));
        item.portionGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("portion_grams"));
        item.carbsForPortion = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_for_portion"));
        int giIdx = cursor.getColumnIndex("gi_estimate");
        item.giEstimate = giIdx != -1 ? cursor.getInt(giIdx) : 0;
        int glIdx = cursor.getColumnIndex("gl_estimate");
        item.glEstimate = glIdx != -1 ? cursor.getDouble(glIdx) : 0.0;
        return item;
    }
}
