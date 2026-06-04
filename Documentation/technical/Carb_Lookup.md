# Carb Lookup

Carb Lookup is an Activity-based Java feature under `com.eveningoutpost.dexdrip.carblookup`. It uses a separate `SQLiteOpenHelper` database, `CarbLookupDatabase`, rather than the main xDrip ActiveAndroid database.

## Barcode Food Database

Barcode lookup uses `OpenFoodFactsClient` and the selected `FoodDbSource`. The setting key is `carblookup_food_db_source`; the default is world Open Food Facts.

Supported barcode sources are:

- Open Food Facts: `https://world.openfoodfacts.org/api/v2/product/`
- Open Food Facts US: `https://us.openfoodfacts.org/api/v2/product/`
- Open Food Facts UK: `https://uk.openfoodfacts.org/api/v2/product/`
- Open Food Facts France: `https://fr.openfoodfacts.org/api/v2/product/`
- Open Food Facts Germany: `https://de.openfoodfacts.org/api/v2/product/`

The product cache is keyed by both source and barcode. Keep that scoping intact so the same barcode can resolve differently for different regional databases.

## Item Search

The Carb Lookup home page and recipe editor include Item Search beside Common Foods. Item Search opens `FoodSearchActivity`, which searches the selected Open Food Facts source as the user types. Selecting a result returns its barcode to the caller; the existing `ProductDetailActivity` barcode flow then fetches the product and asks for the portion size before adding it to the meal or recipe.

`OpenFoodFactsClient.searchProducts()` uses the selected regional host and `/cgi/search.pl`, requesting `code`, product name, brand, serving, quantity, and nutriment fields. Keep search selection barcode-based so product-detail caching and nutrition warnings stay in one place.

Item Search starts searching once the query has at least two characters. The UI debounces typing, ignores stale results from older queries, clears results on errors, and refuses to continue if a selected search result does not include a barcode. The client also filters out Open Food Facts search rows that do not have both a barcode and a usable display name.

## Common Foods

The Carb Lookup home page and recipe editor include a Common Foods picker for non-barcode foods. The first dialog lists food groups, and the second dialog lists foods in the selected group. The second dialog has a Back button to return to the group list. Selecting an item opens `ProductDetailActivity` with the curated item, sets the medium portion as the default portion, and shows small, medium, and large portion buttons.

The curated data lives in `StandardCarbRepository` and is grouped by `StandardCarbCategory`. Current groups include everyday staples plus hot drinks, coffee shop items, and snacks/desserts such as ice cream, brownies, and cookies. `StandardCarbItem` entries should always include:

- Category
- Display name
- Carbs per 100g
- Small, medium, and large portion sizes in grams
- Display portion unit and grams-per-display-unit conversion when the UI should not use grams
- Source label

Common Foods keeps storage and logging gram-backed. Curated items store carbs per 100g and small, medium, and large portions as grams. Items that users naturally measure by volume, such as milk, gravy, passata, honey, syrup, coffee, tea, and coffee-shop drinks, set their display unit to `ml` and provide a grams-per-ml conversion. The picker and `ProductDetailActivity` show those items in ml, while `ProductDetailActivity` converts the entered display amount back to grams before calculating carbs and returning `EXTRA_PORTION_GRAMS` for meal or recipe logging.

The Dairy group includes common milks, yogurts, and cheeses. Keep cheese entries gram-based, with practical portion sizes for snacks or ingredients.

Regional mapping is handled by `FoodDbSource.StandardFoodRegion`:

- US source uses US names and portions.
- UK source uses UK names and portions.
- World, France, Germany, and null fallback use European-style names and portions.

When adding or editing items, keep names distinct within each region, portions ordered from small to large, and categories populated so the two-step picker remains useful.

### User-customisable Common Foods (JSON file)

Common Foods can be customised by placing a JSON file at `/sdcard/xdrip/common_foods.json`. The file is loaded at runtime and merged with the built-in defaults:

- Items in the JSON that match a built-in item by category + name (case-insensitive) override that built-in item.
- Items with a new name are appended to the list.
- Items with an unrecognised category are silently skipped.

To generate a seed file containing all built-in items as a starting template, call `StandardCarbRepository.exportDefaultsToFile(source)`. The file is re-read automatically when its timestamp changes. Call `StandardCarbRepository.reloadUserFoods()` to force an immediate cache refresh.

Each JSON entry has the following fields:

