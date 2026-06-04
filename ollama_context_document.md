# CONTEXT DOCUMENT: xDrip+ Carb Lookup Module
# Version: 2026-06 | Feed this to your local LLM at the start of each session
# Purpose: Pre-loaded reference for the Carb Lookup module in the xDrip+ fork

See `Documentation/technical/Carb_Lookup.md` for maintained implementation notes.

---

## PROJECT IDENTITY

Personal Android app fork of xDrip+ with a carbohydrate lookup and meal logging module.

- **Module package:** `com.eveningoutpost.dexdrip.carblookup`
- **Build system:** Gradle 7.5, AGP 7.4.2
- **Java:** Source/target Java 8. **JAVA_HOME must point to JDK 11** (required by AGP 7.4.2)
- **Min SDK:** API 24 | **Target SDK:** API 34
- **Architecture:** Traditional Activity-based — NO Compose, NO ViewModel, NO LiveData, NO Room, NO coroutines
- **Database:** Standalone `SQLiteOpenHelper` (`CarbLookupDatabase.db`) — NOT ActiveAndroid

---

## MODULE STRUCTURE

```
carblookup/
  CarbLookupActivity.java         — Main screen: current meal builder, total carbs, GI/GL, treatment logging
  ProductDetailActivity.java      — Product nutrition display, portion entry, portion presets
  FoodSearchActivity.java         — Online Open Food Facts text search
  MealHistoryActivity.java        — Saved meals list with retention filtering
  MealDetailActivity.java         — Single meal breakdown, copy, delete
  RecipeListActivity.java         — Saved recipes/templates list
  RecipeEditActivity.java         — Recipe ingredient editor
  RecipeDetailActivity.java       — Recipe detail, add-to-meal
  FavoriteItemsActivity.java      — Favourites manager
  CarbLookupCaptureActivity.java  — ZXing barcode camera wrapper
  CommonFoodsPicker.java          — Two-step grouped food picker dialog
  CurrentMeal.java                — In-memory meal state
  MealItemFactory.java            — Creates MealItems from various sources
  MealLogService.java             — Logs carb treatments to xDrip
  MealSummary.java                — POJO: id, name, totalCarbs, savedAt, notes, mealTime
  CenteredIconButton.java         — Custom view

carblookup/api/
  FoodDbSource.java               — Enum: regional endpoints, labels, standard-food regions
  OpenFoodFactsClient.java        — OkHttp client for barcode + text search
  ProductData.java                — API response model
  ProductSearchData.java          — Search response model

carblookup/db/
  CarbLookupDatabase.java         — SQLiteOpenHelper (version 10), singleton + in-memory factory
  RecipeRepository.java           — Full CRUD for recipes with ON DELETE CASCADE
  MealRepository.java             — Save/query/delete meals
  ProductCacheRepository.java     — Barcode cache with 90-day TTL, GI override
  FavoriteItemRepository.java     — Favourites with default portions

carblookup/model/
  MealItem.java, Recipe.java, RecipeItem.java, RecipeSummary.java,
  PortionResult.java, PortionItemResult.java, FavoriteItem.java

carblookup/standard/
  StandardCarbRepository.java     — Built-in curated foods (108 items × 3 regions)
  StandardCarbFileLoader.java     — User JSON overrides from /sdcard/xdrip/common_foods.json
  StandardCarbItem.java           — Model with carbs/100g, portions, unit conversion
  StandardCarbCategory.java       — 11-category enum
  StandardCarbCategoryAdapter.java, StandardCarbItemAdapter.java

carblookup/utils/
  CarbLookupCalculator.java       — Carb arithmetic
  GiLookupTable.java              — GI estimation from product name keywords
```

---

## DATABASE SCHEMA (CarbLookupDatabase.db, version 10)

```sql
saved_meals (id, name, total_carbs_g, saved_at, treatment_uuid, notes, meal_time)
saved_meal_items (id, meal_id FK, item_order, barcode, product_name, brand,
                  carbs_per_100g, portion_grams, carbs_for_portion, gi_estimate, gl_estimate)
recipes (id, name, portion_count, created_at, updated_at, last_used_at,
         total_carbs_grams, carbs_per_portion)
recipe_items (id, recipe_id FK CASCADE, barcode, product_name, brand,
              carbs_per_100g, item_weight_grams, item_carbs_grams)
scanned_products (barcode PK, product_name, brand, carbs_per_100g, source,
                  fetched_at, gi_override)
favorite_items (id, barcode, product_name, brand, carbs_per_100g,
               default_portion_grams, source, added_at)
```

---

## INTEGRATION POINTS

- `Home.java` — ImageButton + menu item launch `CarbLookupActivity`
- `AndroidManifest.xml` — 10 activities registered (all `exported="false"`)
- `Preferences.java` + `xdrip_plus_prefs.xml` — Food DB source ListPreference
- `activity_home.xml` — carbLookupButton below note button
- `menu_home.xml` — action_carb_lookup menu item

---

## KEY RULES

1. Never add tables to the xDrip+ ActiveAndroid database. CarbLookup uses its own `CarbLookupDatabase.db`.
2. OkHttp callbacks run on background threads — always `runOnUiThread()` for UI updates.
3. `PRAGMA foreign_keys = ON` is set in `CarbLookupDatabase.onOpen()`.
4. `ActivityResultLauncher` must be a class-level field.
5. Call `Home.staticRefreshBgHigh(false)` after saving a treatment.
6. `CarbLookupDatabase` is a singleton — use `getInstance(context)`, never construct directly.
7. User-Agent for OFF requests uses `BuildConfig.VERSION_NAME`.

---

## TESTING

```powershell
# Unit tests (Robolectric)
$javaPath = (Get-Command java).Source; $env:JAVA_HOME = Split-Path (Split-Path $javaPath -Parent) -Parent
.\gradlew.bat :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.carblookup.*" --no-daemon

# Instrumented tests on device/emulator
.\gradlew.bat :app:runCarblookupFastDebugAndroidTest -PandroidSerial=<serial>
```

---

## QUICK-START SESSION TEMPLATE

```
I am extending xDrip+ (Android, Java only) with a carb lookup/meal logging module.
Package: com.eveningoutpost.dexdrip.carblookup
Architecture: Traditional Activity-based. No Room, no ViewModel, no LiveData.
Database: SQLiteOpenHelper (CarbLookupDatabase.java → CarbLookupDatabase.db). NOT ActiveAndroid.
HTTP: OkHttp. JSON: Gson. Barcode: zxing-android-embedded 4.3.0.
See Documentation/technical/Carb_Lookup.md for full implementation notes.

Now help me with: [YOUR TASK HERE]
```
