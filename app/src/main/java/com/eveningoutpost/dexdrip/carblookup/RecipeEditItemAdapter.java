package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;

import java.util.List;
import java.util.Locale;

class RecipeEditItemAdapter extends ArrayAdapter<RecipeItem> {

    interface EditListener  { void onEdit(RecipeItem item, int position); }
    interface RemoveListener { void onRemove(RecipeItem item, int position); }

    private final EditListener  editListener;
    private final RemoveListener removeListener;

    RecipeEditItemAdapter(Context context, List<RecipeItem> items,
                          EditListener editListener, RemoveListener removeListener) {
        super(context, 0, items);
        this.editListener   = editListener;
        this.removeListener = removeListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecipeItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_recipe_item_edit_row, parent, false);
        }

        TextView nameView   = convertView.findViewById(R.id.itemNameTextView);
        TextView detailView = convertView.findViewById(R.id.itemDetailTextView);
        ImageButton editBtn   = convertView.findViewById(R.id.editItemButton);
        ImageButton removeBtn = convertView.findViewById(R.id.removeItemButton);

        nameView.setText(item.productName);
        detailView.setText(String.format(Locale.getDefault(),
                "%.0fg  |  %.1fg carbs", item.itemWeightGrams, item.itemCarbsGrams));

        editBtn.setOnClickListener(v -> editListener.onEdit(item, position));
        removeBtn.setOnClickListener(v -> removeListener.onRemove(item, position));

        return convertView;
    }
}
