# CONTEXT DOCUMENT: xDrip+ Carb Lookup Development
# Version: 2026-05 | Feed this to your local LLM at the start of each session
# Purpose: Pre-loaded reference facts for the Carb Lookup module in the xDrip+ fork

> Current implementation note: the module now lives under `com.eveningoutpost.dexdrip.carblookup`, not `carbcalc`. Barcode lookup and Item Search are source-aware through `FoodDbSource`, and the home page includes grouped regional Common Foods from `StandardCarbRepository`. See `Documentation/technical/Carb_Lookup.md` for the maintained implementation notes.

---

## SECTION 1: PROJECT IDENTITY

This is a personal Android app project. The goal is to fork xDrip+ and add a carbohydrate lookup and calculation module. All new code is written in **Java** (not Kotlin). This is a personal-use fork — not for public distribution.

- **Root package:** `com.eveningoutpost.dexdrip`
- **Build system:** Gradle (wrapper version 7.5, AGP 7.4.2)
- **Java:** Source/target compatibility Java 8. **JAVA_HOME must point to JDK 11** (required by AGP 7.4.2)
- **Min SDK:** API 24 (Android 7.0) | **Target SDK:** API 34
- **Architecture:** Traditional Activity-based Android — NO Jetpack Compose, NO ViewModel, NO LiveData, NO Room, NO coroutines
- **ORM in xDrip+:** ActiveAndroid (NOT SugarORM). The Carb Lookup module does NOT use ActiveAndroid — it uses a separate `SQLiteOpenHelper`

---

## SECTION 2: xDrip+ CODEBASE KEY FACTS

### Key existing classes (do not modify unless instructed)
```
com.eveningoutpost.dexdrip/
  Home.java                    ← Main Activity — add menu item here for entry point
  models/
    Treatments.java            ← Carbs + insulin treatment (ActiveAndroid model)
    BgReading.java             ← Sensor glucose readings
  utils/
    PersistentStore.java       ← Key-value persistent store
```

### How to save a carb entry to xDrip+ and refresh the graph
```java
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.Home;

Treatments treatment = new Treatments();
treatment.carbs     = totalCarbsGrams;   // double
treatment.insulin   = 0.0;
treatment.timestamp = System.currentTimeMillis();
treatment.notes     = "Meal: " + mealName;
treatment.save();                         // ActiveAndroid save()

Home.staticRefreshBgHigh(false);          // redraws the BG graph
```

### How to add the entry point to the overflow menu
```java
// Step 1: app/src/main/res/menu/  — add to the existing home overflow menu XML:
<item
    android:id="@+id/action_carb_calc"
    android:title="Carb Calculator"
    app:showAsAction="never" />

// Step 2: In Home.java → onOptionsItemSelected():
if (item.getItemId() == R.id.action_carb_calc) {
    startActivity(new Intent(this,
        com.eveningoutpost.dexdrip.carblookup.CarbLookupActivity.class));
    return true;
}
```

### Database rule — CRITICAL
**Never add tables to the xDrip+ ActiveAndroid database.** The Carb Lookup module uses its own separate `CarbLookupDatabase.java` (SQLiteOpenHelper) writing to `CarbLookupDatabase.db`.

---

## SECTION 3: WHAT HAS ALREADY BEEN BUILT (DO NOT RECREATE)

The following files exist and **compile successfully**. Do not recreate them — only extend or call them.

