package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;
import com.eveningoutpost.dexdrip.carblookup.model.PortionResult;
import com.eveningoutpost.dexdrip.carblookup.model.Recipe;
import com.eveningoutpost.dexdrip.models.JoH;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "RECIPE_ID";
    public static final String EXTRA_PICK_MODE = "PICK_MODE";
    public static final String EXTRA_PORTIONS = "PORTIONS";

    private RecipeRepository repo;
    private MealLogService mealLogService;
    private MealItemFactory mealItemFactory;
    private long recipeId;

    private TextView recipeNameTextView;
    private EditText portionsEditText;
    private TextView totalCarbsSummaryTextView;
    private TextView glEstimateTextView;
    private TextView notesTextView;
    private RecipeDetailItemAdapter itemAdapter;
    private List<PortionItemResult> displayedItems;
    private PortionResult currentResult;
    private Recipe recipeData;
    private boolean pickMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_template);

        repo = createRecipeRepository();
        mealItemFactory = createMealItemFactory();
        mealLogService = createMealLogService(createMealRepository());
        recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, 0L);
        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        if (recipeId == 0) {
            Toast.makeText(this, R.string.carblookup_invalid_recipe, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipeNameTextView      = findViewById(R.id.recipeDetailNameTextView);
        portionsEditText        = findViewById(R.id.portionsEditText);
        totalCarbsSummaryTextView = findViewById(R.id.totalCarbsSummaryTextView);
        glEstimateTextView      = findViewById(R.id.glEstimateTextView);
        notesTextView           = findViewById(R.id.recipeNotesDetailTextView);
        ListView itemListView   = findViewById(R.id.detailItemListView);
        Button logMealButton    = findViewById(R.id.logMealButton);
        Button addToCurrentMealButton = findViewById(R.id.addToCurrentMealButton);
        ImageButton deleteRecipeButton = findViewById(R.id.deleteRecipeButton);
        findViewById(R.id.recipeDetailRoot).requestFocus();

        displayedItems = new ArrayList<>();
        itemAdapter    = new RecipeDetailItemAdapter(this, displayedItems);
        itemListView.setAdapter(itemAdapter);

        portionsEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                refreshBreakdown();
            }
        });

        logMealButton.setOnClickListener(v -> confirmAndLogMeal());
        addToCurrentMealButton.setOnClickListener(v -> addToCurrentMeal());
        deleteRecipeButton.setOnClickListener(v -> confirmDeleteRecipe());

        loadInitialData();
    }

    protected RecipeRepository createRecipeRepository() {
        return new RecipeRepository(CarbLookupDatabase.getInstance(this));
    }

    protected MealRepository createMealRepository() {
        return new MealRepository(this);
    }

    protected MealItemFactory createMealItemFactory() {
        return new MealItemFactory();
    }

    protected MealLogService createMealLogService(MealRepository mealRepository) {
        return new MealLogService(mealRepository);
    }

    private void loadInitialData() {
        recipeData = repo.getRecipeById(recipeId);
        if (recipeData != null && recipeData.notes != null && !recipeData.notes.isEmpty()) {
            notesTextView.setText(recipeData.notes);
            notesTextView.setVisibility(android.view.View.VISIBLE);
        } else {
            notesTextView.setVisibility(android.view.View.GONE);
        }
        portionsEditText.setText(getString(R.string.carblookup_integer_one_hint));
        refreshBreakdown();
    }

    private void refreshBreakdown() {
        double portions;
        try {
            portions = Double.parseDouble(portionsEditText.getText().toString());
            if (portions <= 0) {
                clearBreakdownState();
                return;
            }
        } catch (NumberFormatException e) {
            clearBreakdownState();
            return;
        }

        currentResult = repo.getCarbsBreakdownForPortions(recipeId, portions);

        if (!currentResult.isRecipeFound()) {
            Toast.makeText(this, R.string.carblookup_recipe_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipeNameTextView.setText(currentResult.getRecipeName());

        totalCarbsSummaryTextView.setText(String.format(Locale.getDefault(),
            getString(R.string.carblookup_carbs_format),
            currentResult.getTotalCarbsGrams()));

        if (!currentResult.isGlPartial() || currentResult.getTotalGlEstimate() > 0) {
            String glText = String.format(Locale.getDefault(),
                    getString(R.string.carblookup_gl_format), currentResult.getTotalGlEstimate());
            if (currentResult.isGlPartial()) glText += getString(R.string.carblookup_gl_partial_suffix);
            glEstimateTextView.setText(glText);
            glEstimateTextView.setVisibility(android.view.View.VISIBLE);
        } else {
            glEstimateTextView.setVisibility(android.view.View.GONE);
        }

        displayedItems.clear();
        if (currentResult.getItems() != null) {
            displayedItems.addAll(currentResult.getItems());
        }
        itemAdapter.notifyDataSetChanged();
    }

    private void clearBreakdownState() {
        currentResult = null;
        totalCarbsSummaryTextView.setText("");
        glEstimateTextView.setVisibility(android.view.View.GONE);
        displayedItems.clear();
        itemAdapter.notifyDataSetChanged();
    }

    private void confirmAndLogMeal() {
        if (currentResult == null || !currentResult.isRecipeFound()) return;

        double carbs = currentResult.getTotalCarbsGrams();
        if (carbs <= 0) {
            Toast.makeText(this, R.string.carblookup_no_carbs_to_log, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_confirm_log_title)
                .setMessage(getString(R.string.carblookup_confirm_log_meal_message, carbs))
                .setPositiveButton(R.string.carblookup_log, (d, w) -> logMeal())
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void logMeal() {
        if (currentResult == null || !currentResult.isRecipeFound()) return;

        double carbs = currentResult.getTotalCarbsGrams();

        mealLogService.logMeal(buildLoggedMealName(), carbs, buildMealItems());
        repo.incrementUseCount(recipeId);

        Toast.makeText(this,
                String.format(Locale.getDefault(), getString(R.string.carblookup_logged_format), carbs),
                Toast.LENGTH_SHORT).show();
        finish();
    }

    private void addToCurrentMeal() {
        if (currentResult == null || !currentResult.isRecipeFound()) return;
        Intent result = new Intent();
        result.putExtra(RecipeListActivity.EXTRA_RECIPE_ID, recipeId);
        result.putExtra(EXTRA_PORTIONS, currentResult.getNumberOfPortions());
        setResult(RESULT_OK, result);
        finish();
    }

    private void confirmDeleteRecipe() {
        String recipeName = recipeData != null && recipeData.name != null
                ? recipeData.name : getString(R.string.carblookup_title_template);
        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_delete_recipe_title)
                .setMessage(getString(R.string.carblookup_delete_recipe_message, recipeName))
                .setPositiveButton(R.string.carblookup_context_delete, (dialog, which) -> deleteRecipe())
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void deleteRecipe() {
        repo.deleteRecipe(recipeId);
        Toast.makeText(this, R.string.carblookup_recipe_deleted, Toast.LENGTH_SHORT).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    private String buildLoggedMealName() {
        double portions = currentResult.getNumberOfPortions();
        return portions == 1.0
                ? currentResult.getRecipeName()
                : String.format(Locale.getDefault(), getString(R.string.carblookup_recipe_portions_name_format), currentResult.getRecipeName(), portions);
    }

    private List<MealItem> buildMealItems() {
        List<MealItem> items = new ArrayList<>();
        if (currentResult.getItems() != null) {
            for (PortionItemResult itemResult : currentResult.getItems()) {
                items.add(mealItemFactory.fromPortionItemResult(itemResult));
            }
        }
        return items;
    }
}
