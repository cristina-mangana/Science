package com.example.android.science.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.science.R;
import com.example.android.science.model.User;
import com.example.android.science.utilities.DatabaseUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rjsv.circularview.CircleView;

import static com.example.android.science.ui.PreQuestionDialog.CATEGORY_KEY;
import static com.example.android.science.ui.QuestionActivity.CORRECT_KEY;
import static com.example.android.science.ui.QuestionActivity.POINTS_KEY;
import static com.example.android.science.ui.QuestionActivity.TIME_LEFT_KEY;
import static com.example.android.science.ui.QuestionActivity.TOTAL_QUESTIONS_KEY;
import static com.example.android.science.utilities.DatabaseUtils.USERS_REFERENCE;

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

    private int mTotalQuestions;
    private int mNumberOfCorrect;
    private int mPoints;
    private String mCategoryName;
    private static final int SECONDS_IN_ONE_MINUTE = 60;

    private DatabaseReference mDatabase;

    private boolean isUpdating;

    private boolean isAnimate = false;

    private static final String ANIMATE_KEY = "is_animate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        // Bind the views
        ButterKnife.bind(this);

        mTotalQuestions = getIntent().getExtras().getInt(TOTAL_QUESTIONS_KEY);
        mNumberOfCorrect = getIntent().getExtras().getInt(CORRECT_KEY);
        int timeLeft = getIntent().getExtras().getInt(TIME_LEFT_KEY);
        mPoints = getIntent().getExtras().getInt(POINTS_KEY);
        mCategoryName = getIntent().getExtras().getString(CATEGORY_KEY);

        // Get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            // Initialize Firebase Database
            mDatabase = FirebaseDatabase.getInstance().getReference().child(USERS_REFERENCE)
                    .child(userId);
            isUpdating = true;
            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User databaseUser = dataSnapshot.getValue(User.class);
                        int numberOfGames = databaseUser.getNumberOfGames() + 1;
                        int numberOfCorrect = databaseUser.getCorrectAnswers() + mNumberOfCorrect;
                        int numberOfIncorrect = databaseUser.getIncorrectAnswers() +
                                (mTotalQuestions - mNumberOfCorrect);
                        int totalPoints = databaseUser.getTotalPoints() + mPoints;

                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put(DatabaseUtils.NUMBER_OF_GAMES, numberOfGames);
                        childUpdates.put(DatabaseUtils.CORRECT_ANSWERS, numberOfCorrect);
                        childUpdates.put(DatabaseUtils.INCORRECT_ANSWERS, numberOfIncorrect);
                        childUpdates.put(DatabaseUtils.TOTAL_POINTS, totalPoints);

                        Map<String, Integer> pointsByCategory;
                        if (databaseUser.getPointsByCategory() != null) {
                             pointsByCategory = databaseUser.getPointsByCategory();
                             if (pointsByCategory.containsKey(mCategoryName)) {
                                 int categoryPoints = pointsByCategory.get(mCategoryName) + mPoints;
                                 childUpdates.put(DatabaseUtils.POINTS_BY_CATEGORY_REFERENCE +
                                         "/" + mCategoryName, categoryPoints);
                             } else {
                                 childUpdates.put(DatabaseUtils.POINTS_BY_CATEGORY_REFERENCE +
                                         "/" + mCategoryName, mPoints);
                             }
                        } else {
                            pointsByCategory = new HashMap<>();
                            pointsByCategory.put(mCategoryName, mPoints);
                            childUpdates.put(DatabaseUtils.POINTS_BY_CATEGORY_REFERENCE,
                                    pointsByCategory);
                        }
                        mDatabase.updateChildren(childUpdates);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // An error has occurred
                    Toast.makeText(ResultsActivity.this, getString(R.string.error_database),
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(ResultsActivity.this, getString(R.string.error_database),
                    Toast.LENGTH_LONG).show();
        }
        isUpdating = false;

        // Set toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Add back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Set circular view
        int percentage = 100 * mNumberOfCorrect / mTotalQuestions;
        // Animate
        if (savedInstanceState != null) {
            isAnimate = savedInstanceState.getBoolean(ANIMATE_KEY);
        }
        if (!isAnimate) {
            isAnimate = true;
            ObjectAnimator animator = ObjectAnimator.ofFloat(mCircleView, "progressValue",
                    0, percentage);
            animator.setDuration(2500).start();
        } else {
            mCircleView.setProgressValue(percentage);
        }

        // Set summary
        mCorrectTextView.setText(getResources().getString(R.string.questions_number_text,
                String.valueOf(mNumberOfCorrect), String.valueOf(mTotalQuestions)));
        timeLeft = timeLeft / 1000;
        String minutes = getString(R.string.minutes_zero);
        String seconds = String.valueOf(timeLeft);
        if (timeLeft > SECONDS_IN_ONE_MINUTE) {
            minutes = getString(R.string.minutes_one);
            seconds = String.valueOf(timeLeft - SECONDS_IN_ONE_MINUTE);
        } else if (timeLeft == SECONDS_IN_ONE_MINUTE) {
            minutes = getString(R.string.minutes_one);
            seconds = getString(R.string.seconds_zero);
        } else if (timeLeft < 10) {
            seconds = getString(R.string.seconds_min, String.valueOf(timeLeft));
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

    @OnClick(R.id.see_stats_button)
    public void seeStats() {
        Intent openProfileActivityIntent = new Intent(this, ProfileActivity.class);
        startActivity(openProfileActivityIntent);
        // Activity transition for older devices
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        }
        finish();
    }

    // Handle back button on toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isUpdating) {
            Toast.makeText(ResultsActivity.this, getString(R.string.database_loading),
                    Toast.LENGTH_LONG).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ANIMATE_KEY, isAnimate);
    }
}
