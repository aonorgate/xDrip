package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeSummary;

import java.util.List;
import java.util.Locale;

public class RecipeSummaryAdapter extends ArrayAdapter<RecipeSummary> {

    public RecipeSummaryAdapter(Context context, List<RecipeSummary> recipes) {
        super(context, 0, recipes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecipeSummary recipe = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_recipe_row, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.recipeNameTextView);
        TextView detailView = convertView.findViewById(R.id.recipeDetailTextView);
        TextView carbsView = convertView.findViewById(R.id.recipeCarbsTextView);

        nameView.setText(recipe.name);
        detailView.setText(String.format(Locale.getDefault(),
            getContext().getString(R.string.carblookup_recipe_summary_format),
            recipe.itemCount,
            recipe.portionCount,
                mealTimeLabel(recipe.mealTime)));
        carbsView.setText(String.format(Locale.getDefault(), getContext().getString(R.string.carblookup_per_portion_format), recipe.carbsPerPortion));

        return convertView;
    }

    private String mealTimeLabel(String mealTime) {
        if (mealTime == null) return getContext().getString(R.string.carblookup_meal_time_any);
        switch (mealTime) {
            case "breakfast": return getContext().getString(R.string.carblookup_filter_breakfast);
            case "lunch":     return getContext().getString(R.string.carblookup_filter_lunch);
            case "dinner":    return getContext().getString(R.string.carblookup_filter_dinner);
            case "snack":     return getContext().getString(R.string.carblookup_filter_snack);
            default:          return getContext().getString(R.string.carblookup_meal_time_any);
        }
    }
}
