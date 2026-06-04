package com.eveningoutpost.dexdrip.carblookup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.ProductCacheRepository;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;
import com.eveningoutpost.dexdrip.carblookup.utils.GiLookupTable;
import com.eveningoutpost.dexdrip.models.JoH;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeEditActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "RECIPE_ID";
    static final String EXTRA_SEED_ITEM_COUNT = "SEED_ITEM_COUNT";
    static final String EXTRA_SEED_PRODUCT_NAMES = "SEED_PRODUCT_NAMES";
    static final String EXTRA_SEED_BRANDS = "SEED_BRANDS";
    static final String EXTRA_SEED_BARCODES = "SEED_BARCODES";
    static final String EXTRA_SEED_CARBS_PER_100G = "SEED_CARBS_PER_100G";
    static final String EXTRA_SEED_ITEM_WEIGHT_GRAMS = "SEED_ITEM_WEIGHT_GRAMS";
    static final String EXTRA_SEED_ITEM_CARBS_GRAMS = "SEED_ITEM_CARBS_GRAMS";
    static final String EXTRA_SEED_GI_ESTIMATES = "SEED_GI_ESTIMATES";
    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int REQUEST_PRODUCT_DETAIL = 1002;
    static final int REQUEST_ITEM_SEARCH = 1003;

    private static final String[] MEAL_KEYS   = {"any", "breakfast", "lunch", "dinner", "snack"};

    private RecipeRepository repo;
    private Recipe currentRecipe;
    private RecipeEditItemAdapter itemAdapter;
    private List<RecipeItem> items;

    private EditText nameEditText;
    private EditText portionCountEditText;
    private EditText notesEditText;
    private Spinner  mealTimeSpinner;
    private TextView totalCarbsTextView;
    private ProductCacheRepository cacheRepo;
    private MealItemFactory mealItemFactory;
    private ImageButton editTitleButton;
    private ImageButton deleteTemplateButton;
    private int editingItemIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_edit);
        JoH.fixActionBar(this);

        repo = createRecipeRepository();
        cacheRepo = createProductCacheRepository();
        mealItemFactory = createMealItemFactory();
        items   = new ArrayList<>();

        nameEditText        = findViewById(R.id.recipeNameEditText);
        portionCountEditText = findViewById(R.id.portionCountEditText);
        notesEditText       = findViewById(R.id.recipeNotesEditText);
        mealTimeSpinner     = findViewById(R.id.mealTimeSpinner);
        totalCarbsTextView  = findViewById(R.id.totalCarbsTextView);
        ListView itemListView = findViewById(R.id.recipeItemsListView);
        Button addItemButton = findViewById(R.id.addItemButton);
        Button addManualItemButton = findViewById(R.id.addManualItemButton);
        Button itemSearchButton = findViewById(R.id.itemSearchButton);
        Button commonFoodsButton = findViewById(R.id.commonFoodsButton);
        Button saveButton    = findViewById(R.id.saveRecipeButton);
        editTitleButton = findViewById(R.id.editTitleButton);
        deleteTemplateButton = findViewById(R.id.deleteTemplateButton);

        editTitleButton.setOnClickListener(v -> {
            nameEditText.requestFocus();
            nameEditText.setSelection(nameEditText.getText().length());
        });
        deleteTemplateButton.setOnClickListener(v -> confirmDeleteTemplate());

        // Meal-time spinner
        String[] mealLabels = {getString(R.string.carblookup_meal_time_any), getString(R.string.carblookup_filter_breakfast), getString(R.string.carblookup_filter_lunch), getString(R.string.carblookup_filter_dinner), getString(R.string.carblookup_filter_snack)};
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, mealLabels);
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mealTimeSpinner.setAdapter(mealAdapter);

        itemAdapter = new RecipeEditItemAdapter(this, items,
                (item, position) -> {
                    editIngredient(item, position);
                },
                (item, position) -> {
                    items.remove(position);
                    itemAdapter.notifyDataSetChanged();
                    updateTotalCarbs();
                });
        itemListView.setAdapter(itemAdapter);
        itemListView.setEmptyView(findViewById(R.id.emptyItemsTextView));

        portionCountEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { updateTotalCarbs(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        addItemButton.setOnClickListener(v -> launchBarcodeScanner());
        addManualItemButton.setOnClickListener(v -> launchManualEntry());
        itemSearchButton.setOnClickListener(v -> launchItemSearch());
        commonFoodsButton.setOnClickListener(v -> showCommonFoods());
        saveButton.setOnClickListener(v -> saveRecipe());

        // Load existing recipe if editing
        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, 0L);
        if (recipeId != 0) {
            loadRecipe(recipeId);
        } else {
            currentRecipe = new Recipe();
            setTitle(getString(R.string.carblookup_title_new_template));
            portionCountEditText.setText(String.valueOf(1));
            loadSeedItemsFromIntent();
            updateTotalCarbs();
        }
    }

    static Intent newRecipeWithItems(Context context, List<RecipeItem> seedItems) {
        Intent intent = new Intent(context, RecipeEditActivity.class);
        putSeedItems(intent, seedItems);
        return intent;
    }

    static List<RecipeItem> seedItemsFromIntent(Intent intent) {
        List<RecipeItem> seedItems = new ArrayList<>();
        if (intent == null) {
            return seedItems;
        }

        int count = intent.getIntExtra(EXTRA_SEED_ITEM_COUNT, 0);
        String[] names = intent.getStringArrayExtra(EXTRA_SEED_PRODUCT_NAMES);
        double[] carbsPer100g = intent.getDoubleArrayExtra(EXTRA_SEED_CARBS_PER_100G);
        double[] weights = intent.getDoubleArrayExtra(EXTRA_SEED_ITEM_WEIGHT_GRAMS);
        double[] itemCarbs = intent.getDoubleArrayExtra(EXTRA_SEED_ITEM_CARBS_GRAMS);
        if (count <= 0 || names == null || carbsPer100g == null || weights == null || itemCarbs == null) {
            return seedItems;
        }

        String[] brands = intent.getStringArrayExtra(EXTRA_SEED_BRANDS);
        String[] barcodes = intent.getStringArrayExtra(EXTRA_SEED_BARCODES);
        int[] giEstimates = intent.getIntArrayExtra(EXTRA_SEED_GI_ESTIMATES);
        int safeCount = Math.min(count, Math.min(names.length,
                Math.min(carbsPer100g.length, Math.min(weights.length, itemCarbs.length))));
        for (int i = 0; i < safeCount; i++) {
            if (names[i] == null || names[i].trim().isEmpty() || weights[i] <= 0.0) {
                continue;
            }
            RecipeItem item = new RecipeItem();
            item.productName = names[i];
            item.brand = stringAt(brands, i);
            item.barcode = stringAt(barcodes, i);
            item.carbsPer100g = carbsPer100g[i];
            item.itemWeightGrams = weights[i];
            item.itemCarbsGrams = itemCarbs[i];
            item.giEstimate = intAt(giEstimates, i);
            seedItems.add(item);
        }
        return seedItems;
    }

    private static void putSeedItems(Intent intent, List<RecipeItem> seedItems) {
        if (seedItems == null || seedItems.isEmpty()) {
            return;
        }
        int count = seedItems.size();
        String[] names = new String[count];
        String[] brands = new String[count];
        String[] barcodes = new String[count];
        double[] carbsPer100g = new double[count];
        double[] weights = new double[count];
        double[] itemCarbs = new double[count];
        int[] giEstimates = new int[count];
        for (int i = 0; i < count; i++) {
            RecipeItem item = seedItems.get(i);
            names[i] = item.productName;
            brands[i] = item.brand;
            barcodes[i] = item.barcode;
            carbsPer100g[i] = item.carbsPer100g;
            weights[i] = item.itemWeightGrams;
            itemCarbs[i] = item.itemCarbsGrams;
            giEstimates[i] = item.giEstimate;
        }
        intent.putExtra(EXTRA_SEED_ITEM_COUNT, count);
        intent.putExtra(EXTRA_SEED_PRODUCT_NAMES, names);
        intent.putExtra(EXTRA_SEED_BRANDS, brands);
        intent.putExtra(EXTRA_SEED_BARCODES, barcodes);
        intent.putExtra(EXTRA_SEED_CARBS_PER_100G, carbsPer100g);
        intent.putExtra(EXTRA_SEED_ITEM_WEIGHT_GRAMS, weights);
        intent.putExtra(EXTRA_SEED_ITEM_CARBS_GRAMS, itemCarbs);
        intent.putExtra(EXTRA_SEED_GI_ESTIMATES, giEstimates);
    }

    private static String stringAt(String[] values, int index) {
        return values != null && index < values.length ? values[index] : null;
    }

    private static int intAt(int[] values, int index) {
        return values != null && index < values.length ? values[index] : 0;
    }

    private void loadSeedItemsFromIntent() {
        List<RecipeItem> seedItems = seedItemsFromIntent(getIntent());
        if (seedItems.isEmpty()) {
            return;
        }
        items.clear();
        items.addAll(seedItems);
        itemAdapter.notifyDataSetChanged();
    }

    protected RecipeRepository createRecipeRepository() {
        return new RecipeRepository(CarbLookupDatabase.getInstance(this));
    }

    protected ProductCacheRepository createProductCacheRepository() {
        return new ProductCacheRepository(CarbLookupDatabase.getInstance(this));
    }

    protected MealItemFactory createMealItemFactory() {
        return new MealItemFactory();
    }

    protected FoodDbSource createFoodDbSource() {
        return FoodDbSource.current();
    }

    private void loadRecipe(long id) {
        currentRecipe = repo.getRecipeById(id);
        if (currentRecipe == null) {
            Toast.makeText(this, R.string.carblookup_recipe_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setTitle(getString(R.string.carblookup_title_edit_template));
        editTitleButton.setVisibility(View.VISIBLE);
        deleteTemplateButton.setVisibility(View.VISIBLE);
        nameEditText.setText(currentRecipe.name);
        portionCountEditText.setText(currentRecipe.portionCount == Math.rint(currentRecipe.portionCount)
                ? String.valueOf((int) currentRecipe.portionCount)
                : String.format(Locale.getDefault(), "%.1f", currentRecipe.portionCount));
        notesEditText.setText(currentRecipe.notes != null ? currentRecipe.notes : "");

        // Set meal-time spinner
        for (int i = 0; i < MEAL_KEYS.length; i++) {
            if (MEAL_KEYS[i].equals(currentRecipe.mealTime)) {
                mealTimeSpinner.setSelection(i);
                break;
            }
        }

        items.clear();
        items.addAll(currentRecipe.items);
        itemAdapter.notifyDataSetChanged();
        updateTotalCarbs();
    }

    private void launchBarcodeScanner() {
        editingItemIndex = -1;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        launchBarcodeScannerInternal();
    }

    private void launchBarcodeScannerInternal() {
        try {
            new IntentIntegrator(this)
                    .setDesiredBarcodeFormats(IntentIntegrator.PRODUCT_CODE_TYPES)
                    .setPrompt(getString(R.string.carblookup_scan_product_prompt))
                    .setCaptureActivity(CarbLookupCaptureActivity.class)
                    .setOrientationLocked(false)
                    .setBeepEnabled(false)
                    .initiateScan();
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.carblookup_scanner_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchManualEntry() {
        editingItemIndex = -1;
        startActivityForResult(ProductDetailContract.manualEntry(this), REQUEST_PRODUCT_DETAIL);
    }

    private void launchItemSearch() {
        editingItemIndex = -1;
        startActivityForResult(new Intent(this, FoodSearchActivity.class), REQUEST_ITEM_SEARCH);
    }

    private void showCommonFoods() {
        editingItemIndex = -1;
        new CommonFoodsPicker(this, createFoodDbSource(), this::launchStandardCarbItem).show();
    }

    private void launchStandardCarbItem(StandardCarbItem item) {
        editingItemIndex = -1;
        startActivityForResult(ProductDetailContract.forStandardCarbItem(this, item), REQUEST_PRODUCT_DETAIL);
    }

    private void editIngredient(RecipeItem item, int position) {
        editingItemIndex = position;
        startActivityForResult(ProductDetailContract.forRecipeItem(this, item), REQUEST_PRODUCT_DETAIL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                editingItemIndex = -1;
                startActivityForResult(ProductDetailContract.forBarcode(this, scanResult.getContents()),
                        REQUEST_PRODUCT_DETAIL);
            } else {
                editingItemIndex = -1;
                Toast.makeText(this, R.string.carblookup_scan_cancelled, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PRODUCT_DETAIL && resultCode == RESULT_OK && data != null) {
            RecipeItem item = mealItemFactory.toRecipeItem(mealItemFactory.fromProductResult(data));
            if (editingItemIndex >= 0 && editingItemIndex < items.size()) {
                RecipeItem original = items.get(editingItemIndex);
                int cachedGi = cacheRepo.getGiOverride(item.barcode);
                item.giEstimate = cachedGi > 0 ? cachedGi : original.giEstimate;
                items.set(editingItemIndex, item);
                editingItemIndex = -1;
            } else {
                int cachedGi = cacheRepo.getGiOverride(item.barcode);
                item.giEstimate = cachedGi > 0 ? cachedGi : GiLookupTable.lookupGi(item.productName);
                items.add(item);
            }
            itemAdapter.notifyDataSetChanged();
            updateTotalCarbs();
        } else if (requestCode == REQUEST_ITEM_SEARCH && resultCode == RESULT_OK && data != null) {
            String barcode = data.getStringExtra(FoodSearchActivity.EXTRA_BARCODE);
            if (barcode != null && !barcode.trim().isEmpty()) {
                editingItemIndex = -1;
                startActivityForResult(ProductDetailContract.forBarcode(this, barcode), REQUEST_PRODUCT_DETAIL);
            }
        } else if (requestCode == REQUEST_PRODUCT_DETAIL) {
            editingItemIndex = -1;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchBarcodeScannerInternal();
            } else {
                Toast.makeText(this, R.string.without_camera_permission_cannot_scan_barcode,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateTotalCarbs() {
        double total = 0;
        double portions;
        try {
            portions = Double.parseDouble(portionCountEditText.getText().toString());
            if (portions <= 0) portions = 1;
        } catch (NumberFormatException e) {
            portions = 1;
        }
        for (RecipeItem item : items) {
            total += item.itemCarbsGrams;
        }
        double perPortion = Math.round((total / portions) * 10.0) / 10.0;
        totalCarbsTextView.setText(String.format(Locale.getDefault(),
                getString(R.string.carblookup_total_per_portion_format), total, perPortion));
    }

    private void saveRecipe() {
        String name = nameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            nameEditText.setError(getString(R.string.carblookup_recipe_name_required));
            return;
        }
        double portions;
        try {
            portions = Double.parseDouble(portionCountEditText.getText().toString());
            if (portions <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            portionCountEditText.setError(getString(R.string.carblookup_invalid_portions));
            return;
        }
        if (items.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_add_ingredient, Toast.LENGTH_SHORT).show();
            return;
        }

        currentRecipe.name         = name;
        currentRecipe.portionCount = portions;
        currentRecipe.mealTime     = MEAL_KEYS[mealTimeSpinner.getSelectedItemPosition()];
        currentRecipe.notes        = notesEditText.getText().toString().trim();
        currentRecipe.items        = new ArrayList<>(items);

        if (currentRecipe.id == 0) {
            repo.saveRecipe(currentRecipe);
            Toast.makeText(this, R.string.carblookup_recipe_saved, Toast.LENGTH_SHORT).show();
        } else {
            repo.updateRecipe(currentRecipe);
            Toast.makeText(this, R.string.carblookup_recipe_updated, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmDeleteTemplate() {
        if (currentRecipe == null || currentRecipe.id == 0) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_delete_recipe_title)
                .setMessage(getString(R.string.carblookup_delete_recipe_message, currentRecipe.name))
                .setPositiveButton(R.string.carblookup_context_delete, (d, w) -> {
                    repo.deleteRecipe(currentRecipe.id);
                    Toast.makeText(this, R.string.carblookup_recipe_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }
}
