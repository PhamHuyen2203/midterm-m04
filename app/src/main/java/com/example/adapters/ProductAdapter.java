package com.example.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mcommercemobile04.R;
import com.example.models.Product;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProductAdapter
        extends RecyclerView.Adapter<
        ProductAdapter.ProductViewHolder
        > {

    public interface OnAddToCartListener {

        void onAddToCart(Product product);
    }

    private final List<Product> products =
            new ArrayList<>();

    private final OnAddToCartListener listener;

    private final NumberFormat currencyFormatter;

    public ProductAdapter(
            OnAddToCartListener listener
    ) {
        this.listener = listener;

        currencyFormatter =
                NumberFormat.getCurrencyInstance(
                        Locale.forLanguageTag("vi-VN")
                );

        currencyFormatter.setMaximumFractionDigits(0);
    }

    public void submitList(
            List<Product> newProducts
    ) {
        products.clear();

        if (newProducts != null) {
            products.addAll(newProducts);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View itemView =
                LayoutInflater.from(
                        parent.getContext()
                ).inflate(
                        R.layout.item_product,
                        parent,
                        false
                );

        return new ProductViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductViewHolder holder,
            int position
    ) {

        Product product =
                products.get(position);

        Context context =
                holder.itemView.getContext();

        holder.textViewProductInitial.setText(
                getProductInitial(
                        context,
                        product.getProductName()
                )
        );

        holder.textViewProductCategory.setText(
                product.getCategoryName()
        );

        holder.textViewProductName.setText(
                product.getProductName()
        );

        if (TextUtils.isEmpty(
                product.getDescription()
        )) {

            holder.textViewProductDescription
                    .setVisibility(View.GONE);

        } else {

            holder.textViewProductDescription
                    .setVisibility(View.VISIBLE);

            holder.textViewProductDescription.setText(
                    product.getDescription()
            );
        }

        String formattedPrice =
                currencyFormatter.format(
                        product.getPrice()
                );

        holder.textViewProductPrice.setText(
                context.getString(
                        R.string.product_price_format,
                        formattedPrice
                )
        );

        holder.textViewProductStock.setText(
                context.getString(
                        R.string.product_stock_format,
                        product.getStockQuantity()
                )
        );

        holder.textViewProductRating.setText(
                context.getString(
                        R.string.product_rating_format,
                        product.getAverageRating(),
                        product.getRatingCount()
                )
        );

        boolean isAvailable =
                product.getStockQuantity() > 0;

        holder.buttonAddToCart.setEnabled(
                isAvailable
        );

        holder.buttonAddToCart.setText(
                isAvailable
                        ? R.string.button_add_to_cart
                        : R.string.product_out_of_stock
        );

        holder.buttonAddToCart
                .setContentDescription(
                        context.getString(
                                R.string
                                        .add_product_accessibility,
                                product.getProductName()
                        )
                );

        holder.buttonAddToCart.setOnClickListener(
                view -> {

                    if (listener != null
                            && product
                            .getStockQuantity() > 0) {

                        listener.onAddToCart(
                                product
                        );
                    }
                }
        );
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String getProductInitial(
            Context context,
            String productName
    ) {

        if (TextUtils.isEmpty(productName)) {

            return context.getString(
                    R.string.product_initial_fallback
            );
        }

        int endIndex =
                productName.offsetByCodePoints(
                        0,
                        1
                );

        return productName
                .substring(0, endIndex)
                .toUpperCase(
                        Locale.getDefault()
                );
    }

    static final class ProductViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView textViewProductInitial;
        private final TextView textViewProductCategory;
        private final TextView textViewProductName;
        private final TextView textViewProductDescription;
        private final TextView textViewProductPrice;
        private final TextView textViewProductStock;
        private final TextView textViewProductRating;
        private final MaterialButton buttonAddToCart;

        private ProductViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            textViewProductInitial =
                    itemView.findViewById(
                            R.id.textViewProductInitial
                    );

            textViewProductCategory =
                    itemView.findViewById(
                            R.id.textViewProductCategory
                    );

            textViewProductName =
                    itemView.findViewById(
                            R.id.textViewProductName
                    );

            textViewProductDescription =
                    itemView.findViewById(
                            R.id.textViewProductDescription
                    );

            textViewProductPrice =
                    itemView.findViewById(
                            R.id.textViewProductPrice
                    );

            textViewProductStock =
                    itemView.findViewById(
                            R.id.textViewProductStock
                    );

            textViewProductRating =
                    itemView.findViewById(
                            R.id.textViewProductRating
                    );

            buttonAddToCart =
                    itemView.findViewById(
                            R.id.buttonAddToCart
                    );
        }
    }
}