### POJOs / Models
```
carbcalc/MealSummary.java          — plain POJO: id, name, totalCarbs, savedAt
carbcalc/model/MealItem.java       — productName, brand, barcode, carbsPer100g,
                                     portionGrams, carbsForPortion
carbcalc/model/Recipe.java         — id, name, portionCount, createdAt, updatedAt,
                                     lastUsedAt, totalCarbsGrams, carbsPerPortion,
                                     List<RecipeItem> items
carbcalc/model/RecipeItem.java     — id, recipeId, barcode, productName, brand,
                                     carbsPer100g, itemWeightGrams, itemCarbsGrams
carbcalc/model/RecipeSummary.java  — id, name, portionCount, carbsPerPortion,
                                     totalCarbsGrams, itemCount, lastUsedAt
carbcalc/model/PortionResult.java  — recipeId, recipeName, numberOfPortions,
                                     totalCarbsGrams, carbsPerSinglePortion,
                                     List<PortionItemResult> items, recipeFound
carbcalc/model/PortionItemResult.java — productName, barcode, scaledWeightGrams,
                                        scaledCarbsGrams (with getters/setters)
```

### Database layer
```
carbcalc/db/CarbCalcDatabase.java   — SQLiteOpenHelper (CarbCalcDatabase.db)
                                      Tables: saved_meals, recipes, recipe_items
                                      Thread-safe singleton + createInMemoryInstance()
carbcalc/db/MealRepository.java     — getAllMeals(), saveMeal(MealSummary)
                                      Uses CarbCalcDatabase, background thread for writes
carbcalc/db/RecipeRepository.java   — Full CRUD for Recipe objects:
                                        saveRecipe(Recipe) → long id
                                        getRecipeById(long id) → Recipe
                                        updateRecipe(Recipe)
                                        getAllRecipes() → List<RecipeSummary>
                                        deleteRecipe(long id)
                                        getCarbsForPortions(long recipeId, double portions) → double
                                        getCarbsBreakdownForPortions(long recipeId, double portions) → PortionResult
                                        getOrphanItemCount() → int (test helper)
                                      Validates input, calculates carbs on save, uses transactions
```

### Database schema (CarbCalcDatabase.db)
```sql
CREATE TABLE saved_meals (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT NOT NULL,
    total_carbs_g REAL NOT NULL DEFAULT 0,
    saved_at     INTEGER NOT NULL
);

CREATE TABLE recipes (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT NOT NULL,
    portion_count    INTEGER NOT NULL,
    created_at       INTEGER NOT NULL,
    updated_at       INTEGER NOT NULL,
    last_used_at     INTEGER,
    total_carbs_grams REAL NOT NULL DEFAULT 0,
    carbs_per_portion REAL NOT NULL DEFAULT 0
);

CREATE TABLE recipe_items (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id        INTEGER NOT NULL,
    barcode          TEXT,
    product_name     TEXT NOT NULL,
    brand            TEXT,
    carbs_per_100g   REAL NOT NULL,
    item_weight_grams REAL NOT NULL,
    item_carbs_grams REAL NOT NULL,
    FOREIGN KEY (recipe_id) REFERENCES recipes(id)
);
```

### Utilities
```
carbcalc/utils/CarbCalculator.java
  — calculateCarbs(double carbsPer100g, double portionGrams) → double
    (rounds to 1 dp; throws IllegalArgumentException if portionGrams <= 0 or carbsPer100g < 0)
  — totalMealCarbs(List<MealItem> items) → double
```

### Activities (shells — functional but incomplete)
```
carbcalc/CarbCalcActivity.java
  — 3 buttons: Scan Barcode, Save Meal, View History
  — barcodeLauncher uses registerForActivityResult(new ScanContract(), ...) [correct modern API]
  — TODO: wire up ProductDetailActivity after scan
  — TODO: wire up real save to MealRepository

carbcalc/MealHistoryActivity.java
  — loads meals from MealRepository, displays with MealSummaryAdapter
  — fully functional for displaying saved meals

carbcalc/MealSummaryAdapter.java
  — ArrayAdapter<MealSummary> using item_meal_row.xml layout
  — shows meal name + totalCarbs
```

### Layouts (all in app/src/main/res/layout/)
```
activity_carb_calc.xml    — LinearLayout with scanButton, saveMealButton,
                            viewHistoryButton, carbsResultTextView
activity_meal_history.xml — LinearLayout with titleTextView, mealListView (ListView)
item_meal_row.xml         — horizontal row: mealNameTextView, mealCarbsTextView
```

