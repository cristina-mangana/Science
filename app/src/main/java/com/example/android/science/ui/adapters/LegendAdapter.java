package com.example.android.science.ui.adapters;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.science.R;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.ViewHolder> {
    private String[] mTopicNames;

    /**
     * Custom constructor
     */
    public LegendAdapter(String[] topicNames) {
        mTopicNames = topicNames;
    }

    // Provide a reference to the views for each data item.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.legend_label)
        TextView legendLabelTextView;
        @BindArray(R.array.topic_colors)
        int[] topicColors;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public LegendAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.legend_list_item, parent, false);
        return new LegendAdapter.ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull LegendAdapter.ViewHolder holder, int position) {
        // Set the label
        holder.legendLabelTextView.setText(mTopicNames[position]);
        // Set the color: https://tinyurl.com/yavqqta7
        Drawable[] drawables = holder.legendLabelTextView.getCompoundDrawablesRelative();
        Drawable wrappedDrawable = DrawableCompat.wrap(drawables[0]);
        DrawableCompat.setTint(wrappedDrawable, holder.topicColors[position]);
        holder.legendLabelTextView.setCompoundDrawablesRelative(wrappedDrawable, null, null,
                null);
    }

    // Return the size of the data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mTopicNames.length;
    }
}
