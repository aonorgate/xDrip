package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;

import java.text.DateFormat;
import java.util.Locale;

public class MealDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MEAL_ID = "MEAL_ID";
    public static final String EXTRA_REUSE_MEAL_ID = "REUSE_MEAL_ID";

    private MealRepository mealRepository;
    private MealSummary meal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_detail);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_meal_details);

        long mealId = getIntent().getLongExtra(EXTRA_MEAL_ID, 0L);
        if (mealId == 0L) {
            Toast.makeText(this, R.string.carblookup_invalid_meal, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mealRepository = createMealRepository();
        meal = mealRepository.getMealById(mealId);
        if (meal == null) {
            Toast.makeText(this, R.string.carblookup_meal_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView nameView = findViewById(R.id.mealDetailNameTextView);
        TextView carbsView = findViewById(R.id.mealDetailCarbsTextView);
        TextView notesView = findViewById(R.id.mealDetailNotesTextView);
        ListView itemListView = findViewById(R.id.mealDetailListView);
        itemListView.setEmptyView(findViewById(R.id.emptyMealDetailTextView));

        String dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(meal.savedAt);
        nameView.setText(meal.name + ", " + dateStr);
        carbsView.setText(String.format(Locale.getDefault(),
                getString(R.string.carblookup_meal_detail_carbs_items_format),
                meal.totalCarbs, meal.itemCount));
        if (meal.notes != null && !meal.notes.trim().isEmpty()) {
            notesView.setText(meal.notes);
            notesView.setVisibility(View.VISIBLE);
        } else {
            notesView.setVisibility(View.GONE);
        }
        itemListView.setAdapter(new MealItemAdapter(this, meal.items));

        ImageButton addToMealButton = findViewById(R.id.addToMealButton);
        addToMealButton.setOnClickListener(v -> reuseMeal());

        ImageButton deleteMealButton = findViewById(R.id.deleteMealButton);
        deleteMealButton.setOnClickListener(v -> confirmDeleteMeal());
    }

    protected MealRepository createMealRepository() {
        return new MealRepository(this);
    }

    private void reuseMeal() {
        if (meal == null || meal.items == null || meal.items.isEmpty()) {
            Toast.makeText(this, R.string.carblookup_no_item_details, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_REUSE_MEAL_ID, meal.id);
        setResult(RESULT_OK, result);
        finish();
    }

    private void confirmDeleteMeal() {
        if (meal == null) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_delete_meal_title)
                .setMessage(getString(R.string.carblookup_delete_meal_message, meal.name))
                .setPositiveButton(R.string.carblookup_context_delete, (dialog, which) -> deleteMeal())
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void deleteMeal() {
        if (meal == null) {
            return;
        }
        deleteAssociatedTreatment(meal);
        mealRepository.deleteMeal(meal.id);
        Toast.makeText(this, R.string.carblookup_meal_deleted, Toast.LENGTH_SHORT).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    protected void deleteAssociatedTreatment(MealSummary meal) {
        if (meal.treatmentUuid != null && !meal.treatmentUuid.isEmpty()) {
            Treatments.delete_by_uuid(meal.treatmentUuid, true);
        } else {
            Treatments.delete_by_timestamp(meal.savedAt, 1500, true);
        }
    }
}