| Field | Required | Description |
|-------|----------|-------------|
| `category` | Yes | Category enum name (e.g. `STARCHES_GRAINS`) or label (e.g. `Fruit`) |
| `name` | Yes | Display name |
| `carbsPer100g` | Yes | Carbohydrates per 100g |
| `smallPortion` | Yes | Small portion in grams |
| `mediumPortion` | Yes | Medium portion in grams |
| `largePortion` | Yes | Large portion in grams |
| `source` | No | Source label (defaults to `User`) |
| `portionUnit` | No | `"g"` or `"ml"` (defaults to `"g"`) |
| `gramsPerMl` | No | Grams per millilitre for liquid items |

Example:

```json
[
  {
    "category": "FRUIT",
    "name": "Kiwi",
    "carbsPer100g": 14.7,
    "smallPortion": 60,
    "mediumPortion": 90,
    "largePortion": 130,
    "source": "User"
  },
  {
    "category": "DAIRY",
    "name": "Almond milk",
    "carbsPer100g": 1.5,
    "smallPortion": 100,
    "mediumPortion": 200,
    "largePortion": 300,
    "source": "User",
    "portionUnit": "ml",
    "gramsPerMl": 1.03
  }
]
```

`StandardCarbFileLoader` handles reading, caching, and merging. Tests for this feature are in `StandardCarbFileLoaderTest`.

## Templates

The Carb Lookup home page can create templates from the current meal. The Add Template button opens `RecipeEditActivity` and passes a copied snapshot of the current meal items as new recipe ingredients. The current meal stays unchanged, and the recipe editor owns its copy until the user saves or discards the template.

The home page keeps saved-content actions at the top: Add Template, Saved Recipes, Favourites, and Meal History. Tapping a Favourite row adds it to the current meal immediately when it has a default portion; the row edit and delete icons remain separate actions. New-item actions stay below the meal logging area: Scan Barcode, Manual Entry, Item Search, and Common Foods. The recipe editor keeps its ingredient-adding actions at the bottom near Save Recipe.

Saved templates can be deleted from the list context menu, the edit screen, or the recipe detail page. The recipe detail page exposes delete as a top-right bin icon, uses the same confirmation strings as the other deletion flows, and returns without adding anything to the current meal when deletion is confirmed.

## Meal History

Saved meals store notes and a structured meal-time key (`any`, `breakfast`, `lunch`, `dinner`, or `snack`) in `saved_meals`. The meal history row shows notes followed by the meal-time label when both exist, for example `Cornflakes with milk - Breakfast`.

Opening a meal history row shows Meal Details. The header keeps the copy action and delete action together as icon buttons: copy returns the saved meal id so the current meal can reuse its items, while delete asks for confirmation, removes the saved meal, and removes the associated xDrip treatment by `treatmentUuid` when available or by timestamp fallback.

The home page also has an editable `HH:mm` clock-time field beside the meal-time dropdown. It defaults to the current time, accepts `H:mm` or `HH:mm`, and controls the timestamp used for both the saved meal record and the xDrip carb treatment. Use it when logging a meal eaten earlier the same day.

Meal History is filtered by a persisted retention-window setting, `carblookup_history_retention_days`. The supported windows are 30, 90, and 180 days, with 90 days as the default. This setting controls which saved meals are listed; it does not delete older saved meal records.

## Tests

Focused coverage includes:

- Source-aware barcode lookup and cache scoping
- Item Search launch, typed result display, error handling, stale-response handling, invalid-result protection, and barcode handoff to Product Detail
- Open Food Facts search request shape, empty-query behavior, malformed search responses, and filtering unusable results
- Common Foods group display, row styling, Back navigation, regional item launch, category filtering, cheese entries, liquid ml row display, and data invariants
- Common Foods JSON file loading, user overrides, additions, liquid items, invalid JSON fallback, unknown category skipping, seed file export, and cache invalidation
- Template creation from current meal items, recipe-editor ingredient prefill, edit-screen deletion, and recipe detail deletion
- Editable meal clock-time defaults, historic meal logging timestamps, saved meal-time persistence, Meal History notes-plus-meal-time display, retention-window filtering, Meal Details reuse, and Meal Details deletion
- Product Detail portion entry, standard-food portion suggestions, liquid ml-to-grams conversion, favorite row selection, favorites, manual entry, and nutrition warnings

Run focused Carb Lookup tests after changes:

```powershell
$javaPath = (Get-Command java).Source; $env:JAVA_HOME = Split-Path (Split-Path $javaPath -Parent) -Parent; .\gradlew.bat :app:clean :app:testFastDebugUnitTest --tests "com.eveningoutpost.dexdrip.carblookup.*" --no-daemon
```

Use `:app:clean` for this suite after resource or layout changes. Non-clean runs can hit stale Android resource intermediates in this workspace.
