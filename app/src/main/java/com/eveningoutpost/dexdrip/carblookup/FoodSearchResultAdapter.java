package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.api.ProductData;

import java.util.List;

class FoodSearchResultAdapter extends ArrayAdapter<ProductData.Product> {
    FoodSearchResultAdapter(@NonNull Context context) {
        super(context, 0);
    }

    void setProducts(List<ProductData.Product> products) {
        clear();
        if (products != null) {
            addAll(products);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_food_search_result, parent, false);
        }
        ProductData.Product product = getItem(position);
        if (product == null) {
            return view;
        }

        TextView name = view.findViewById(R.id.foodSearchNameTextView);
        TextView brand = view.findViewById(R.id.foodSearchBrandTextView);
        TextView carbs = view.findViewById(R.id.foodSearchCarbsTextView);

        name.setText(displayName(product));
        String brandText = product.brands != null ? product.brands.trim() : "";
        brand.setText(brandText);
        brand.setVisibility(brandText.isEmpty() ? View.GONE : View.VISIBLE);
        if (product.nutriments != null && product.nutriments.carbohydrates100g > 0.0) {
            carbs.setText(getContext().getString(R.string.carblookup_food_search_carbs_format,
                    product.nutriments.carbohydrates100g));
        } else {
            carbs.setText(R.string.carblookup_food_search_carbs_unknown);
        }
        return view;
    }

    private String displayName(ProductData.Product product) {
        if (product.productName != null && !product.productName.trim().isEmpty()) {
            return product.productName;
        }
        if (product.productNameEn != null && !product.productNameEn.trim().isEmpty()) {
            return product.productNameEn;
        }
        return getContext().getString(R.string.carblookup_manual_item_name);
    }
}
