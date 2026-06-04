package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.eveningoutpost.dexdrip.R;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

public class MealSummaryAdapter extends ArrayAdapter<MealSummary> {
    private final DateFormat dateFormat;

    public MealSummaryAdapter(Context context, List<MealSummary> meals) {
        super(context, 0, meals);
        dateFormat = android.text.format.DateFormat.getMediumDateFormat(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MealSummary meal = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_meal_row, parent, false);
        }

        TextView nameTextView = convertView.findViewById(R.id.mealNameTextView);
        TextView detailTextView = convertView.findViewById(R.id.mealDetailTextView);
        TextView carbsTextView = convertView.findViewById(R.id.mealCarbsTextView);

        nameTextView.setText(meal.name);
        String metaText = String.format(Locale.getDefault(),
                getContext().getString(R.string.carblookup_meal_meta_format),
                dateFormat.format(meal.savedAt),
            meal.itemCount);
        String mealContext = mealContextText(meal);
        detailTextView.setText(mealContext.isEmpty() ? metaText
            : getContext().getString(R.string.carblookup_meal_meta_context_format, metaText, mealContext));
        carbsTextView.setText(String.format(Locale.getDefault(),
                getContext().getString(R.string.carblookup_grams_one_decimal_spaced_format),
                meal.totalCarbs));

        return convertView;
    }

    private String mealContextText(MealSummary meal) {
        String notes = meal.notes != null ? meal.notes.trim() : "";
        String mealTime = mealTimeLabel(meal.mealTime);
        if (!notes.isEmpty() && !mealTime.isEmpty()) {
            return getContext().getString(R.string.carblookup_meal_notes_meal_time_format, notes, mealTime);
        }
        if (!notes.isEmpty()) {
            return notes;
        }
        return mealTime;
    }

    private String mealTimeLabel(String mealTime) {
        if (mealTime == null) {
            return "";
        }
        switch (mealTime) {
            case "breakfast":
                return getContext().getString(R.string.carblookup_filter_breakfast);
            case "lunch":
                return getContext().getString(R.string.carblookup_filter_lunch);
            case "dinner":
                return getContext().getString(R.string.carblookup_filter_dinner);
            case "snack":
                return getContext().getString(R.string.carblookup_filter_snack);
            default:
                return "";
        }
    }
}