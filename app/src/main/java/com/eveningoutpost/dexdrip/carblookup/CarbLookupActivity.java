package com.eveningoutpost.dexdrip.carblookup;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.eveningoutpost.dexdrip.carblookup.db.FavoriteItemRepository;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.model.PortionResult;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;
import com.eveningoutpost.dexdrip.carblookup.utils.CarbLookupCalculator;
import com.eveningoutpost.dexdrip.models.JoH;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarbLookupActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int REQUEST_PRODUCT_DETAIL = 1002;
    private static final int REQUEST_TEMPLATE_PICK = 1003;
    private static final int REQUEST_FAVORITE_PICK = 1004;
    private static final int REQUEST_MEAL_HISTORY = 1005;
    static final int REQUEST_ITEM_SEARCH = 1006;
    private static final String[] MEAL_KEYS = {"any", "breakfast", "lunch", "dinner", "snack"};
    private static final Pattern MEAL_CLOCK_TIME_PATTERN = Pattern.compile("^(\\d{1,2}):(\\d{2})$");

    private TextView carbsResultTextView;
    private EditText mealNotesEditText;
    private EditText mealClockTimeEditText;
    private Spinner mealTimeSpinner;
    private double totalCarbs = 0.0;
    private MealRepository mealRepository;
    private RecipeRepository templateRepository;
    private FavoriteItemRepository favoriteRepository;
    private MealLogService mealLogService;
    private MealItemFactory mealItemFactory;
    private final CurrentMeal currentMeal = new CurrentMeal();
    private MealItemAdapter mealItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carb_lookup);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title);

        carbsResultTextView = findViewById(R.id.carbsResultTextView);
        mealNotesEditText = findViewById(R.id.mealNotesEditText);
        mealClockTimeEditText = findViewById(R.id.currentMealClockTimeEditText);
        mealTimeSpinner = findViewById(R.id.currentMealTimeSpinner);
        mealRepository = createMealRepository();
        templateRepository = createRecipeRepository();
        favoriteRepository = createFavoriteItemRepository();
        mealItemFactory = new MealItemFactory();
        mealLogService = createMealLogService(mealRepository);
        mealItemAdapter = new MealItemAdapter(this, currentMeal.getItems(),
                (item, position) -> editMealItem(position),
                (item, position) -> removeMealItem(position));
        setupMealTimeSpinner();
            resetMealClockTimeToNow();

        ListView mealListView = findViewById(R.id.currentMealListView);
        mealListView.setAdapter(mealItemAdapter);
        mealListView.setEmptyView(findViewById(R.id.emptyCurrentMealTextView));
        mealListView.setOnItemClickListener((parent, view, position, id) -> editMealItem(position));
        mealListView.setOnItemLongClickListener((parent, view, position, id) -> {
            removeMealItem(position);
            return true;
        });

        Button scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(v -> scanBarcode());

        Button manualEntryButton = findViewById(R.id.manualEntryButton);
        manualEntryButton.setOnClickListener(v -> launchManualEntry());

        Button itemSearchButton = findViewById(R.id.itemSearchButton);
        itemSearchButton.setOnClickListener(v -> launchItemSearch());

        Button commonFoodsButton = findViewById(R.id.commonFoodsButton);
        commonFoodsButton.setOnClickListener(v -> showCommonFoods());

        Button logMealButton = findViewById(R.id.logMealButton);
        logMealButton.setOnClickListener(v -> confirmAndSaveMeal());

        Button addTemplateButton = findViewById(R.id.addTemplateButton);
        addTemplateButton.setOnClickListener(v -> launchRecipeEditor());

        Button favoritesButton = findViewById(R.id.favoritesButton);
        favoritesButton.setOnClickListener(v -> pickFavorite());

        Button clearMealButton = findViewById(R.id.clearMealButton);
        clearMealButton.setOnClickListener(v -> confirmClearMeal());

        Button viewHistoryButton = findViewById(R.id.viewHistoryButton);
        viewHistoryButton.setOnClickListener(v -> viewMealHistory());

        Button myRecipesButton = findViewById(R.id.myRecipesButton);
        myRecipesButton.setOnClickListener(v ->
                startActivityForResult(new Intent(this, RecipeListActivity.class), REQUEST_TEMPLATE_PICK));

        focusMealNotes();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void setupMealTimeSpinner() {
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.carblookup_meal_time_any),
                        getString(R.string.carblookup_filter_breakfast),
                        getString(R.string.carblookup_filter_lunch),
                        getString(R.string.carblookup_filter_dinner),
                        getString(R.string.carblookup_filter_snack)});
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mealTimeSpinner.setAdapter(mealAdapter);
        mealTimeSpinner.setSelection(defaultMealTimeIndex());
    }

    private int defaultMealTimeIndex() {
        int hour = getCurrentHourOfDay();
        if (hour >= 5 && hour < 10) {
            return 1;
        }
        if (hour >= 11 && hour < 14) {
            return 2;
        }
        if (hour >= 17 && hour < 21) {
            return 3;
        }
        return 4;
    }

    protected int getCurrentHourOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getCurrentTimeMillis());
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    protected MealLogService createMealLogService(MealRepository mealRepository) {
        return new MealLogService(mealRepository);
    }

    protected MealRepository createMealRepository() {
        return new MealRepository(this);
    }

    protected RecipeRepository createRecipeRepository() {
        return new RecipeRepository(CarbLookupDatabase.getInstance(this));
    }

    protected FavoriteItemRepository createFavoriteItemRepository() {
        return new FavoriteItemRepository(this);
    }

    protected FoodDbSource createFoodDbSource() {
        return FoodDbSource.current();
    }

    private void focusMealNotes() {
        mealNotesEditText.requestFocus();
        mealNotesEditText.post(() -> mealNotesEditText.requestFocus());
    }

    private void scanBarcode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }
        launchBarcodeScanner();
    }

    private void launchBarcodeScanner() {
        try {
            new IntentIntegrator(this)
                    .setDesiredBarcodeFormats(IntentIntegrator.PRODUCT_CODE_TYPES)
                    .setPrompt(getString(R.string.carblookup_scan_food_prompt))
                    .setCaptureActivity(CarbLookupCaptureActivity.class)
                    .setOrientationLocked(false)
                    .setBeepEnabled(true)
                    .initiateScan();
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.carblookup_scanner_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchManualEntry() {
        currentMeal.beginNewItem();
        startActivityForResult(ProductDetailContract.manualEntry(this), REQUEST_PRODUCT_DETAIL);
    }

    private void launchItemSearch() {
        currentMeal.beginNewItem();
        startActivityForResult(new Intent(this, FoodSearchActivity.class), REQUEST_ITEM_SEARCH);
    }

    private void showCommonFoods() {
        new CommonFoodsPicker(this, createFoodDbSource(), this::launchStandardCarbItem).show();
    }

    private void launchStandardCarbItem(StandardCarbItem item) {
        currentMeal.beginNewItem();
        startActivityForResult(ProductDetailContract.forStandardCarbItem(this, item), REQUEST_PRODUCT_DETAIL);
    }

    private void editMealItem(int position) {
        if (!currentMeal.beginEditItem(position)) {
            return;
        }
        startActivityForResult(ProductDetailContract.forMealItem(this, currentMeal.getItem(position)),
                REQUEST_PRODUCT_DETAIL);
    }

    private void removeMealItem(int position) {
        MealItem removedItem = currentMeal.remove(position);
        if (removedItem == null) {
            return;
        }
        updateMealDisplay();
        Toast.makeText(this,
                getString(R.string.carblookup_removed_format, removedItem.productName),
                Toast.LENGTH_SHORT).show();
    }

    private void confirmAndSaveMeal() {
        if (currentMeal.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_no_items_added, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMealTimestamp() == null) {
            Toast.makeText(this, R.string.carblookup_invalid_meal_clock_time, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_confirm_log_title)
                .setMessage(getString(R.string.carblookup_confirm_log_meal_message, totalCarbs))
                .setPositiveButton(R.string.carblookup_log, (d, w) -> saveMeal())
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void saveMeal() {
        if (currentMeal.isEmpty()) {
            return;
        }
        Long mealTimestamp = selectedMealTimestamp();
        if (mealTimestamp == null) {
            Toast.makeText(this, R.string.carblookup_invalid_meal_clock_time, Toast.LENGTH_SHORT).show();
            return;
        }
        String mealName = getString(
            R.string.carblookup_meal_name_format,
            buildSelectedMealNamePrefix(),
            formatMealClockTime(mealTimestamp));
        String notes = mealNotesEditText.getText().toString().trim();
        mealLogService.logMeal(mealName, totalCarbs, currentMeal.getItems(),
                notes.isEmpty() ? null : notes, selectedMealTimeKey(), mealTimestamp);

        // Increment use count for any favourites used in this meal.
        for (MealItem item : currentMeal.getItems()) {
            if (item.favoriteId > 0) {
                favoriteRepository.incrementUseCount(item.favoriteId);
            }
        }

        currentMeal.clear();
        mealNotesEditText.setText("");
        resetMealClockTimeToNow();
        updateMealDisplay();
        Toast.makeText(this, getString(R.string.carblookup_meal_saved_format, mealName), Toast.LENGTH_SHORT).show();
    }

    private String buildSelectedMealNamePrefix() {
        int selected = mealTimeSpinner != null ? mealTimeSpinner.getSelectedItemPosition() : 0;
        if (selected <= 0 || selected >= MEAL_KEYS.length) {
            return getString(R.string.carblookup_meal_name_generic);
        }
        return mealTimeSpinner.getSelectedItem().toString();
    }

    private String selectedMealTimeKey() {
        int selected = mealTimeSpinner != null ? mealTimeSpinner.getSelectedItemPosition() : 0;
        return selected >= 0 && selected < MEAL_KEYS.length ? MEAL_KEYS[selected] : MEAL_KEYS[0];
    }

    private Long selectedMealTimestamp() {
        int[] mealClockTime = parseMealClockTime();
        if (mealClockTime == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(getCurrentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, mealClockTime[0]);
        calendar.set(Calendar.MINUTE, mealClockTime[1]);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private int[] parseMealClockTime() {
        if (mealClockTimeEditText == null) {
            return null;
        }
        Matcher matcher = MEAL_CLOCK_TIME_PATTERN.matcher(mealClockTimeEditText.getText().toString().trim());
        if (!matcher.matches()) {
            return null;
        }
        try {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return new int[]{hour, minute};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void resetMealClockTimeToNow() {
        if (mealClockTimeEditText != null) {
            mealClockTimeEditText.setText(formatMealClockTime(getCurrentTimeMillis()));
        }
    }

    private String formatMealClockTime(long timestamp) {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private void useTemplate() {
        Intent intent = new Intent(this, RecipeListActivity.class);
        intent.putExtra(RecipeListActivity.EXTRA_PICK_MODE, true);
        startActivityForResult(intent, REQUEST_TEMPLATE_PICK);
    }

    private void launchRecipeEditor() {
        startActivity(RecipeEditActivity.newRecipeWithItems(this,
                mealItemFactory.toRecipeItems(currentMeal.getItems())));
    }

    private void pickFavorite() {
        Intent intent = new Intent(this, FavoriteItemsActivity.class);
        intent.putExtra(FavoriteItemsActivity.EXTRA_PICK_MODE, true);
        startActivityForResult(intent, REQUEST_FAVORITE_PICK);
    }

    private void confirmClearMeal() {
        if (currentMeal.isEmpty() && mealNotesEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.carblookup_no_items_added, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_clear_meal_title)
                .setMessage(R.string.carblookup_clear_meal_message)
                .setPositiveButton(R.string.carblookup_clear, (dialog, which) -> clearCurrentMeal())
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void clearCurrentMeal() {
        currentMeal.clear();
        mealNotesEditText.setText("");
        resetMealClockTimeToNow();
        updateMealDisplay();
        Toast.makeText(this, R.string.carblookup_meal_cleared, Toast.LENGTH_SHORT).show();
    }

    private void showSaveAsTemplateDialog() {
        if (currentMeal.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_no_items_added, Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText nameInput = new EditText(this);
        nameInput.setHint(R.string.carblookup_template_name_hint);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameInput.setText(getString(R.string.carblookup_template_name_format,
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())));
        nameInput.selectAll();
        layout.addView(nameInput);

        EditText portionsInput = new EditText(this);
        portionsInput.setHint(R.string.carblookup_portions_label);
        portionsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portionsInput.setText(R.string.carblookup_integer_one_hint);
        layout.addView(portionsInput);

        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_save_as_template)
                .setView(layout)
                .setPositiveButton(R.string.carblookup_save_template, (dialog, which) -> saveAsTemplate(nameInput, portionsInput))
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void saveAsTemplate(EditText nameInput, EditText portionsInput) {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_template_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        int portions;
        try {
            portions = Integer.parseInt(portionsInput.getText().toString().trim());
            if (portions <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.carblookup_invalid_portions, Toast.LENGTH_SHORT).show();
            return;
        }

        Recipe template = new Recipe();
        template.name = name;
        template.portionCount = portions;
        template.notes = mealNotesEditText.getText().toString().trim();
        template.items = mealItemFactory.toRecipeItems(currentMeal.getItems());
        templateRepository.saveRecipe(template);
        Toast.makeText(this, R.string.carblookup_template_saved, Toast.LENGTH_SHORT).show();
    }

    private void viewMealHistory() {
        startActivityForResult(new Intent(this, MealHistoryActivity.class), REQUEST_MEAL_HISTORY);
    }

    private void updateMealDisplay() {
        totalCarbs = currentMeal.totalCarbs();
        mealItemAdapter.notifyDataSetChanged();
        carbsResultTextView.setText(String.format(Locale.getDefault(), getString(R.string.carblookup_carbs_format), totalCarbs));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                currentMeal.beginNewItem();
                startActivityForResult(ProductDetailContract.forBarcode(this, scanResult.getContents()),
                        REQUEST_PRODUCT_DETAIL);
            } else {
                Toast.makeText(this, R.string.carblookup_scan_cancelled, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PRODUCT_DETAIL && resultCode == RESULT_OK && data != null) {
            MealItem item = mealItemFactory.fromProductResult(data);
            if (currentMeal.addOrUpdate(item)) {
                Toast.makeText(this, R.string.carblookup_item_updated, Toast.LENGTH_SHORT).show();
            }
            updateMealDisplay();
        } else if (requestCode == REQUEST_TEMPLATE_PICK && resultCode == RESULT_OK && data != null) {
            long recipeId = data.getLongExtra(RecipeListActivity.EXTRA_RECIPE_ID, 0L);
            double portions = data.getDoubleExtra(RecipeDetailActivity.EXTRA_PORTIONS, 1.0);
            addTemplateToCurrentMeal(recipeId, portions);
        } else if (requestCode == REQUEST_FAVORITE_PICK && resultCode == RESULT_OK && data != null) {
            addFavoriteToCurrentMeal(data);
        } else if (requestCode == REQUEST_MEAL_HISTORY && resultCode == RESULT_OK && data != null) {
            long reuseMealId = data.getLongExtra(MealDetailActivity.EXTRA_REUSE_MEAL_ID, 0L);
            if (reuseMealId != 0L) {
                reuseMealItems(reuseMealId);
            }
        } else if (requestCode == REQUEST_ITEM_SEARCH && resultCode == RESULT_OK && data != null) {
            String barcode = data.getStringExtra(FoodSearchActivity.EXTRA_BARCODE);
            if (barcode != null && !barcode.trim().isEmpty()) {
                startActivityForResult(ProductDetailContract.forBarcode(this, barcode), REQUEST_PRODUCT_DETAIL);
            }
        }
    }

    private void addTemplateToCurrentMeal(long recipeId, double portions) {
        if (recipeId == 0L) {
            Toast.makeText(this, R.string.carblookup_invalid_recipe, Toast.LENGTH_SHORT).show();
            return;
        }
        if (portions <= 0.0) {
            Toast.makeText(this, R.string.carblookup_invalid_portions, Toast.LENGTH_SHORT).show();
            return;
        }
        PortionResult result = templateRepository.getCarbsBreakdownForPortions(recipeId, portions);
        if (result == null || !result.isRecipeFound()) {
            Toast.makeText(this, R.string.carblookup_recipe_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        int before = currentMeal.getItems().size();
        if (result.getItems() != null) {
            for (com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult itemResult : result.getItems()) {
                currentMeal.add(mealItemFactory.fromPortionItemResult(itemResult));
            }
        }
        if (currentMeal.getItems().size() > before) {
            templateRepository.incrementUseCount(recipeId);
            updateMealDisplay();
            Toast.makeText(this, getString(R.string.carblookup_template_added_format, result.getRecipeName()), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.carblookup_add_ingredient, Toast.LENGTH_SHORT).show();
        }
    }

    private void addFavoriteToCurrentMeal(Intent data) {
        String name = data.getStringExtra(FavoriteItemsActivity.EXTRA_PRODUCT_NAME);
        String brand = data.getStringExtra(FavoriteItemsActivity.EXTRA_BRAND);
        String barcode = data.getStringExtra(FavoriteItemsActivity.EXTRA_BARCODE);
        double carbsPer100g = data.getDoubleExtra(FavoriteItemsActivity.EXTRA_CARBS_PER_100G, 0.0);
        double portionGrams = data.getDoubleExtra(FavoriteItemsActivity.EXTRA_PORTION_GRAMS, 0.0);

        if (portionGrams <= 0) {
            currentMeal.beginNewItem();
            long favoriteId = data.getLongExtra(FavoriteItemsActivity.EXTRA_FAVORITE_ID, 0L);
            startActivityForResult(ProductDetailContract.forFavoriteWithoutPortion(this, name, brand,
                barcode, carbsPer100g, favoriteId), REQUEST_PRODUCT_DETAIL);
            return;
        }

        MealItem item = new MealItem();
        item.productName = name;
        item.brand = brand;
        item.barcode = barcode;
        item.carbsPer100g = carbsPer100g;
        item.portionGrams = portionGrams;
        item.carbsForPortion = CarbLookupCalculator.calculateCarbs(carbsPer100g, portionGrams);
        item.favoriteId = data.getLongExtra(FavoriteItemsActivity.EXTRA_FAVORITE_ID, 0L);
        currentMeal.add(item);
        updateMealDisplay();
    }

    private void reuseMealItems(long mealId) {
        MealSummary meal = mealRepository.getMealById(mealId);
        if (meal == null || meal.items == null || meal.items.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_no_item_details, Toast.LENGTH_SHORT).show();
            return;
        }
        currentMeal.addAll(mealItemFactory.copyMealItems(meal.items));
        updateMealDisplay();
        Toast.makeText(this, getString(R.string.carblookup_template_added_format, meal.name), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchBarcodeScanner();
            } else {
                Toast.makeText(this, R.string.without_camera_permission_cannot_scan_barcode,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
