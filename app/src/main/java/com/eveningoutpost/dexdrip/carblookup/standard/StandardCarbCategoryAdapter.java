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
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;

import java.util.List;

public class StandardCarbCategoryAdapter extends ArrayAdapter<StandardCarbCategory> {
    private final FoodDbSource source;

    public StandardCarbCategoryAdapter(@NonNull Context context,
            @NonNull List<StandardCarbCategory> categories, @NonNull FoodDbSource source) {
        super(context, 0, categories);
        this.source = source;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_standard_carb_category, parent, false);
        }
        StandardCarbCategory category = getItem(position);
        if (category == null) {
            return view;
        }

        TextView name = view.findViewById(R.id.standardFoodCategoryNameTextView);
        TextView count = view.findViewById(R.id.standardFoodCategoryCountTextView);
        int itemCount = StandardCarbRepository.forSourceAndCategory(source, category).size();

        name.setText(category.label());
        count.setText(getContext().getString(R.string.carblookup_group_count_format, itemCount));
        return view;
    }
}
