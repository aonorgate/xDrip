package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.RecipeRepository;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeSummary;
import com.eveningoutpost.dexdrip.models.JoH;

import java.util.ArrayList;
import java.util.List;

public class RecipeListActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "RECIPE_ID";
    public static final String EXTRA_PICK_MODE  = "PICK_MODE";   // true = pick for logging
    private static final int REQUEST_TEMPLATE_DETAIL = 1101;

    private RecipeRepository repo;
    private RecipeSummaryAdapter adapter;
    private List<RecipeSummary> displayedRecipes;
    private TextView emptyTextView;

    private EditText searchEditText;
    private Spinner  sortSpinner;
    private Spinner  mealTimeSpinner;

    private static final String[] SORT_KEYS = {"name", "most_used", "recent"};
    private static final String[] MEAL_KEYS = {"any", "breakfast", "lunch", "dinner", "snack"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_saved_templates);

        repo = new RecipeRepository(CarbLookupDatabase.getInstance(this));
        displayedRecipes = new ArrayList<>();
        adapter = new RecipeSummaryAdapter(this, displayedRecipes);

        ListView listView = findViewById(R.id.recipeListView);
        listView.setAdapter(adapter);
        emptyTextView = findViewById(R.id.emptyTextView);
        listView.setEmptyView(emptyTextView);

        searchEditText   = findViewById(R.id.searchEditText);
        sortSpinner      = findViewById(R.id.sortSpinner);
        mealTimeSpinner  = findViewById(R.id.mealTimeSpinner);

        // Sort spinner
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.carblookup_sort_az), getString(R.string.carblookup_sort_most_used), getString(R.string.carblookup_sort_recent)});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        // Meal-time filter spinner
        ArrayAdapter<String> mealAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.carblookup_filter_all), getString(R.string.carblookup_filter_breakfast), getString(R.string.carblookup_filter_lunch), getString(R.string.carblookup_filter_dinner), getString(R.string.carblookup_filter_snack)});
        mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mealTimeSpinner.setAdapter(mealAdapter);

        AdapterView.OnItemSelectedListener refreshListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { loadRecipes(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        sortSpinner.setOnItemSelectedListener(refreshListener);
        mealTimeSpinner.setOnItemSelectedListener(refreshListener);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { loadRecipes(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        boolean pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            RecipeSummary recipe = displayedRecipes.get(position);
            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.id);
            intent.putExtra(RecipeDetailActivity.EXTRA_PICK_MODE, pickMode);
            startActivityForResult(intent, REQUEST_TEMPLATE_DETAIL);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            RecipeSummary recipe = displayedRecipes.get(position);
            showContextMenu(recipe);
            return true;
        });

        findViewById(R.id.fabNewRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(this, RecipeEditActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TEMPLATE_DETAIL && resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private void loadRecipes() {
        String query = searchEditText.getText().toString().trim();
        String sortKey = SORT_KEYS[sortSpinner.getSelectedItemPosition()];
        String mealKey = MEAL_KEYS[mealTimeSpinner.getSelectedItemPosition()];

        List<RecipeSummary> results;
        if (!query.isEmpty()) {
            results = repo.searchRecipes(query, sortKey, mealKey);
        } else {
            results = repo.getAllRecipes(sortKey, mealKey);
        }

        displayedRecipes.clear();
        displayedRecipes.addAll(results);
        adapter.notifyDataSetChanged();

        if (results.isEmpty() && !query.isEmpty()) {
            int total = repo.getRecipeCount();
            emptyTextView.setText(getString(R.string.carblookup_no_matches_format, total));
        } else if (results.isEmpty()) {
            emptyTextView.setText(R.string.carblookup_no_recipes);
        }
    }

    private void showContextMenu(RecipeSummary recipe) {
        String[] options = {getString(R.string.carblookup_context_edit), getString(R.string.carblookup_context_duplicate), getString(R.string.carblookup_context_delete)};
        new AlertDialog.Builder(this)
                .setTitle(recipe.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, RecipeEditActivity.class);
                        intent.putExtra(RecipeEditActivity.EXTRA_RECIPE_ID, recipe.id);
                        startActivity(intent);
                    } else if (which == 1) {
                        showDuplicateDialog(recipe);
                    } else {
                        confirmDelete(recipe);
                    }
                })
                .show();
    }

    private void showDuplicateDialog(RecipeSummary recipe) {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setText(getString(R.string.carblookup_copy_of_format, recipe.name));
        nameInput.selectAll();
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        nameInput.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.carblookup_duplicate_recipe_title))
                .setView(nameInput)
                .setPositiveButton(getString(R.string.carblookup_context_duplicate), (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        repo.duplicateRecipe(recipe.id, name);
                        loadRecipes();
                        Toast.makeText(this, R.string.carblookup_recipe_duplicated, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(getString(R.string.carblookup_cancel), null)
                .show();
    }

    private void confirmDelete(RecipeSummary recipe) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.carblookup_delete_recipe_title))
                .setMessage(getString(R.string.carblookup_delete_recipe_message, recipe.name))
                .setPositiveButton(getString(R.string.carblookup_context_delete), (d, w) -> {
                    repo.deleteRecipe(recipe.id);
                    loadRecipes();
                    Toast.makeText(this, R.string.carblookup_recipe_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.carblookup_cancel), null)
                .show();
    }
}
