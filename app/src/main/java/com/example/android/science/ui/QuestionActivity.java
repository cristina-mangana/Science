package com.example.android.science.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.science.R;
import com.example.android.science.model.Question;
import com.example.android.science.ui.adapters.AnswersAdapter;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QuestionActivity extends AppCompatActivity {
    private static final String LOG_TAG = QuestionActivity.class.getSimpleName();

    /* Extra keys*/
    public static final String TOTAL_QUESTIONS_KEY = "total_questions";
    public static final String CORRECT_KEY = "correct_questions";
    public static final String TIME_LEFT_KEY = "time_left";
    public static final String POINTS_KEY = "points";
    private static final String OPEN_DIALOG_KEY = "open";
    private static final String QUESTION_POSITION_KEY = "current_question";
    private static final String CURRENT_TIME_KEY = "current_time";
    private static final String CURRENT_PROGRESS_KEY = "current_progress";
    private static final String HANDLER_KEY = "handler";

    @BindView(R.id.tv_number_of_questions)
    TextView mNumberOfQuestionsTextView;
    @BindView(R.id.tv_question)
    TextView mQuestionTextView;
    @BindView(R.id.rv_answers_list)
    RecyclerView mAnswersRecyclerView;
    @BindView(R.id.countdown_timer)
    ProgressBar mCountdownProgressBar;

    private CountDownTimer mCountDownTimer;
    private AlertDialog mDialog;

    private List<Question> mQuestions;

    /* Variables to track user's results. Time stored in milliseconds*/
    private int mNumberOfCorrects = 0;
    private int mTimeLeft = 0;
    private int mPoints = 0;

    /* Variables to track the time left*/
    private int mTime;
    private int mProgressValue;

    /* Variable to track the current question*/
    private int mCurrentNumberOfQuestion = 0;

    /* Boolean to track the state of the dialog on orientation changes */
    private boolean isOpen = false;

    /* Boolean to track the state of the handler */
    private boolean isWaiting = false;

    private int mCorrectPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        // Bind the views
        ButterKnife.bind(this);

        if (getIntent() == null && !getIntent().hasExtra(PreQuestionDialog.QUESTIONS_KEY)) {
            showError();
        }

        mQuestions = getIntent().getExtras()
                .getParcelableArrayList(PreQuestionDialog.QUESTIONS_KEY);

        if (mQuestions.size() == 0) {
            showError();
        }

        if (savedInstanceState != null) {
            // Save the results
            mNumberOfCorrects = savedInstanceState.getInt(CORRECT_KEY);
            mTimeLeft = savedInstanceState.getInt(TIME_LEFT_KEY);
            mPoints = savedInstanceState.getInt(POINTS_KEY);
            // Re-set the current question
            mCurrentNumberOfQuestion = savedInstanceState.getInt(QUESTION_POSITION_KEY);
            // Re-set timer
            mTime = savedInstanceState.getInt(CURRENT_TIME_KEY);
            mProgressValue = savedInstanceState.getInt(CURRENT_PROGRESS_KEY);
            // If mProgressValue = 0, change has occurred while waiting for the next question to
            // start
            if (mProgressValue == 0) mCurrentNumberOfQuestion++;
            isWaiting = savedInstanceState.getBoolean(HANDLER_KEY);
            if (isWaiting) {
                mCurrentNumberOfQuestion++;
                isWaiting = false;
                startNewQuestion(mCurrentNumberOfQuestion);
            } else {
                setUI(mCurrentNumberOfQuestion);
            }
            // Re-set dialog state
            isOpen = savedInstanceState.getBoolean(OPEN_DIALOG_KEY);
            if (isOpen) onBackPressed();
        } else {
            // Show the question
            startNewQuestion(mCurrentNumberOfQuestion);
        }
    }

    private void startNewQuestion(int numberOfQuestion) {
        mTime = 10000;
        mProgressValue = mCountdownProgressBar.getMax();
        setUI(numberOfQuestion);
    }

    /**
     * Helper method to finish the activity when an error occur.
     */
    private void showError() {
        Toast.makeText(this, getString(R.string.error_api), Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Helper method to re-draw the UI
     */
    private void setUI(int position) {
        Question currentQuestion = mQuestions.get(position);
        final int totalNumberOfQuestions = mQuestions.size();

        // Set count
        String positionString = String.valueOf(position + 1);
        String totalNumber = String.valueOf(totalNumberOfQuestions);
        String count = getResources()
                .getString(R.string.questions_number_text, positionString, totalNumber);
        SpannableStringBuilder spannable = new SpannableStringBuilder(count);
        spannable.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(this, R.color.colorAccent)), 0,
                positionString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mNumberOfQuestionsTextView.setText(spannable);

        // Set question
        mQuestionTextView.setText(Html.fromHtml(currentQuestion.getQuestionTitle()));

        // Set answers
        final List<String> answers = currentQuestion.getIncorrectAnswers();
        String correctAnswer = currentQuestion.getCorrectAnswer();
        Log.d(LOG_TAG, "Correct answer:" + correctAnswer);
        answers.add(correctAnswer);
        // Shuffle the list
        Collections.shuffle(answers);
        mCorrectPosition = answers.indexOf(correctAnswer);

        // Use a grid layout manager. Calculate the number of columns depending on the device
        // orientation
        int columnMultiplier = getResources().getInteger(R.integer.column_multiplier);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this,
                (answers.size()) / 2 * columnMultiplier);
        mAnswersRecyclerView.setLayoutManager(mLayoutManager);
        // Set the adapter
        AnswersAdapter adapter = new AnswersAdapter(answers, mCorrectPosition,
                new AnswersAdapter.AnswersAdapterListener() {
            @Override
            public void OnClick(View v, int position) {
                // Stop the timer
                if (mCountDownTimer != null) {
                    mCountDownTimer.cancel();
                    mCountDownTimer = null;
                }
                // Save the results
                if (position == mCorrectPosition) {
                    mNumberOfCorrects++;
                    mTimeLeft = mTimeLeft + mTime;
                    if (answers.size() == 2) {
                        // true-false questions are worth 2 points
                        mPoints = mPoints + 2 * mTime / 1000;
                    } else {
                        // multi-answers questions are worth 5 points
                        mPoints = mPoints + 5 * mTime / 1000;
                    }
                }
                if (mCurrentNumberOfQuestion < totalNumberOfQuestions - 1) {
                    Handler handler = new Handler();
                    isWaiting = true;
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            mCurrentNumberOfQuestion++;
                            isWaiting = false;
                            startNewQuestion(mCurrentNumberOfQuestion);
                        }
                    }, 1000);
                } else {
                    // This is the last question
                    Intent openResultsActivity = new Intent(getApplicationContext(),
                            ResultsActivity.class);
                    openResultsActivity.putExtra(TOTAL_QUESTIONS_KEY, totalNumberOfQuestions);
                    openResultsActivity.putExtra(CORRECT_KEY, mNumberOfCorrects);
                    openResultsActivity.putExtra(TIME_LEFT_KEY, mTimeLeft);
                    openResultsActivity.putExtra(POINTS_KEY, mPoints);
                    startActivity(openResultsActivity);
                    finish();
                }
            }
        });
        mAnswersRecyclerView.setAdapter(adapter);
        // Disable scrolling
        mAnswersRecyclerView.setNestedScrollingEnabled(false);

        // Set timer
        // Help from: https://stackoverflow.com/questions/10241633/android-progressbar-countdown
        startTimer(mTime, mProgressValue);
    }

    /**
     * Helper method for starting the countdown timer given a initial time
     * @param time is the time for the countdown
     * @param progressValue is the value of the progress bar
     */
    private void startTimer(int time, final int progressValue) {
        int interval = 100;
        mCountdownProgressBar.setProgress(progressValue);
        mCountDownTimer = new CountDownTimer(time, interval) {
            int counter = 0;
            @Override
            public void onTick(long millisUntilFinished) {
                counter++;
                mCountdownProgressBar.setProgress(progressValue - counter);
                mTime = (int) millisUntilFinished;
                mProgressValue = mCountdownProgressBar.getProgress();
            }

            @Override
            public void onFinish() {
                mCountdownProgressBar.setProgress(0);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mAnswersRecyclerView.findViewHolderForAdapterPosition(mCorrectPosition)
                                .itemView.performClick();
                        if (mNumberOfCorrects >= 0) mNumberOfCorrects--;
                    }
                }, 200);
            }
        };
        mCountDownTimer.start();
    }

    // Ask for confirmation before quiting the game
    @Override
    public void onBackPressed() {
        // Pause the timer
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.want_close);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                isOpen = false;
                finish();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                isOpen = false;
            }
        });
        mDialog = builder.create();
        mDialog.show();
        isOpen = true;
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                isOpen = false;
                // Resume the timer
                if (mCountDownTimer == null) {
                    startTimer(mTime, mProgressValue);
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mDialog != null) mDialog.dismiss();
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        savedInstanceState.putBoolean(OPEN_DIALOG_KEY, isOpen);
        savedInstanceState.putInt(QUESTION_POSITION_KEY, mCurrentNumberOfQuestion);
        savedInstanceState.putInt(CURRENT_TIME_KEY, mTime);
        savedInstanceState.putInt(CURRENT_PROGRESS_KEY, mProgressValue);
        savedInstanceState.putInt(CORRECT_KEY, mNumberOfCorrects);
        savedInstanceState.putInt(TIME_LEFT_KEY, mTimeLeft);
        savedInstanceState.putInt(POINTS_KEY, mPoints);
        savedInstanceState.putBoolean(HANDLER_KEY, isWaiting);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the timer
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Resume the timer
        if (mCountDownTimer == null) {
            startTimer(mTime, mProgressValue);
        }
    }
}
