package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;

import java.util.List;
import java.util.Locale;

class MealItemAdapter extends ArrayAdapter<MealItem> {

    interface EditListener { void onEdit(MealItem item, int position); }
    interface RemoveListener { void onRemove(MealItem item, int position); }

    private final EditListener editListener;
    private final RemoveListener removeListener;

    MealItemAdapter(Context context, List<MealItem> items) {
        this(context, items, null, null);
    }

    MealItemAdapter(Context context, List<MealItem> items,
                    EditListener editListener, RemoveListener removeListener) {
        super(context, 0, items);
        this.editListener = editListener;
        this.removeListener = removeListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MealItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_meal_component_row, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.mealComponentNameTextView);
        TextView detailView = convertView.findViewById(R.id.mealComponentDetailTextView);
        TextView carbsView = convertView.findViewById(R.id.mealComponentCarbsTextView);
        ImageButton editButton = convertView.findViewById(R.id.editMealComponentButton);
        ImageButton removeButton = convertView.findViewById(R.id.removeMealComponentButton);

        nameView.setText(item.productName);

        StringBuilder detail = new StringBuilder();
        if (item.brand != null && !item.brand.trim().isEmpty()) {
            detail.append(item.brand.trim()).append(getContext().getString(R.string.carblookup_detail_separator));
        }
        detail.append(String.format(Locale.getDefault(), getContext().getString(R.string.carblookup_grams_format), item.portionGrams));
        if (item.glEstimate > 0) {
            detail.append(String.format(Locale.getDefault(), getContext().getString(R.string.carblookup_detail_gl_format), item.glEstimate));
        } else if (item.giEstimate > 0) {
            detail.append(String.format(Locale.getDefault(), getContext().getString(R.string.carblookup_detail_gi_format), item.giEstimate));
        }
        detailView.setText(detail.toString());
        carbsView.setText(String.format(Locale.getDefault(), getContext().getString(R.string.carblookup_grams_one_decimal_format), item.carbsForPortion));

        editButton.setVisibility(editListener == null ? View.GONE : View.VISIBLE);
        removeButton.setVisibility(removeListener == null ? View.GONE : View.VISIBLE);
        if (editListener != null) {
            editButton.setOnClickListener(v -> editListener.onEdit(item, position));
        }
        if (removeListener != null) {
            removeButton.setOnClickListener(v -> removeListener.onRemove(item, position));
        }

        return convertView;
    }
}