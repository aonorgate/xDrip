# CarbLookup Branch – Summary of Changes vs Upstream

This document summarises all differences between the NightscoutFoundation/xDrip origin/master branch and the local CarbCalc branch (which implements the CarbLookup feature). Changes that are NOT functionally part of the CarbLookup feature are explicitly called out at the end.

## 1. CarbLookup Feature – Core Module

The CarbLookup module is an entirely new carbohydrate lookup, meal logging, and recipe management system added under `com.eveningoutpost.dexdrip.carblookup`. It comprises approximately 14,700 new lines of code across ~120 changed files.

### Activities (new)

- **CarbLookupActivity** – main entry point; current meal builder with total carbs, GI/GL estimates, portion logging, and treatment submission.
- **ProductDetailActivity** – displays product nutritional data from barcode scan, manual entry, or Common Foods; portion entry with small/medium/large presets.
- **FoodSearchActivity** – online text search against Open Food Facts (regional source-aware).
- **MealHistoryActivity** – lists saved meals with retention-window filtering (30/90/180 days).
- **MealDetailActivity** – shows meal breakdown; supports copy-to-current-meal and delete-with-treatment-removal.
- **RecipeListActivity** – lists saved recipes/templates.
- **RecipeEditActivity** – create/edit recipe with ingredients from scan, search, Common Foods, or manual entry.
- **RecipeDetailActivity** – view recipe details; add portion to current meal.
- **FavoriteItemsActivity** – manages favourite food items for quick re-use.
- **CarbLookupCaptureActivity** – ZXing barcode camera wrapper with CAMERA permission handling.

### Database (new)

- **CarbLookupDatabase** – standalone SQLiteOpenHelper (CarbLookupDatabase.db, version 10) with tables: saved_meals, saved_meal_items, recipes, recipe_items, scanned_products, favorite_items.
- **RecipeRepository** – full CRUD for recipes and recipe items with ON DELETE CASCADE.
- **MealRepository** – save/query/delete meals with structured items.
- **ProductCacheRepository** – barcode product cache with 90-day TTL and GI override persistence.
- **FavoriteItemRepository** – manage favourite items with default portions.

### API Layer (new)

- **OpenFoodFactsClient** – HTTP client for product barcode lookup and text search, source-aware (US, UK, World, France, Germany).
- **FoodDbSource** – enum defining regional endpoints, labels, and standard-food-region mapping.
- **ProductData / ProductSearchData** – API response models.

### Common Foods (new)

- **StandardCarbRepository** – hardcoded curated food items grouped by StandardCarbCategory (11 categories, 108 items per region).
- **StandardCarbFileLoader** – reads user-customisable `/sdcard/xdrip/common_foods.json`; merges user additions/overrides with built-in defaults; caches by file timestamp.
- **StandardCarbItem** – model with carbs/100g, three portion sizes, unit (g or ml), and grams-per-unit conversion.
- **StandardCarbCategory** – enum with user-facing labels.
- **StandardCarbCategoryAdapter / StandardCarbItemAdapter** – dialog list adapters for the two-step picker.
- **CommonFoodsPicker** – shows grouped category then item selection dialogs.

### Utilities (new)

- **CarbLookupCalculator** – carb arithmetic helpers.
- **GiLookupTable** – estimates glycaemic index from product name keywords.
- **MealLogService** – logs carb treatments to xDrip.
- **CurrentMeal** – in-memory current meal state.
- **MealItemFactory** – creates meal items from various sources.
- **CenteredIconButton** – custom view for action buttons.

### UI Resources (new)

20+ new layout XML files (activities, list item rows), 12 new vector drawables (icons), 237 new string resources (all prefixed `carblookup_`), a new colour entry, a preference arrays resource, and a menu XML for the CarbLookup activity.

### Tests (new)

Comprehensive Robolectric unit tests (17 test classes) covering all activities, repositories, the API client, calculator, GI table, file loader, and current-meal logic. 6 instrumented Android test classes for on-device repository verification.

### Documentation (new)

- `Documentation/technical/Carb_Lookup.md` – maintained implementation notes covering all features, JSON config file format, and test commands.
- `ollama_context_document.md` – AI context overview of the module architecture and schema.
- `ollama_context_supplement.md` – condensed context supplement.

## 2. Integration Points (modifications to existing files)

These changes wire the new module into the existing xDrip app:

- **Home.java** – added a new ImageButton (carbLookupButton) with click handler to launch CarbLookupActivity, and a menu item handler for the same action.
- **activity_home.xml** – added the carbLookupButton ImageButton below the Note button; adjusted Undo button positioning to be below the new button.
- **AndroidManifest.xml** – registered 10 new CarbLookup activities (all `android:exported="false"`).
- **Preferences.java** – bound the `carblookup_food_db_source` ListPreference summary to its value.
- **xdrip_plus_prefs.xml** – added a ListPreference for Food Database Source under the Profile section.
- **menu_home.xml** – added an `action_carb_lookup` menu item.
- **values/strings.xml** – added 237 string resources for the CarbLookup module.
- **values/arrays.xml** – added food database source entries/values arrays.
- **values/colors.xml** – added one colour entry.

## 3. Changes NOT Functionally Part of CarbLookup

The following changes were made on this branch but are not part of the CarbLookup feature itself. They should be reviewed separately if submitting CarbLookup as a standalone PR.

### Build system / Gradle

- **app/build.gradle** – added `androidTestImplementation` dependency on `net.sf.kxml:kxml2:2.3.0` (XML parser library used by instrumented tests but not by CarbLookup runtime code).
- **app/build.gradle** – added a custom Gradle task `runCarblookupFastDebugAndroidTest` with helper functions `resolveAdbExecutable()` and `resolveConnectedDeviceSerial()`. This is a developer tooling / test-runner convenience task, not a feature change.
- **app/build.gradle** – removed trailing newline at end of file (whitespace-only change to final `apply plugin` line).

### Proguard / R8 rules

- **app/proguard-rules.pro** – added 5 `-keep` rules for `carblookup.db.**`, `carblookup.model.**`, `MealSummary`, and `ProductData` classes. These exist solely to prevent R8 from stripping classes needed by instrumented tests in minified debug builds. They are test infrastructure, not runtime feature code.
- **app/proguard-debug.pro** – added `-dontwarn` for `org.kxml2` classes (suppresses warnings from the kxml2 test dependency) and the same 5 `-keep` rules as above.
- **app/proguard-rules.pro** – inherited upstream's new Gson `@Expose` `-if`/`-keepclassmembers` rule (this came from origin/master during the merge and is unrelated to CarbLookup).

### Context / AI documentation files

- **ollama_context_document.md** and **ollama_context_supplement.md** – AI-assistant context documents placed at the repository root. They describe the CarbLookup module architecture for LLM consumption but are not part of the app's runtime or build. They would not typically be included in a feature PR to an upstream project.

## 5. Summary Statistics

| Metric | Value |
|--------|-------|
| Files changed | ~122 (after cleanup) |
| Lines added | ~14,700 |
| Lines removed | ~100 (stale files + old context doc) |
| New Java source files | ~60 (main) + ~23 (test) |
| New resource files | ~40 (layouts, drawables, menus, values) |

---

*Generated: 4 June 2026*
