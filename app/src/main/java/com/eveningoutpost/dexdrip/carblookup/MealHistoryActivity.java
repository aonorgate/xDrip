package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.ArrayList;
import java.util.List;

public class MealHistoryActivity extends AppCompatActivity {

    private static final int REQUEST_MEAL_DETAIL = 2001;
    static final String PREF_HISTORY_RETENTION_DAYS = "carblookup_history_retention_days";
    private static final int DEFAULT_HISTORY_RETENTION_DAYS = 90;
    private static final int[] HISTORY_RETENTION_DAYS = {30, 90, 180};
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private MealRepository mealRepository;
    private MealSummaryAdapter adapter;
    private List<MealSummary> meals;
    private Spinner historyRetentionSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_history);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_meal_history);

        mealRepository = createMealRepository();
        ListView listView = findViewById(R.id.mealListView);
        listView.setEmptyView(findViewById(R.id.emptyTextView));
        historyRetentionSpinner = findViewById(R.id.historyRetentionSpinner);

        meals = new ArrayList<>();
        adapter = new MealSummaryAdapter(this, meals);
        listView.setAdapter(adapter);
        setupHistoryRetentionSpinner();
        loadMeals();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            MealSummary meal = meals.get(position);
            Intent intent = new Intent(this, MealDetailActivity.class);
            intent.putExtra(MealDetailActivity.EXTRA_MEAL_ID, meal.id);
            startActivityForResult(intent, REQUEST_MEAL_DETAIL);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            MealSummary meal = meals.get(position);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.carblookup_delete_meal_title)
                    .setMessage(getString(R.string.carblookup_delete_meal_message, meal.name))
                    .setPositiveButton(R.string.carblookup_context_delete, (d, w) -> {
                        if (meal.treatmentUuid != null && !meal.treatmentUuid.isEmpty()) {
                            Treatments.delete_by_uuid(meal.treatmentUuid, true);
                        } else {
                            Treatments.delete_by_timestamp(meal.savedAt, 1500, true);
                        }
                        mealRepository.deleteMeal(meal.id);
                        loadMeals();
                        Toast.makeText(this, R.string.carblookup_meal_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.carblookup_cancel, null)
                    .show();
            return true;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEAL_DETAIL && resultCode == RESULT_OK && data != null) {
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMeals();
    }

    protected MealRepository createMealRepository() {
        return new MealRepository(this);
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private void setupHistoryRetentionSpinner() {
        ArrayAdapter<String> retentionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.carblookup_history_retention_30_days),
                        getString(R.string.carblookup_history_retention_90_days),
                        getString(R.string.carblookup_history_retention_180_days)});
        retentionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        historyRetentionSpinner.setAdapter(retentionAdapter);
        historyRetentionSpinner.setSelection(retentionIndexForDays(selectedRetentionDays()));
        historyRetentionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                int days = HISTORY_RETENTION_DAYS[Math.max(0, Math.min(position, HISTORY_RETENTION_DAYS.length - 1))];
                if (days != selectedRetentionDays()) {
                    Pref.setString(PREF_HISTORY_RETENTION_DAYS, String.valueOf(days));
                    loadMeals();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadMeals() {
        if (meals == null || adapter == null) {
            return;
        }
        meals.clear();
        meals.addAll(mealRepository.getMealsSince(historyCutoffTimestamp()));
        adapter.notifyDataSetChanged();
    }

    private long historyCutoffTimestamp() {
        return getCurrentTimeMillis() - selectedRetentionDays() * DAY_MS;
    }

    private int selectedRetentionDays() {
        int days = Pref.getStringToInt(PREF_HISTORY_RETENTION_DAYS, DEFAULT_HISTORY_RETENTION_DAYS);
        return isSupportedRetention(days) ? days : DEFAULT_HISTORY_RETENTION_DAYS;
    }

    private int retentionIndexForDays(int days) {
        for (int i = 0; i < HISTORY_RETENTION_DAYS.length; i++) {
            if (HISTORY_RETENTION_DAYS[i] == days) {
                return i;
            }
        }
        return retentionIndexForDays(DEFAULT_HISTORY_RETENTION_DAYS);
    }

    private boolean isSupportedRetention(int days) {
        for (int retentionDays : HISTORY_RETENTION_DAYS) {
            if (retentionDays == days) {
                return true;
            }
        }
        return false;
    }
}