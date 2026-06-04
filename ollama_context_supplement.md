# SUPPLEMENT DOCUMENT: xDrip+ Carb Lookup Current Notes

# Version: 2026-05 | Append to primary context doc at session start

This supplement reflects the current `com.eveningoutpost.dexdrip.carblookup` implementation. Older `carbcalc` scaffolding has been removed from this file so future work does not follow obsolete package names or build steps.

---

## Current Implementation

- Package: `com.eveningoutpost.dexdrip.carblookup`
- Architecture: traditional Java Activities, XML layouts, `SQLiteOpenHelper`
- Database: `CarbLookupDatabase`, separate from xDrip ActiveAndroid tables
- Calculator utility: `CarbLookupCalculator`
- Barcode API: `OpenFoodFactsClient` with source selection through `FoodDbSource`
- Item Search: home-page typed online search through `FoodSearchActivity`
- Common Foods: home-page grouped, region-aware picker backed by `StandardCarbRepository`; user-customisable via `/sdcard/xdrip/common_foods.json` through `StandardCarbFileLoader`

See `Documentation/technical/Carb_Lookup.md` for maintained implementation guidance.

---

## Source-Aware Barcode Lookup

`FoodDbSource` owns the user preference key, display label, Open Food Facts base URL, and standard-food region mapping.

Supported sources:

- `WORLD`: `https://world.openfoodfacts.org/api/v2/product/`, European-style common-food portions
- `US`: `https://us.openfoodfacts.org/api/v2/product/`, US common-food portions
- `UK`: `https://uk.openfoodfacts.org/api/v2/product/`, UK common-food portions
- `FRANCE`: `https://fr.openfoodfacts.org/api/v2/product/`, European-style common-food portions
- `GERMANY`: `https://de.openfoodfacts.org/api/v2/product/`, European-style common-food portions

The scanned product cache is keyed by both source and barcode. Keep all cache lookup, store, and GI override calls source-scoped.

---

## Common Foods Picker

Common Foods is shown from the `CarbLookupActivity` home page. The first dialog lists `StandardCarbCategory` groups with counts using `StandardCarbCategoryAdapter`. Selecting a group opens a filtered second dialog rendered by `StandardCarbItemAdapter`; the second dialog includes a Back button to return to the group list.

Selecting a `StandardCarbItem` launches `ProductDetailActivity` with:

- Product name
- Carbs per 100g
- Source label
- Default medium portion
- Small, medium, and large portion buttons

When extending the list, keep item names distinct per region, portion sizes ordered small to large, and every item assigned to a useful group.

---

## Item Search

Item Search is shown from the `CarbLookupActivity` home page beside Common Foods. `FoodSearchActivity` searches the selected Open Food Facts regional host as the user types. Selecting a result returns its barcode to `CarbLookupActivity`, which then launches the normal `ProductDetailActivity` barcode flow.

Keep Item Search barcode-based so product details, cache scoping, nutrition warnings, and portion entry remain centralized in Product Detail.

Search starts after at least two characters, debounces typing, ignores stale results from older queries, clears the list on errors, and blocks selection if a result has no barcode. `OpenFoodFactsClient.searchProducts()` filters out search rows without a barcode or display name.

---

## Focused Test Command

Use a clean focused run after Carb Lookup code, resource, or documentation-adjacent behavior changes:

```powershell
$javaPath = (Get-Command java).Source; $env:JAVA_HOME = Split-Path (Split-Path $javaPath -Parent) -Parent; .\gradlew.bat :app:clean :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.carblookup.*" --no-daemon
```

This workspace has previously shown stale Android resource intermediates on non-clean runs, so keep `:app:clean` in the command for reliability.
