package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.model.PortionItemResult;

import java.util.List;
import java.util.Locale;

class RecipeDetailItemAdapter extends ArrayAdapter<PortionItemResult> {

    RecipeDetailItemAdapter(Context context, List<PortionItemResult> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PortionItemResult item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_recipe_detail_row, parent, false);
        }

        TextView nameView   = convertView.findViewById(R.id.detailItemNameTextView);
        TextView weightView = convertView.findViewById(R.id.detailItemWeightTextView);
        TextView carbsView  = convertView.findViewById(R.id.detailItemCarbsTextView);
        TextView glView     = convertView.findViewById(R.id.detailItemGlTextView);

        nameView.setText(item.getProductName());
        weightView.setText(String.format(Locale.getDefault(), "%.0fg", item.getScaledWeightGrams()));
        carbsView.setText(String.format(Locale.getDefault(), "%.1fg", item.getScaledCarbsGrams()));
        if (item.getGiEstimate() > 0) {
            glView.setText(String.format(Locale.getDefault(), glView.getContext().getString(R.string.carblookup_gl_item_format), item.getScaledGlEstimate()));
            glView.setTextColor(glView.getContext().getColor(R.color.carbsHighlight));
        } else {
            glView.setText(R.string.carblookup_gl_unknown);
            glView.setTextColor(android.graphics.Color.GRAY);
        }

        return convertView;
    }
}
