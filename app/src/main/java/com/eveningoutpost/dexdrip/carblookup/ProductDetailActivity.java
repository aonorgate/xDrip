package com.eveningoutpost.dexdrip.carblookup;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.api.OpenFoodFactsClient;
import com.eveningoutpost.dexdrip.carblookup.api.ProductData;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.db.FavoriteItemRepository;
import com.eveningoutpost.dexdrip.carblookup.db.ProductCacheRepository;
import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;
import com.eveningoutpost.dexdrip.carblookup.utils.CarbLookupCalculator;
import com.eveningoutpost.dexdrip.models.JoH;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE       = "BARCODE";
    public static final String EXTRA_MANUAL_ENTRY  = "MANUAL_ENTRY";
    public static final String EXTRA_PRODUCT_NAME  = "PRODUCT_NAME";
    public static final String EXTRA_BRAND         = "BRAND";
    public static final String EXTRA_CARBS_PER_100G = "CARBS_PER_100G";
    public static final String EXTRA_CARBS_FOR_PORTION = "CARBS_FOR_PORTION";
    public static final String EXTRA_PORTION_GRAMS = "PORTION_GRAMS";
    public static final String EXTRA_FAVORITE_ID = "FAVORITE_ID";
    public static final String EXTRA_FAVORITE_USE_COUNT = "FAVORITE_USE_COUNT";
    public static final String EXTRA_LOOKUP_SOURCE = "LOOKUP_SOURCE";
    public static final String EXTRA_SMALL_PORTION_GRAMS = "SMALL_PORTION_GRAMS";
    public static final String EXTRA_MEDIUM_PORTION_GRAMS = "MEDIUM_PORTION_GRAMS";
    public static final String EXTRA_LARGE_PORTION_GRAMS = "LARGE_PORTION_GRAMS";
    public static final String EXTRA_PORTION_UNIT = "PORTION_UNIT";
    public static final String EXTRA_GRAMS_PER_PORTION_UNIT = "GRAMS_PER_PORTION_UNIT";
    public static final String EXTRA_STANDARD_CARB_ITEM = "STANDARD_CARB_ITEM";

    private String barcode;
    private String productName = "";
    private String brand = "";
    // Positive value = known carbs; -1.0 = not yet entered (sentinel for manual-entry mode)
    private double carbsPer100g = 0.0;

    private TextView productNameTextView;
    private TextView brandTextView;
    private TextView carbsPer100gTextView;
    private TextView servingSizeTextView;
    private TextView lookupSourceTextView;
    private TextView nutritionWarningTextView;
    private TextView favoriteUseCountTextView;
    private EditText portionGramsEditText;
    private TextView carbsResultTextView;
    private LinearLayout portionSuggestionLayout;
    private Button smallPortionButton;
    private Button mediumPortionButton;
    private Button largePortionButton;
    private Button addToMealButton;
    private Button saveToFavoritesButton;
    private ImageButton resetFavoriteUseCountButton;
    private LinearLayout manualEntryLayout;
    private EditText manualProductNameEditText;
    private EditText manualBrandEditText;
    private EditText manualCarbsPer100gEditText;
    private static final double MAX_PORTION_GRAMS = 5000.0;

    private boolean manualEntryRequested;
    private long favoriteId;
    private int favoriteUseCount;
    private FoodDbSource foodDbSource;
    private String portionUnit = StandardCarbItem.UNIT_GRAMS;
    private double gramsPerPortionUnit = 1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_product_details);

        productNameTextView = findViewById(R.id.productNameTextView);
        brandTextView = findViewById(R.id.brandTextView);
        carbsPer100gTextView = findViewById(R.id.carbsPer100gTextView);
        servingSizeTextView = findViewById(R.id.servingSizeTextView);
        lookupSourceTextView = findViewById(R.id.lookupSourceTextView);
        nutritionWarningTextView = findViewById(R.id.nutritionWarningTextView);
        favoriteUseCountTextView = findViewById(R.id.favoriteUseCountTextView);
        portionGramsEditText = findViewById(R.id.portionGramsEditText);
        carbsResultTextView = findViewById(R.id.carbsResultTextView);
        portionSuggestionLayout = findViewById(R.id.portionSuggestionLayout);
        smallPortionButton = findViewById(R.id.smallPortionButton);
        mediumPortionButton = findViewById(R.id.mediumPortionButton);
        largePortionButton = findViewById(R.id.largePortionButton);
        addToMealButton = findViewById(R.id.addToMealButton);
        saveToFavoritesButton = findViewById(R.id.saveToFavoritesButton);
        resetFavoriteUseCountButton = findViewById(R.id.resetFavoriteUseCountButton);
        manualEntryLayout = findViewById(R.id.manualEntryLayout);
        manualProductNameEditText = findViewById(R.id.manualProductNameEditText);
        manualBrandEditText = findViewById(R.id.manualBrandEditText);
        manualCarbsPer100gEditText = findViewById(R.id.manualCarbsPer100gEditText);
        bindPortionUnitHint();

        barcode = getIntent().getStringExtra(EXTRA_BARCODE);
        manualEntryRequested = getIntent().getBooleanExtra(EXTRA_MANUAL_ENTRY, false);
        favoriteId = getIntent().getLongExtra(EXTRA_FAVORITE_ID, 0L);
        favoriteUseCount = getIntent().getIntExtra(EXTRA_FAVORITE_USE_COUNT, 0);
        foodDbSource = createFoodDbSource();
        bindFavoriteUseCount();

        if (hasExistingItemExtras()) {
            bindExistingItemDetails();
        } else if (barcode == null || barcode.trim().isEmpty()) {
            if (manualEntryRequested) {
                showManualEntry(getString(R.string.carblookup_manual_entry));
            } else {
                Toast.makeText(this, R.string.carblookup_barcode_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else if (manualEntryRequested) {
            showManualEntry(getString(R.string.carblookup_manual_entry));
        } else {
            loadProductDetails();
        }

        portionGramsEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateCarbsResult();
            }
        });

        manualCarbsPer100gEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                syncManualCarbsPer100g();
                updateCarbsResult();
            }
        });

        addToMealButton.setOnClickListener(v -> addToMeal());
        saveToFavoritesButton.setOnClickListener(v -> saveToFavorites());
        resetFavoriteUseCountButton.setOnClickListener(v -> resetFavoriteUseCount());
        updateCarbsResult();
    }

    private void bindFavoriteUseCount() {
        if (favoriteId > 0 && favoriteUseCount > 0) {
            favoriteUseCountTextView.setText(getString(R.string.carblookup_favorite_used_format, favoriteUseCount));
            favoriteUseCountTextView.setVisibility(View.VISIBLE);
            resetFavoriteUseCountButton.setVisibility(View.VISIBLE);
        } else {
            favoriteUseCountTextView.setVisibility(View.GONE);
            resetFavoriteUseCountButton.setVisibility(View.GONE);
        }
    }

    private void resetFavoriteUseCount() {
        if (favoriteId <= 0) {
            return;
        }
        createFavoriteItemRepository().resetUseCount(favoriteId);
        favoriteUseCount = 0;
        bindFavoriteUseCount();
    }

    private void loadProductDetails() {
        if (barcode == null) {
            Toast.makeText(this, R.string.carblookup_barcode_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            ProductCacheRepository cacheRepo = createProductCacheRepository();
            ProductCacheRepository.CachedProduct cached = cacheRepo.lookup(foodDbSource.key(), barcode);
            if (cached != null) {
                productName = cached.productName != null ? cached.productName : barcode;
                brand = cached.brands != null ? cached.brands : "";
                carbsPer100g = cached.carbsPer100g;
                bindProductDetails(productName, brand, carbsPer100g, cached.servingSize, null,
                    foodDbSource.cachedSourceLabel());
                return;
            }

            productNameTextView.setText(R.string.carblookup_loading);
            createOpenFoodFactsClient().fetchByBarcode(barcode, new OpenFoodFactsClient.ProductCallback() {
                @Override
                public void onSuccess(ProductData.Product product) {
                    productName = product.productName != null ? product.productName : barcode;
                    brand = product.brands != null ? product.brands : "";
                    carbsPer100g = product.nutriments != null ? product.nutriments.carbohydrates100g : 0.0;

                    bindProductDetails(productName, brand, carbsPer100g, product.servingSize, product.quantity,
                            foodDbSource.sourceLabel());

                    try {
                        createProductCacheRepository().store(foodDbSource.key(), barcode, product);
                    } catch (RuntimeException ignored) {
                        // Keep the manual/product flow usable even if the optional cache is unavailable.
                    }
                }

                @Override
                public void onNotFound(String bc) {
                    showManualEntry(getString(R.string.carblookup_product_not_found));
                }

                @Override
                public void onError(String errorMessage) {
                    showManualEntry(getString(R.string.carblookup_error_format, errorMessage));
                }
            });
        } catch (RuntimeException e) {
            showManualEntry(getString(R.string.carblookup_manual_entry));
        }
    }

    protected OpenFoodFactsClient createOpenFoodFactsClient() {
        return new OpenFoodFactsClient(foodDbSource != null ? foodDbSource : FoodDbSource.current());
    }

    protected FoodDbSource createFoodDbSource() {
        return FoodDbSource.current();
    }

    protected ProductCacheRepository createProductCacheRepository() {
        return new ProductCacheRepository(com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase.getInstance(this));
    }

    protected FavoriteItemRepository createFavoriteItemRepository() {
        return new FavoriteItemRepository(com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase.getInstance(this));
    }

    private void showManualEntry(String reason) {
        carbsPer100g = -1.0;
        productNameTextView.setText(reason);
        brandTextView.setText("");
        brandTextView.setVisibility(View.GONE);
        carbsPer100gTextView.setVisibility(View.GONE);
        servingSizeTextView.setVisibility(View.GONE);
        lookupSourceTextView.setVisibility(View.GONE);
        nutritionWarningTextView.setVisibility(View.GONE);
        manualEntryLayout.setVisibility(View.VISIBLE);
    }

    private boolean hasExistingItemExtras() {
        return ProductDetailContract.hasExistingItemExtras(getIntent());
    }

    private void bindExistingItemDetails() {
        productName = getIntent().getStringExtra(EXTRA_PRODUCT_NAME);
        if (productName == null || productName.trim().isEmpty()) {
            productName = getString(R.string.carblookup_manual_item_name);
        }
        brand = getIntent().getStringExtra(EXTRA_BRAND);
        if (brand == null) {
            brand = "";
        }
        carbsPer100g = getIntent().getDoubleExtra(EXTRA_CARBS_PER_100G, 0.0);
        double portionGrams = getIntent().getDoubleExtra(EXTRA_PORTION_GRAMS, 0.0);
        String sourceLabel = getIntent().getStringExtra(EXTRA_LOOKUP_SOURCE);
        boolean standardCarbItem = getIntent().getBooleanExtra(EXTRA_STANDARD_CARB_ITEM, false);
        if (standardCarbItem) {
            bindPortionUnitFromIntent();
        } else {
            resetPortionUnit();
        }
        if (!standardCarbItem) {
            addToMealButton.setText(R.string.carblookup_update_item);
        }

        if (manualEntryRequested) {
            showManualEntry(getString(R.string.carblookup_edit_item_title));
            carbsPer100g = getIntent().getDoubleExtra(EXTRA_CARBS_PER_100G, 0.0);
            manualProductNameEditText.setText(productName);
            manualBrandEditText.setText(brand);
            manualCarbsPer100gEditText.setText(formatDecimal(carbsPer100g));
        } else {
            bindProductDetails(productName, brand, carbsPer100g, null, null,
                    sourceLabel != null ? sourceLabel : getString(R.string.carblookup_source_saved_item));
            bindPortionSuggestionsFromIntent();
        }
        if (portionGrams > 0) {
            portionGramsEditText.setText(formatDecimal(displayAmountForGrams(portionGrams)));
        }
    }

    private void bindProductDetails(String displayName, String displayBrand, double displayCarbsPer100g,
                                    String servingSize, String quantity) {
        bindProductDetails(displayName, displayBrand, displayCarbsPer100g, servingSize, quantity, null);
    }

    private void bindProductDetails(String displayName, String displayBrand, double displayCarbsPer100g,
                                    String servingSize, String quantity, String sourceLabel) {
        productNameTextView.setText(displayName);
        brandTextView.setText(displayBrand);
        brandTextView.setVisibility(displayBrand == null || displayBrand.isEmpty() ? View.GONE : View.VISIBLE);
        carbsPer100gTextView.setVisibility(View.VISIBLE);
        carbsPer100gTextView.setText(getString(R.string.carblookup_carbs_per_100_unit_format,
            carbsPer100PortionUnits(displayCarbsPer100g), portionUnit));
        PortionInfo portionInfo = inferPortionInfo(quantity, servingSize);
        if (servingSize != null && !servingSize.isEmpty()) {
            if (portionInfo.portionCount > 0) {
                servingSizeTextView.setText(getString(R.string.carblookup_serving_size_portions_format,
                        servingSize, portionInfo.portionCount));
            } else {
                servingSizeTextView.setText(getString(R.string.carblookup_serving_size_format, servingSize));
            }
            servingSizeTextView.setVisibility(View.VISIBLE);
        } else if (portionInfo.portionCount > 0) {
            servingSizeTextView.setText(getString(R.string.carblookup_portions_available_format, portionInfo.portionCount));
            servingSizeTextView.setVisibility(View.VISIBLE);
        } else {
            servingSizeTextView.setVisibility(View.GONE);
        }
        if (portionInfo.defaultPortionGrams > 0 && portionGramsEditText.getText().toString().trim().isEmpty()) {
            portionGramsEditText.setText(formatDecimal(displayAmountForGrams(portionInfo.defaultPortionGrams)));
        }
        if (sourceLabel != null && !sourceLabel.trim().isEmpty()) {
            lookupSourceTextView.setText(sourceLabel);
            lookupSourceTextView.setVisibility(View.VISIBLE);
        } else {
            lookupSourceTextView.setVisibility(View.GONE);
        }
        showNutritionWarningIfNeeded(displayCarbsPer100g);
        manualEntryLayout.setVisibility(View.GONE);
        updateCarbsResult();
    }

    private void bindPortionSuggestionsFromIntent() {
        double small = getIntent().getDoubleExtra(EXTRA_SMALL_PORTION_GRAMS, 0.0);
        double medium = getIntent().getDoubleExtra(EXTRA_MEDIUM_PORTION_GRAMS, 0.0);
        double large = getIntent().getDoubleExtra(EXTRA_LARGE_PORTION_GRAMS, 0.0);
        bindPortionSuggestions(small, medium, large);
    }

    private void bindPortionSuggestions(double small, double medium, double large) {
        if (small <= 0.0 || medium <= 0.0 || large <= 0.0) {
            portionSuggestionLayout.setVisibility(View.GONE);
            return;
        }
        portionSuggestionLayout.setVisibility(View.VISIBLE);
        bindPortionButton(smallPortionButton, R.string.carblookup_portion_small_unit_format, small);
        bindPortionButton(mediumPortionButton, R.string.carblookup_portion_medium_unit_format, medium);
        bindPortionButton(largePortionButton, R.string.carblookup_portion_large_unit_format, large);
    }

    private void bindPortionButton(Button button, int labelResId, double grams) {
        double displayAmount = displayAmountForGrams(grams);
        button.setText(getString(labelResId, formatDecimal(displayAmount), portionUnit));
        button.setOnClickListener(v -> portionGramsEditText.setText(formatDecimal(displayAmount)));
    }

    private void showNutritionWarningIfNeeded(double displayCarbsPer100g) {
        if (displayCarbsPer100g <= 0.0) {
            nutritionWarningTextView.setText(R.string.carblookup_warning_zero_carbs);
            nutritionWarningTextView.setVisibility(View.VISIBLE);
        } else if (displayCarbsPer100g > 100.0) {
            nutritionWarningTextView.setText(R.string.carblookup_warning_high_carbs);
            nutritionWarningTextView.setVisibility(View.VISIBLE);
        } else {
            nutritionWarningTextView.setVisibility(View.GONE);
        }
    }

    private void syncManualCarbsPer100g() {
        if (manualEntryLayout.getVisibility() != View.VISIBLE) {
            return;
        }
        String val = manualCarbsPer100gEditText.getText().toString().trim();
        if (val.isEmpty()) {
            carbsPer100g = -1.0;
            return;
        }
        try {
            carbsPer100g = Double.parseDouble(val);
        } catch (NumberFormatException ignored) {
            carbsPer100g = -1.0;
        }
    }

    private void updateCarbsResult() {
        String portionText = portionGramsEditText.getText().toString().trim();
        if (portionText.isEmpty() || carbsPer100g < 0) {
            carbsResultTextView.setText(getString(R.string.carblookup_carbs_zero_decimal));
            return;
        }
        try {
            double portionGrams = gramsForDisplayAmount(Double.parseDouble(portionText));
            double carbs = CarbLookupCalculator.calculateCarbs(carbsPer100g, portionGrams);
            carbsResultTextView.setText(getString(R.string.carblookup_carbs_format, carbs));
        } catch (Exception ignored) {
            carbsResultTextView.setText(getString(R.string.carblookup_carbs_zero_decimal));
        }
    }

    private void addToMeal() {
        String portionText = portionGramsEditText.getText().toString().trim();
        if (portionText.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_please_enter_portion, Toast.LENGTH_SHORT).show();
            return;
        }
        if (carbsPer100g < 0) {
            Toast.makeText(this, R.string.carblookup_please_enter_carbs, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (manualEntryLayout.getVisibility() == View.VISIBLE) {
                String manualName = manualProductNameEditText.getText().toString().trim();
                if (manualName.isEmpty()) {
                    Toast.makeText(this, R.string.carblookup_please_enter_product_name, Toast.LENGTH_SHORT).show();
                    return;
                }
                productName = manualName;
                brand = manualBrandEditText.getText().toString().trim();
            }
            double portionGrams = gramsForDisplayAmount(Double.parseDouble(portionText));
            if (portionGrams > MAX_PORTION_GRAMS) {
                Toast.makeText(this, getString(R.string.carblookup_portion_too_large, (int) MAX_PORTION_GRAMS), Toast.LENGTH_SHORT).show();
                return;
            }
            double carbs = CarbLookupCalculator.calculateCarbs(carbsPer100g, portionGrams);
            Intent result = new Intent();
            ProductDetailContract.putProductResult(result, productName, brand, barcode,
                    carbsPer100g, portionGrams, carbs);
            if (favoriteId > 0) {
                result.putExtra(EXTRA_FAVORITE_ID, favoriteId);
            }
            setResult(RESULT_OK, result);
            finish();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.carblookup_invalid_input, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFavorites() {
        if (carbsPer100g < 0) {
            syncManualCarbsPer100g();
        }
        if (carbsPer100g < 0) {
            Toast.makeText(this, R.string.carblookup_please_enter_carbs, Toast.LENGTH_SHORT).show();
            return;
        }

        String favName = productName;
        String favBrand = brand;
        if (manualEntryLayout.getVisibility() == View.VISIBLE) {
            String manualName = manualProductNameEditText.getText().toString().trim();
            if (manualName.isEmpty()) {
                Toast.makeText(this, R.string.carblookup_please_enter_product_name, Toast.LENGTH_SHORT).show();
                return;
            }
            favName = manualName;
            favBrand = manualBrandEditText.getText().toString().trim();
        }

        double portionGrams = 0.0;
        String portionText = portionGramsEditText.getText().toString().trim();
        if (!portionText.isEmpty()) {
            try {
                portionGrams = gramsForDisplayAmount(Double.parseDouble(portionText));
            } catch (NumberFormatException ignored) {
            }
        }

        FavoriteItem fav = new FavoriteItem();
        fav.productName = favName;
        fav.brand = favBrand.isEmpty() ? null : favBrand;
        fav.barcode = barcode;
        fav.carbsPer100g = carbsPer100g;
        fav.defaultPortionGrams = portionGrams;

        createFavoriteItemRepository().save(fav);
        Toast.makeText(this, R.string.carblookup_favorite_saved, Toast.LENGTH_SHORT).show();
    }

    private PortionInfo inferPortionInfo(String quantity, String servingSize) {
        PortionInfo info = new PortionInfo();
        double quantityGrams = parseGramAmount(quantity);
        double servingGrams = parseGramAmount(servingSize);
        if (quantityGrams > 0 && servingGrams > 0) {
            info.portionCount = Math.round((quantityGrams / servingGrams) * 10.0) / 10.0;
            if (info.portionCount > 0) {
                info.defaultPortionGrams = Math.round((quantityGrams / info.portionCount) * 10.0) / 10.0;
            }
        } else if (servingGrams > 0) {
            info.defaultPortionGrams = servingGrams;
        }
        return info;
    }

    private double parseGramAmount(String text) {
        if (text == null) {
            return 0.0;
        }
        String normalized = text.toLowerCase(Locale.US).replace(',', '.');
        Matcher multiplierMatcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[x×]\\s*(\\d+(?:\\.\\d+)?)\\s*(kg|g|mg|ml|l|grams?|milligrams?|kilograms?|milliliters?|millilitres?|liters?|litres?)").matcher(normalized);
        if (multiplierMatcher.find()) {
            double count = Double.parseDouble(multiplierMatcher.group(1));
            double amount = Double.parseDouble(multiplierMatcher.group(2));
            return count * convertToGrams(amount, multiplierMatcher.group(3));
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|g|mg|ml|l|grams?|milligrams?|kilograms?|milliliters?|millilitres?|liters?|litres?)").matcher(normalized);
        if (matcher.find()) {
            return convertToGrams(Double.parseDouble(matcher.group(1)), matcher.group(2));
        }
        return 0.0;
    }

    private double convertToGrams(double amount, String unit) {
        if (unit.startsWith("kg") || unit.startsWith("kilogram")) {
            return amount * 1000.0;
        }
        if (unit.startsWith("mg") || unit.startsWith("milligram")) {
            return amount / 1000.0;
        }
        if (unit.equals("l") || unit.startsWith("liter") || unit.startsWith("litre")) {
            return amount * 1000.0;
        }
        if (unit.startsWith("ml") || unit.startsWith("milliliter") || unit.startsWith("millilitre")) {
            return amount;
        }
        return amount;
    }

    private void bindPortionUnitFromIntent() {
        String unit = getIntent().getStringExtra(EXTRA_PORTION_UNIT);
        double gramsPerUnit = getIntent().getDoubleExtra(EXTRA_GRAMS_PER_PORTION_UNIT, 1.0);
        portionUnit = unit == null || unit.trim().isEmpty() ? StandardCarbItem.UNIT_GRAMS : unit;
        gramsPerPortionUnit = gramsPerUnit > 0.0 ? gramsPerUnit : 1.0;
        bindPortionUnitHint();
    }

    private void resetPortionUnit() {
        portionUnit = StandardCarbItem.UNIT_GRAMS;
        gramsPerPortionUnit = 1.0;
        bindPortionUnitHint();
    }

    private void bindPortionUnitHint() {
        portionGramsEditText.setHint(getString(R.string.carblookup_portion_size_unit_hint, portionUnit));
    }

    private double displayAmountForGrams(double grams) {
        return grams / gramsPerPortionUnit;
    }

    private double gramsForDisplayAmount(double amount) {
        return amount * gramsPerPortionUnit;
    }

    private double carbsPer100PortionUnits(double carbsPer100g) {
        return carbsPer100g * gramsPerPortionUnit;
    }

    private String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static class PortionInfo {
        double portionCount;
        double defaultPortionGrams;
    }
}
