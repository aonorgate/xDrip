package com.eveningoutpost.dexdrip.carblookup.standard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.R;

import java.util.List;

public class StandardCarbItemAdapter extends ArrayAdapter<StandardCarbItem> {
    public StandardCarbItemAdapter(@NonNull Context context, @NonNull List<StandardCarbItem> items) {
        super(context, 0, items);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_standard_carb_food, parent, false);
        }
        StandardCarbItem item = getItem(position);
        if (item == null) {
            return view;
        }

        TextView name = view.findViewById(R.id.standardFoodNameTextView);
        TextView carbs = view.findViewById(R.id.standardFoodCarbsTextView);
        TextView small = view.findViewById(R.id.standardFoodSmallTextView);
        TextView medium = view.findViewById(R.id.standardFoodMediumTextView);
        TextView large = view.findViewById(R.id.standardFoodLargeTextView);

        name.setText(item.name);
        carbs.setText(getContext().getString(R.string.carblookup_standard_food_carbs_unit_format,
            item.carbsPer100PortionUnits(), item.portionUnit));
        small.setText(getContext().getString(R.string.carblookup_portion_small_unit_format,
            formatAmount(item.displayAmountForGrams(item.smallPortionGrams)), item.portionUnit));
        medium.setText(getContext().getString(R.string.carblookup_portion_medium_unit_format,
            formatAmount(item.displayAmountForGrams(item.mediumPortionGrams)), item.portionUnit));
        large.setText(getContext().getString(R.string.carblookup_portion_large_unit_format,
            formatAmount(item.displayAmountForGrams(item.largePortionGrams)), item.portionUnit));
        return view;
    }

        private String formatAmount(double value) {
            if (Math.abs(value - Math.rint(value)) < 0.000001) {
            return String.format(java.util.Locale.US, "%.0f", value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
        }
}