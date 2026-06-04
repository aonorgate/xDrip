package com.eveningoutpost.dexdrip.carblookup.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eveningoutpost.dexdrip.carblookup.api.ProductData;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;

public class ProductCacheRepository {

    private static final long TTL_MILLIS = 90L * 24 * 60 * 60 * 1000; // 90 days

    private final CarbLookupDatabase dbHelper;

    public ProductCacheRepository(CarbLookupDatabase dbHelper) {
        this.dbHelper = dbHelper;
    }

    public CachedProduct lookup(String barcode) {
        return lookup(FoodDbSource.DEFAULT_KEY, barcode);
    }

    public CachedProduct lookup(String source, String barcode) {
        if (barcode == null || barcode.isEmpty()) return null;
        String normalizedSource = normalizeSource(source);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("scanned_products", null, "source = ? AND barcode = ?",
                new String[]{normalizedSource, barcode}, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return null;
        }
        long fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetched_at"));
        if (System.currentTimeMillis() - fetchedAt > TTL_MILLIS) {
            cursor.close();
            db.delete("scanned_products", "source = ? AND barcode = ?", new String[]{normalizedSource, barcode});
            return null;
        }
        CachedProduct cached = new CachedProduct();
        cached.source = normalizedSource;
        cached.barcode = barcode;
        cached.productName = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
        cached.brands = cursor.getString(cursor.getColumnIndexOrThrow("brands"));
        cached.carbsPer100g = cursor.getDouble(cursor.getColumnIndexOrThrow("carbs_per_100g"));
        cached.servingSize = cursor.getString(cursor.getColumnIndexOrThrow("serving_size"));
        cached.giOverride = cursor.getInt(cursor.getColumnIndexOrThrow("gi_override"));
        cached.fetchedAt = fetchedAt;
        cursor.close();
        return cached;
    }

    public void store(String barcode, ProductData.Product product) {
        store(FoodDbSource.DEFAULT_KEY, barcode, product);
    }

    public void store(String source, String barcode, ProductData.Product product) {
        if (barcode == null || barcode.isEmpty() || product == null) return;
        String normalizedSource = normalizeSource(source);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int existingGiOverride = getStoredGiOverride(db, normalizedSource, barcode);
            ContentValues cv = new ContentValues();
            cv.put("source", normalizedSource);
            cv.put("barcode", barcode);
            cv.put("product_name", product.productName);
            cv.put("brands", product.brands);
            cv.put("carbs_per_100g", product.nutriments != null ? product.nutriments.carbohydrates100g : 0.0);
            cv.put("serving_size", product.servingSize);
            cv.put("gi_override", existingGiOverride);
            cv.put("fetched_at", System.currentTimeMillis());
            db.insertWithOnConflict("scanned_products", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateGiOverride(String barcode, int giValue) {
        updateGiOverride(FoodDbSource.DEFAULT_KEY, barcode, giValue);
    }

    public void updateGiOverride(String source, String barcode, int giValue) {
        if (barcode == null || barcode.isEmpty()) return;
        String normalizedSource = normalizeSource(source);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("gi_override", giValue);
        db.update("scanned_products", cv, "source = ? AND barcode = ?", new String[]{normalizedSource, barcode});
    }

    public int getGiOverride(String barcode) {
        return getGiOverride(FoodDbSource.DEFAULT_KEY, barcode);
    }

    public int getGiOverride(String source, String barcode) {
        if (barcode == null || barcode.isEmpty()) return 0;
        String normalizedSource = normalizeSource(source);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return getStoredGiOverride(db, normalizedSource, barcode);
    }

    private int getStoredGiOverride(SQLiteDatabase db, String source, String barcode) {
        Cursor cursor = db.query("scanned_products", new String[]{"gi_override"},
                "source = ? AND barcode = ?", new String[]{source, barcode}, null, null, null);
        int gi = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                gi = cursor.getInt(0);
            }
            cursor.close();
        }
        return gi;
    }

    private String normalizeSource(String source) {
        return source == null || source.trim().isEmpty() ? FoodDbSource.DEFAULT_KEY : source;
    }

    public static class CachedProduct {
        public String source;
        public String barcode;
        public String productName;
        public String brands;
        public double carbsPer100g;
        public String servingSize;
        public int giOverride;
        public long fetchedAt;
    }
}
