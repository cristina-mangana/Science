package com.example.android.science.ui.adapters;

import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.science.R;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Cristina on 11/08/2018.
 * This adapter provides access to the items in the data set, creates views for
 * items, and replaces the content of some of the views with new data when the original item
 * is no longer visible.
 */
public class TopicsAdapter extends RecyclerView.Adapter<TopicsAdapter.ViewHolder> {
    private String[] mTopicNames;
    private static TopicsAdapterListener mOnClickListener;

    // Handle button click
    public interface TopicsAdapterListener {
        void OnClick(View v, int position);
    }

    /**
     * Custom constructor
     */
    public TopicsAdapter(String[] topicNames, TopicsAdapterListener listener) {
        mTopicNames = topicNames;
        mOnClickListener = listener;
    }

    // Provide a reference to the views for each data item.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.topic_card) CardView topicCard;
        @BindView(R.id.tv_topic_name) TextView topicNameTextView;
        @BindView(R.id.iv_topic_image) ImageView topicImageView;
        @BindArray(R.array.topic_colors) int[] topicColors;
        @BindArray(R.array.topic_icons) TypedArray topicIcons;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.OnClick(v, getAdapterPosition());
                }
            });
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public TopicsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.topics_list_item, parent, false);
        // Set the view dimensions
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        params.height = parent.getWidth() / parent.getResources().getInteger(R.integer.column_number)
                - (int) parent.getResources().getDimension(R.dimen.marginSmall) * 2;
        return new ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull TopicsAdapter.ViewHolder holder, int position) {
        // Set the title
        holder.topicNameTextView.setText(mTopicNames[position]);
        // Set the color
        holder.topicCard.setCardBackgroundColor(holder.topicColors[position]);
        // Set the icon
        holder.topicImageView.setImageResource(holder.topicIcons.getResourceId(position, 0));
    }

    // Return the size of the data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mTopicNames.length;
    }
}
