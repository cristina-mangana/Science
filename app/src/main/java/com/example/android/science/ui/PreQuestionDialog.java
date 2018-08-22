package com.example.android.science.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.science.R;
import com.example.android.science.model.Question;
import com.example.android.science.utilities.DatabaseUtils;
import com.example.android.science.utilities.QueryUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Cristina on 11/08/2018.
 * Fragment used to display detailed information about the selected topic and ask for confirmation
 * to start the game
 */
public class PreQuestionDialog extends DialogFragment {
    private static final String LOG_TAG = PreQuestionDialog.class.getSimpleName();

    private static final String POSITION_KEY = "position";
    public static final String QUESTIONS_KEY = "questions";
    private static final String PICKER_KEY = "picker";
    public static final String CATEGORY_KEY = "category";

    /* Request urls */
    private static final String BASE_URL = "https://opentdb.com/api.php?amount=10&category=";
    private String[] categoriesArray = {"17", "19", "18", "27"};
    private static String mCurrentCategory;
    private static String mCurrentCategoryName;
    private static final String DIFFICULTY_PATH = "&difficulty=";

    // Binding views and resources
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.topic_card)
    CardView mTopicCard;
    @BindView(R.id.tv_topic_name)
    TextView mTopicNameTextView;
    @BindView(R.id.tv_topic_subtitle)
    TextView mTopicSubtitleTextView;
    @BindView(R.id.iv_topic_image)
    ImageView mTopicImageView;
    @BindView(R.id.play_button)
    Button mPlayButton;
    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingIndicator;
    @BindView(R.id.difficulty_picker)
    com.shawnlin.numberpicker.NumberPicker mPicker;
    @BindArray(R.array.topic_names)
    String[] topicNames;
    @BindArray(R.array.topic_subtitles)
    String[] topicSubtitles;
    @BindArray(R.array.topic_colors)
    int[] topicColors;
    @BindArray(R.array.topic_colors_button)
    int[] topicColorsComplementary;
    @BindArray(R.array.topic_colors_button_pressed)
    int[] topicColorsComplementaryPressed;
    @BindArray(R.array.topic_icons)
    TypedArray topicIcons;
    @BindArray(R.array.difficulty_levels)
    String[] difficultyLevels;

    // Mandatory empty constructor
    public PreQuestionDialog() {
    }

    /**
     * Constructor method which allows to pass information in the construction.
     *
     * @param position is the index of the item in the RecyclerView starting the fragment
     * @return a {@link PreQuestionDialog} object
     */
    public static PreQuestionDialog getInstance(int position) {
        PreQuestionDialog object = new PreQuestionDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(POSITION_KEY, position);
        object.setArguments(bundle);

        return object;
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.pre_question_dialog, container);
        ButterKnife.bind(this, rootView);

        // Get the index of the item starting the fragment
        int position = getArguments().getInt(POSITION_KEY);

        // Set category
        mCurrentCategory = categoriesArray[position];
        mCurrentCategoryName = DatabaseUtils.categoryNames[position];

        // Set toolbar
        ((AppCompatActivity) getActivity()).setSupportActionBar(mToolbar);
        // Hide the title
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Enable menu
        setHasOptionsMenu(true);

        // Set topic name
        mTopicNameTextView.setText(topicNames[position]);

        // Set topic subtitle
        mTopicSubtitleTextView.setText(topicSubtitles[position]);

        // Set topic color
        mTopicCard.setCardBackgroundColor(topicColors[position]);

        // Set button color. Help from https://tinyurl.com/ycwawljd
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused}, // focused
                new int[]{android.R.attr.state_pressed}, // pressed
                new int[]{android.R.attr.state_enabled, -android.R.attr.state_pressed}  // enabled
        };

        int[] colors = new int[]{
                topicColorsComplementaryPressed[position],
                topicColorsComplementaryPressed[position],
                topicColorsComplementary[position]
        };


        ((AppCompatButton) mPlayButton).setSupportBackgroundTintList(new ColorStateList(states,
                colors));

        // Set topic icon
        mTopicImageView.setImageResource(topicIcons.getResourceId(position, 0));

        // Set difficulty level picker
        mPicker.setMaxValue(difficultyLevels.length);
        mPicker.setMinValue(1);
        mPicker.setDisplayedValues(difficultyLevels);
        mPicker.setDividerColor(topicColorsComplementary[position]);
        if (savedInstanceState != null) {
            mPicker.setValue(savedInstanceState.getInt(PICKER_KEY));
        } else {
            mPicker.setValue(2);
        }

        // Progress bar color: https://tinyurl.com/yatgcjmv
        if (mLoadingIndicator.getIndeterminateDrawable() != null) {
            mLoadingIndicator.getIndeterminateDrawable()
                    .setColorFilter(topicColorsComplementary[position],
                            android.graphics.PorterDuff.Mode.SRC_IN);
        }

        return rootView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Remove the default title: https://developer.android.com/guide/topics/ui/dialogs
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Help from: https://tinyurl.com/y9a9h2o4
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme);
        return dialog;
    }

    // Change dialog size: https://tinyurl.com/yb669qju
    @Override
    public void onResume() {
        super.onResume();
        // Set dimensions
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int width = screenWidth - (int) getResources().getDimension(R.dimen.bigSeparation) * 2;
        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // Inflate toolbar menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dialog_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_close:
                dismiss();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.play_button)
    public void playGame() {
        // Show loading indicator
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.GONE);
        String difficulty = difficultyLevels[mPicker.getValue() - 1];
        String requestUrl = BASE_URL + mCurrentCategory + DIFFICULTY_PATH + difficulty;
        Log.d(LOG_TAG, "Request URL: " + requestUrl);
        new QueryTask(this).execute(requestUrl);
    }

    // Avoid leaks: https://tinyurl.com/ya2pvjwk
    public static class QueryTask extends AsyncTask<String, Void, List<Question>> {

        private WeakReference<DialogFragment> appReference;

        QueryTask(DialogFragment dialogFragment) {
            appReference = new WeakReference<>(dialogFragment);
        }

        @Override
        protected List<Question> doInBackground(String[] strings) {
            String requestUrl = strings[0];
            if (requestUrl == null) {
                return null;
            }
            // Perform the HTTP request and process the response. Return the list
            return QueryUtils.fetchQuestionsListData(requestUrl);
        }

        @Override
        protected void onPostExecute(List<Question> questions) {
            DialogFragment dialogFragment = appReference.get();
            Context context = dialogFragment.getContext();
            if (context != null) {
                // Check if the list is null (network fail), else check if it is empty (API fail),
                // else open question activity
                if (questions == null) {
                    Toast.makeText(context, context.getString(R.string.error), Toast.LENGTH_LONG)
                            .show();
                } else if (questions.size() == 0) {
                    Toast.makeText(context, context.getString(R.string.error_api), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Intent openQuestionActivityIntent = new Intent(context, QuestionActivity.class);
                    openQuestionActivityIntent.putParcelableArrayListExtra(QUESTIONS_KEY,
                            (ArrayList<Question>) questions);
                    openQuestionActivityIntent.putExtra(CATEGORY_KEY, mCurrentCategoryName);
                    context.startActivity(openQuestionActivityIntent);
                }
                dialogFragment.dismiss();
            }
        }
    }

    // Save state of picker on orientation changes
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(PICKER_KEY, mPicker.getValue());
    }
}
