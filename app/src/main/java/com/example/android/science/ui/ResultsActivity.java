package com.example.android.science.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.example.android.science.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rjsv.circularview.CircleView;

import static com.example.android.science.ui.QuestionActivity.CORRECT_KEY;
import static com.example.android.science.ui.QuestionActivity.POINTS_KEY;
import static com.example.android.science.ui.QuestionActivity.TIME_LEFT_KEY;
import static com.example.android.science.ui.QuestionActivity.TOTAL_QUESTIONS_KEY;

public class ResultsActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.circle_view)
    CircleView mCircleView;
    @BindView(R.id.tv_number_of_correct)
    TextView mCorrectTextView;
    @BindView(R.id.tv_time_left)
    TextView mTimeLeftTextView;
    @BindView(R.id.tv_points)
    TextView mPointsTextView;

    private int mPoints;
    private static final int SECONDS_IN_ONE_MINUTE = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        // Bind the views
        ButterKnife.bind(this);

        int totalQuestions = getIntent().getExtras().getInt(TOTAL_QUESTIONS_KEY);
        int numberOfCorrect = getIntent().getExtras().getInt(CORRECT_KEY);
        int timeLeft = getIntent().getExtras().getInt(TIME_LEFT_KEY);
        mPoints = getIntent().getExtras().getInt(POINTS_KEY);

        // Set toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Add back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        // Set circular view
        int percentage = 100 * numberOfCorrect / totalQuestions;
        // Animate
        ObjectAnimator animator = ObjectAnimator.ofFloat(mCircleView, "progressValue",
                0, percentage);
        animator.setDuration(2500).start();

        // Set summary
        mCorrectTextView.setText(getResources().getString(R.string.questions_number_text,
                String.valueOf(numberOfCorrect), String.valueOf(totalQuestions)));
        timeLeft = timeLeft / 1000;
        String minutes = getString(R.string.minutes_zero);
        String seconds = String.valueOf(timeLeft);
        if (timeLeft >= SECONDS_IN_ONE_MINUTE) {
            minutes = getString(R.string.minutes_one);
            if (timeLeft == SECONDS_IN_ONE_MINUTE) {
                seconds = getString(R.string.seconds_zero);
            } else {
                seconds = String.valueOf(timeLeft - SECONDS_IN_ONE_MINUTE);
            }
        }
        mTimeLeftTextView.setText(getString(R.string.time_left, minutes, seconds));
        mPointsTextView.setText(String.valueOf(mPoints));
    }

    @OnClick(R.id.share_button)
    public void share() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.shared_text,
                String.valueOf(mPoints), getString(R.string.app_name)));
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    @OnClick(R.id.play_again_button)
    public void playAgain() {
        // TODO
        finish();
    }

    // Handle back button on toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
