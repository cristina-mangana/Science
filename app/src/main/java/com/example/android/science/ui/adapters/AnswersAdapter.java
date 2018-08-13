package com.example.android.science.ui.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.science.R;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Cristina on 12/08/2018.
 * This adapter provides access to the items in the data set, creates views for
 * items, and replaces the content of some of the views with new data when the original item
 * is no longer visible.
 */
public class AnswersAdapter extends RecyclerView.Adapter<AnswersAdapter.ViewHolder> {
    private List<String> mAnswers;
    private static int mCorrectPosition;
    private static AnswersAdapterListener mOnClickListener;

    // Handle button click
    public interface AnswersAdapterListener {
        void OnClick(View v, int position);
    }

    /**
     * Custom constructor
     */
    public AnswersAdapter(List<String> answers, int correctPosition,
                          AnswersAdapterListener listener) {
        mAnswers = answers;
        mCorrectPosition = correctPosition;
        mOnClickListener = listener;
    }

    // Provide a reference to the views for each data item.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.answer_card)
        CardView answerCard;
        @BindView(R.id.tv_answer_name)
        TextView answerTextView;
        @BindColor(R.color.colorCorrect) int colorCorrect;
        @BindColor(R.color.colorIncorrect) int colorIncorrect;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.OnClick(v, getAdapterPosition());
                    if (getAdapterPosition() == mCorrectPosition) {
                        answerCard.setCardBackgroundColor(colorCorrect);
                    } else {
                        answerCard.setCardBackgroundColor(colorIncorrect);
                    }
                }
            });
        }
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public AnswersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.answers_list_item, parent, false);
        // Set the view height
        int multiplier = parent.getResources().getInteger(R.integer.column_multiplier);
        ViewGroup.LayoutParams params = itemView.getLayoutParams();
        params.height = parent.getHeight() / 2 * multiplier
                - (int) parent.getResources().getDimension(R.dimen.marginSmall) * 2;
        return new AnswersAdapter.ViewHolder(itemView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull AnswersAdapter.ViewHolder holder, int position) {
        // Set the answer
        holder.answerTextView.setText(Html.fromHtml(mAnswers.get(position)));
    }

    // Return the size of the data set (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mAnswers.size();
    }
}