### Stub files (kept for structural compatibility — no functionality)
```
carbcalc/db/MealDatabase.java   — empty stub class, no longer used
carbcalc/db/MealDao.java        — plain interface (no annotations), no longer used
```

---

## SECTION 4: OPEN FOOD FACTS API REFERENCE

Barcode lookups are source-aware. Use `FoodDbSource.current()` to choose the Open Food Facts endpoint and `FoodDbSource.StandardFoodRegion` to choose the matching Common Foods portion set.

### Endpoints
```
World:       https://world.openfoodfacts.org
US:          https://us.openfoodfacts.org
UK:          https://uk.openfoodfacts.org
France:      https://fr.openfoodfacts.org
Germany:     https://de.openfoodfacts.org
Staging:     https://world.openfoodfacts.net  (use for tests/dev)

GET product: {selected-source}/api/v2/product/{barcode}.json
             ?fields=product_name,brands,nutriments,serving_size,quantity
```
- **No API key** required for reads
- **User-Agent required:** `xDrip-CarbCalc/1.0 (personal-use)`
- `status: 1` = found | `status: 0` = not found (still HTTP 200) | HTTP 503 = rate limited

### Response structure
```json
{
  "status": 1,
  "product": {
    "product_name": "Weetabix",
    "brands": "Weetabix",
    "quantity": "430g",
    "serving_size": "37.5g (2 biscuits)",
    "nutriments": {
      "carbohydrates_100g": 68.4,
      "sugars_100g": 4.4,
      "energy-kcal_100g": 362,
      "fat_100g": 2.0,
      "proteins_100g": 12.0,
      "fiber_100g": 10.0
    }
  }
}
```

### Gson POJO gotchas
- `energy-kcal_100g` has a **hyphen** — must use `@SerializedName("energy-kcal_100g")`
- `carbohydrates_100g` can be missing or 0 — always null-check before use
- `fiber_100g` is frequently null — handle gracefully
- All nutriment numbers can be int or double — parse as `double` always

### OkHttp implementation pattern (OkHttp is already in xDrip+ — do NOT add it as a new dependency)
```java
private final OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build();

public void fetchByBarcode(String barcode, ProductCallback callback) {
    String url = "https://world.openfoodfacts.org/api/v2/product/"
               + barcode + ".json?fields=product_name,brands,nutriments,serving_size,quantity";
    Request request = new Request.Builder()
        .url(url)
        .header("User-Agent", "xDrip-CarbCalc/1.0 (personal-use)")
        .build();
    client.newCall(request).enqueue(new Callback() {
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            // OkHttp callbacks run on a BACKGROUND thread
            // Always wrap UI updates: runOnUiThread(() -> { ... })
        }
        @Override
        public void onFailure(Call call, IOException e) {
            callback.onError(e.getMessage());
        }
    });
}
```

### Test barcodes (UK products, known-good in Open Food Facts)
| Product | Barcode | carbohydrates_100g |
|---|---|---|
| Weetabix | 5000169105306 | 68.4 |
| Heinz Baked Beans 415g | 5000157024466 | ~12.5 |

---

## SECTION 5: BARCODE SCANNER (ZXING)

Library `com.journeyapps:zxing-android-embedded:4.3.0` is **already in app/build.gradle**.

### Key rules
- `ActivityResultLauncher` **must be a class-level field** — never inside a method or lifecycle callback
- Use `ScanOptions.PRODUCT_CODE_TYPES` (EAN-13, EAN-8, UPC-A, UPC-E)
- `result.getContents()` returns the barcode string, or `null` if cancelled

### Already implemented in CarbCalcActivity.java
```java
private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
    registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            String barcode = result.getContents();
            // TODO: launch ProductDetailActivity with barcode
        }
    });
```

### AndroidManifest.xml — ZXing CaptureActivity declaration (NOT YET ADDED)
```xml
<activity
    android:name="com.journeyapps.barcodescanner.CaptureActivity"
    android:screenOrientation="fullSensor"
    tools:replace="screenOrientation"
    android:exported="false" />
```

