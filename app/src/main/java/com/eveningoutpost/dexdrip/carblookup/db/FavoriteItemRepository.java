package com.eveningoutpost.dexdrip.carblookup.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;

import java.util.ArrayList;
import java.util.List;

public class FavoriteItemRepository {
    private final CarbLookupDatabase db;

    public FavoriteItemRepository(Context context) {
        db = CarbLookupDatabase.getInstance(context);
    }

    public FavoriteItemRepository(CarbLookupDatabase database) {
        db = database;
    }

    public List<FavoriteItem> getAll() {
        List<FavoriteItem> items = new ArrayList<>();
        SQLiteDatabase database = db.getReadableDatabase();
        Cursor cursor = database.query("favorite_items", null, null, null, null, null,
                "use_count DESC, product_name ASC");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                items.add(cursorToFavoriteItem(cursor));
            }
            cursor.close();
        }
        return items;
    }

    public long save(FavoriteItem item) {
        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_name", item.productName);
        values.put("brand", item.brand);
        values.put("barcode", item.barcode);
        values.put("carbs_per_100g", item.carbsPer100g);
        values.put("default_portion_grams", item.defaultPortionGrams);
        values.put("use_count", 0);
        values.put("created_at", System.currentTimeMillis());
        return database.insertOrThrow("favorite_items", null, values);
    }

    public void update(FavoriteItem item) {
        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("product_name", item.productName);
        values.put("brand", item.brand);
        values.put("barcode", item.barcode);
        values.put("carbs_per_100g", item.carbsPer100g);
        values.put("default_portion_grams", item.defaultPortionGrams);
        database.update("favorite_items", values, "id = ?", new String[]{String.valueOf(item.id)});
    }

    public void incrementUseCount(long id) {
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("UPDATE favorite_items SET use_count = use_count + 1 WHERE id = ?",
                new Object[]{id});
    }

    public void resetUseCount(long id) {
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("UPDATE favorite_items SET use_count = 0 WHERE id = ?", new Object[]{id});
    }

    public void delete(long id) {
        SQLiteDatabase database = db.getWritableDatabase();
        database.delete("favorite_items", "id = ?", new String[]{String.valueOf(id)});
    }

    private FavoriteItem cursorToFavoriteItem(Cursor cursor) {
        FavoriteItem item = new FavoriteItem();
        item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        item.productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
        int brandIdx = cursor.getColumnIndex("brand");
        item.brand = brandIdx != -1 && !cursor.isNull(brandIdx) ? cursor.getString(brandIdx) : null;
        int barcodeIdx = cursor.getColumnIndex("barcode");
        item.barcode = barcodeIdx != -1 && !cursor.isNull(barcodeIdx) ? cursor.getString(barcodeIdx) : null;
        item.carbsPer100g = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_100g"));
        item.defaultPortionGrams = cursor.getDouble(cursor.getColumnIndexOrThrow("default_portion_grams"));
        item.useCount = cursor.getInt(cursor.getColumnIndexOrThrow("use_count"));
        item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
        return item;
    }
}
