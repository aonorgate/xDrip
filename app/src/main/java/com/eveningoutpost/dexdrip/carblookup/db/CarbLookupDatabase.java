package com.eveningoutpost.dexdrip.carblookup.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class CarbLookupDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "CarbLookupDatabase.db";
    private static final int DATABASE_VERSION = 10;

    // Singleton instance
    private static volatile CarbLookupDatabase instance;
    private static final Object lock = new Object();

    // In-memory database flag
    private final boolean inMemory;

    private CarbLookupDatabase(Context context, boolean inMemory) {
        super(context, inMemory ? null : DATABASE_NAME, null, DATABASE_VERSION);
        this.inMemory = inMemory;
    }

    public static CarbLookupDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CarbLookupDatabase(context, false);
                }
            }
        }
        return instance;
    }

    /**
     * Creates a standalone in-memory database for testing.
     * Each call returns a new independent instance — safe to call repeatedly in tests.
     */
    public static CarbLookupDatabase createInMemoryInstance(Context context) {
        return new CarbLookupDatabase(context, true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE saved_meals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "total_carbs_g REAL NOT NULL DEFAULT 0, " +
            "saved_at INTEGER NOT NULL, " +
            "treatment_uuid TEXT, " +
            "notes TEXT, " +
            "meal_time TEXT NOT NULL DEFAULT 'any'" +
                ")");

        db.execSQL("CREATE TABLE saved_meal_items (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "meal_id INTEGER NOT NULL, " +
            "item_order INTEGER NOT NULL DEFAULT 0, " +
            "barcode TEXT, " +
            "product_name TEXT NOT NULL, " +
            "brand TEXT, " +
            "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
            "portion_grams REAL NOT NULL, " +
            "carbs_for_portion REAL NOT NULL, " +
            "gi_estimate INTEGER NOT NULL DEFAULT 0, " +
            "gl_estimate REAL NOT NULL DEFAULT 0, " +
            "FOREIGN KEY (meal_id) REFERENCES saved_meals(id) ON DELETE CASCADE" +
            ")");

        db.execSQL("CREATE TABLE recipes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "portion_count INTEGER NOT NULL, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL, " +
                "last_used_at INTEGER, " +
                "total_carbs_grams REAL NOT NULL DEFAULT 0, " +
                "carbs_per_portion REAL NOT NULL DEFAULT 0, " +
                "meal_time TEXT NOT NULL DEFAULT 'any', " +
                "use_count INTEGER NOT NULL DEFAULT 0, " +
                "notes TEXT" +
                ")" );

        db.execSQL("CREATE TABLE recipe_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "recipe_id INTEGER NOT NULL, " +
                "barcode TEXT, " +
                "product_name TEXT NOT NULL, " +
                "brand TEXT, " +
                "carbs_per_100g REAL NOT NULL, " +
                "item_weight_grams REAL NOT NULL, " +
                "item_carbs_grams REAL NOT NULL, " +
                "gi_estimate INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE scanned_products (" +
            "source TEXT NOT NULL DEFAULT 'off_world', " +
            "barcode TEXT NOT NULL, " +
                "product_name TEXT, " +
                "brands TEXT, " +
                "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
                "serving_size TEXT, " +
                "gi_override INTEGER NOT NULL DEFAULT 0, " +
            "fetched_at INTEGER NOT NULL, " +
            "PRIMARY KEY (source, barcode)" +
                ")");

        db.execSQL("CREATE TABLE favorite_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "product_name TEXT NOT NULL, " +
                "brand TEXT, " +
                "barcode TEXT, " +
                "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
                "default_portion_grams REAL NOT NULL DEFAULT 0, " +
                "use_count INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE recipes ADD COLUMN meal_time TEXT NOT NULL DEFAULT 'any'");
            db.execSQL("ALTER TABLE recipes ADD COLUMN use_count INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE recipes ADD COLUMN notes TEXT");
            db.execSQL("ALTER TABLE recipe_items ADD COLUMN gi_estimate INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE saved_meal_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "meal_id INTEGER NOT NULL, " +
                    "item_order INTEGER NOT NULL DEFAULT 0, " +
                    "barcode TEXT, " +
                    "product_name TEXT NOT NULL, " +
                    "brand TEXT, " +
                    "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
                    "portion_grams REAL NOT NULL, " +
                    "carbs_for_portion REAL NOT NULL, " +
                    "gi_estimate INTEGER NOT NULL DEFAULT 0, " +
                    "gl_estimate REAL NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (meal_id) REFERENCES saved_meals(id) ON DELETE CASCADE" +
                    ")");
        }
        if (oldVersion < 5) {
            // Recreate recipe_items with ON DELETE CASCADE
            db.execSQL("CREATE TABLE recipe_items_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "recipe_id INTEGER NOT NULL, " +
                    "barcode TEXT, " +
                    "product_name TEXT NOT NULL, " +
                    "brand TEXT, " +
                    "carbs_per_100g REAL NOT NULL, " +
                    "item_weight_grams REAL NOT NULL, " +
                    "item_carbs_grams REAL NOT NULL, " +
                    "gi_estimate INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE" +
                    ")");
            db.execSQL("INSERT INTO recipe_items_new SELECT * FROM recipe_items");
            db.execSQL("DROP TABLE recipe_items");
            db.execSQL("ALTER TABLE recipe_items_new RENAME TO recipe_items");

            // Barcode cache table
            db.execSQL("CREATE TABLE IF NOT EXISTS scanned_products (" +
                    "barcode TEXT PRIMARY KEY, " +
                    "product_name TEXT, " +
                    "brands TEXT, " +
                    "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
                    "serving_size TEXT, " +
                    "gi_override INTEGER NOT NULL DEFAULT 0, " +
                    "fetched_at INTEGER NOT NULL" +
                    ")");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE saved_meals ADD COLUMN treatment_uuid TEXT");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE saved_meals ADD COLUMN notes TEXT");
        }
        if (oldVersion < 8) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favorite_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "product_name TEXT NOT NULL, " +
                    "brand TEXT, " +
                    "barcode TEXT, " +
                    "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
                    "default_portion_grams REAL NOT NULL DEFAULT 0, " +
                    "use_count INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER NOT NULL" +
                    ")");
        }
                if (oldVersion < 9) {
                    db.execSQL("CREATE TABLE scanned_products_new (" +
                        "source TEXT NOT NULL DEFAULT 'off_world', " +
                        "barcode TEXT NOT NULL, " +
                        "product_name TEXT, " +
                        "brands TEXT, " +
                        "carbs_per_100g REAL NOT NULL DEFAULT 0, " +
                        "serving_size TEXT, " +
                        "gi_override INTEGER NOT NULL DEFAULT 0, " +
                        "fetched_at INTEGER NOT NULL, " +
                        "PRIMARY KEY (source, barcode)" +
                        ")");
                    db.execSQL("INSERT OR REPLACE INTO scanned_products_new " +
                        "(source, barcode, product_name, brands, carbs_per_100g, serving_size, gi_override, fetched_at) " +
                        "SELECT 'off_world', barcode, product_name, brands, carbs_per_100g, serving_size, gi_override, fetched_at " +
                        "FROM scanned_products");
                    db.execSQL("DROP TABLE scanned_products");
                    db.execSQL("ALTER TABLE scanned_products_new RENAME TO scanned_products");
                }
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE saved_meals ADD COLUMN meal_time TEXT NOT NULL DEFAULT 'any'");
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Re-enable foreign keys on every connection open
        db.execSQL("PRAGMA foreign_keys = ON;");
    }
}