---

## SECTION 6: ANDROIDDMANIFEST.XML — FULL BLOCK TO ADD

The following have **NOT yet been added** to `app/src/main/AndroidManifest.xml`. Add the activity declarations inside `<application>`, and the permissions in the `<manifest>` block:

```xml
<!-- Inside <application> -->
<activity
    android:name=".carbcalc.CarbCalcActivity"
    android:label="Carb Calculator"
    android:theme="@style/AppTheme"
    android:exported="false" />

<activity
    android:name=".carbcalc.ProductDetailActivity"
    android:label="Product Details"
    android:theme="@style/AppTheme"
    android:exported="false"
    android:windowSoftInputMode="adjustResize" />

<activity
    android:name=".carbcalc.MealHistoryActivity"
    android:label="Saved Meals"
    android:theme="@style/AppTheme"
    android:exported="false" />

<activity
    android:name="com.journeyapps.barcodescanner.CaptureActivity"
    android:screenOrientation="fullSensor"
    tools:replace="screenOrientation"
    android:exported="false" />

<!-- In <manifest> block (INTERNET likely already present — check first) -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## SECTION 7: EXISTING GRADLE DEPENDENCIES (DO NOT DUPLICATE)

These are already in `app/build.gradle`:
- `com.squareup.okhttp3:okhttp:3.12.13` — use for all HTTP
- `com.google.code.gson:gson:2.8.6` — use for JSON parsing
- `com.journeyapps:zxing-android-embedded:4.3.0` — barcode scanner
- `androidx.appcompat:appcompat:1.0.0`
- `com.google.android.material:material:1.1.0`
- `androidx.recyclerview:recyclerview:1.0.0`

Do NOT add Room, Retrofit, Coroutines, or any Jetpack component.

---

## SECTION 8: COMMON GOTCHAS

1. **OkHttp callbacks run on a background thread.** Always wrap UI updates: `runOnUiThread(() -> { ... })`
2. **PRAGMA foreign_keys** — SQLite disables FK by default. Already enabled in `CarbCalcDatabase.onOpen()` — no action needed but don't remove it.
3. **`ActivityResultLauncher` must be a class-level field** — throws `IllegalStateException` if declared inside a method.
4. **`carbohydrates_100g`** can be null/missing even when `status=1` — always null-check before using.
5. **`energy-kcal_100g`** has a hyphen — requires `@SerializedName` in Gson POJO.
6. **`Home.staticRefreshBgHigh(false)`** — call this after every `treatment.save()` to refresh the BG graph.
7. **`android:hardwareAccelerated="true"`** must be set on `<application>` in manifest for ZXing to work — check it is already present before scanning.
8. **CarbCalcDatabase is a singleton** — always obtain via `CarbCalcDatabase.getInstance(context)`, never `new CarbCalcDatabase(...)`.

---

## SECTION 9: QUICK-START SESSION TEMPLATE

Copy this block at the start of each chat session with the LLM:

```
PROJECT CONTEXT:

I am extending xDrip+ (Android, Java only — NOT Kotlin) with a carb calculator module.
Package: com.eveningoutpost.dexdrip.carbcalc
Architecture: Traditional Activity-based. No Room, no ViewModel, no LiveData, no Jetpack Compose.
Database: Plain SQLiteOpenHelper (CarbCalcDatabase.java → CarbCalcDatabase.db). NOT ActiveAndroid.
HTTP: OkHttp (already in project — do not add). JSON: Gson.
Barcode scanner: zxing-android-embedded 4.3.0.
ActivityResultLauncher for scanner is already set up in CarbCalcActivity.java as a class field.
RecipeRepository and MealRepository already exist and compile.
CarbCalcDatabase schema: saved_meals, recipes, recipe_items — see context doc for full DDL.

Now help me with: [YOUR TASK HERE]
```
