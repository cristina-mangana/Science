package com.example.android.science.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.science.R;
import com.example.android.science.model.User;
import com.example.android.science.ui.adapters.LegendAdapter;
import com.example.android.science.utilities.DatabaseUtils;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.android.science.utilities.DatabaseUtils.USERS_REFERENCE;

public class ProfileActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.iv_profile_picture)
    CircleImageView mProfilePicture;
    @BindView(R.id.tv_user_name)
    TextView mUserNameTextView;
    @BindView(R.id.user_nickname)
    TextView mNicknameTextView;
    @BindView(R.id.tv_number_of_games)
    TextView mGamesTextView;
    @BindView(R.id.tv_number_of_correct)
    TextView mCorrectTextView;
    @BindView(R.id.tv_number_of_incorrect)
    TextView mIncorrectTextView;
    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingIndicator;
    @BindView(R.id.tv_empty_stats)
    TextView mEmptyTextView;
    @BindView(R.id.stats_layout)
    View mStatsLayout;
    @BindView(R.id.app_bar)
    AppBarLayout mAppBar;
    @BindView(R.id.toolbar_user_name)
    TextView mToolbarUsername;
    @BindView(R.id.legend)
    RecyclerView mLegend;
    @BindView(R.id.iv_add_profile_picture)
    ImageView mAddPhoto;
    @BindView(R.id.et_user_name)
    EditText mUserNameEditText;
    @BindView(R.id.edit_button)
    ImageView mEditButton;
    @BindView(R.id.cancel_button)
    ImageView mCancelButton;
    @BindArray(R.array.topic_names)
    String[] topicNames;
    @BindArray(R.array.topic_colors)
    int[] topicColors;

    private DatabaseReference mDatabase;
    private String mSelectedImage;
    private String mDatabaseImage;

    /* Boolean to track the edit mode state*/
    private boolean isDone = false;

    /* Boolean to track the collapsing state*/
    private boolean isCollapsed = false;

    /* Booleans to track whether the dialogs are open or closed*/
    private boolean isOpenSignOut = false;
    private boolean isOpenCredits = false;

    /**
     * Identifier for the request image intent
     */
    static final int REQUEST_IMAGE_OPEN = 1;

    private static final String COLLAPSED_KEY = "is_collapsed";
    private static final String SIGN_OUT_KEY = "is_open_sign_out";
    private static final String CREDITS_KEY = "is_open_credits";
    private static final String EDIT_KEY = "is_done";
    private static final String NAME_KEY = "new_name";
    private static final String PHOTO_KEY = "new_photo";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        // Bind the views
        ButterKnife.bind(this);
        final IncludedLayout includedStatsLayout = new IncludedLayout();
        ButterKnife.bind(includedStatsLayout, mStatsLayout);

        // TODO check Enter transition for newer devices
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slide = new Slide(Gravity.BOTTOM);
            // TODO need this? slide.addTarget();
            slide.setInterpolator(AnimationUtils.loadInterpolator(this,
                    android.R.interpolator.linear_out_slow_in));
            slide.setDuration(getResources().getInteger(R.integer.activity_transition_duration));
            getWindow().setEnterTransition(slide);
        }

        // Check Internet connection
        if (!checkInternetConnection()) {
            Toast.makeText(this, getString(R.string.error_internet_profile), Toast.LENGTH_LONG).show();
            finish();
        }

        // Set toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        // Add back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // Set overflow icon
        mToolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_settings));

        // Progress bar color: https://tinyurl.com/yatgcjmv
        if (mLoadingIndicator.getIndeterminateDrawable() != null) {
            mLoadingIndicator.getIndeterminateDrawable()
                    .setColorFilter(ContextCompat.getColor(this, R.color.colorAccent),
                            android.graphics.PorterDuff.Mode.SRC_IN);
        }

        // Maintain state on orientation changes
        if (savedInstanceState != null) {
            isDone = savedInstanceState.getBoolean(EDIT_KEY);
            if (isDone) {
                isDone = false;
                changeUsername();
                if (savedInstanceState.getString(NAME_KEY) != null) {
                    mUserNameEditText.setText(savedInstanceState.getString(NAME_KEY));
                }
                if (savedInstanceState.getString(PHOTO_KEY) != null) {
                    mSelectedImage = savedInstanceState.getString(PHOTO_KEY);
                }
            }
            isCollapsed = savedInstanceState.getBoolean(COLLAPSED_KEY);
            if (isCollapsed) {
                mToolbarUsername.setVisibility(View.VISIBLE);
            }
            isOpenSignOut = savedInstanceState.getBoolean(SIGN_OUT_KEY);
            if (isOpenSignOut) openSignOutDialog();
            isOpenCredits = savedInstanceState.getBoolean(CREDITS_KEY);
            if (isOpenCredits) openCredits();
        }

        // Get current user
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            // Initialize Firebase Database
            mDatabase = FirebaseDatabase.getInstance().getReference().child(USERS_REFERENCE)
                    .child(userId);
            mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User databaseUser = dataSnapshot.getValue(User.class);
                        String username = databaseUser.getUsername();
                        mUserNameTextView.setText(username);
                        mEditButton.setVisibility(View.VISIBLE);
                        mUserNameEditText.setHint(username);
                        mToolbarUsername.setText(username);
                        String nickname;
                        if (!databaseUser.getNickname().isEmpty()) {
                            nickname = databaseUser.getNickname();
                        } else {
                            nickname = getString(R.string.nickname_placeholder);
                        }
                        mNicknameTextView.setText(nickname);
                        mDatabaseImage = databaseUser.getPhotoUrl();
                        if (mSelectedImage != null) {
                            Picasso.get().load(mSelectedImage).into(mProfilePicture);
                        } else {
                            if (!mDatabaseImage.isEmpty()) {
                                Picasso.get().load(mDatabaseImage).error(R.drawable.ic_user)
                                        .into(mProfilePicture);
                            } else {
                                Picasso.get().load(R.drawable.ic_user).into(mProfilePicture);
                            }
                        }
                        // Title only in collapsed mode: https://tinyurl.com/yb9u3w2k
                        mAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                            @Override
                            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                                if (Math.abs(verticalOffset) - appBarLayout.getTotalScrollRange() == 0) {
                                    // Collapsed
                                    isCollapsed = true;
                                    mToolbarUsername.setVisibility(View.VISIBLE);
                                } else {
                                    // Expanded
                                    isCollapsed = false;
                                    mToolbarUsername.setVisibility(View.GONE);
                                }
                            }
                        });
                        mGamesTextView.setText(String.valueOf(databaseUser.getNumberOfGames()));
                        int correctAnswers = databaseUser.getCorrectAnswers();
                        mCorrectTextView.setText(String.valueOf(correctAnswers));
                        int incorrectAnswers = databaseUser.getIncorrectAnswers();
                        mIncorrectTextView.setText(String.valueOf(incorrectAnswers));
                        if (databaseUser.getNumberOfGames() == 0) {
                            showError(getString(R.string.no_data));
                        } else {
                            // Calculate percentages
                            int totalAnswers = correctAnswers + incorrectAnswers;
                            int correctPercentage = (100 * correctAnswers / totalAnswers);
                            Log.d("correct", String.valueOf(correctPercentage));
                            int incorrectPercentage = 100 - correctPercentage;
                            includedStatsLayout.statsBar.setProgress(correctPercentage);
                            includedStatsLayout.correctPercentage
                                    .setText(getString(R.string.percentage,
                                            String.valueOf(correctPercentage)));
                            includedStatsLayout.incorrectPercentage
                                    .setText(getString(R.string.percentage,
                                            String.valueOf(incorrectPercentage)));
                            int totalWidth = includedStatsLayout.statsBar.getWidth();
                            if (correctPercentage >= 90) {
                                includedStatsLayout.incorrectLabel.setVisibility(View.INVISIBLE);
                                includedStatsLayout.incorrectLabel
                                        .setWidth((int) (0.1 * totalWidth));
                                includedStatsLayout.correctLabel.setWidth((int) (0.9 * totalWidth));
                                includedStatsLayout.incorrectPercentage
                                        .setWidth((int) (0.1 * totalWidth));
                            } else if (correctPercentage <= 10) {
                                includedStatsLayout.correctLabel.setVisibility(View.INVISIBLE);
                                includedStatsLayout.correctLabel.setWidth((int) (0.1 * totalWidth));
                                includedStatsLayout.incorrectLabel
                                        .setWidth((int) (0.9 * totalWidth));
                                includedStatsLayout.incorrectPercentage
                                        .setWidth((int) (0.9 * totalWidth));
                            } else {
                                includedStatsLayout.incorrectLabel
                                        .setWidth(totalWidth * incorrectPercentage / 100);
                                includedStatsLayout.correctLabel
                                        .setWidth(totalWidth * correctPercentage / 100);
                                includedStatsLayout.incorrectPercentage
                                        .setWidth(totalWidth * incorrectPercentage / 100);
                            }
                            // Set pie chart
                            Map<String, Integer> pointsByCategory = databaseUser.getPointsByCategory();
                            List<PieEntry> entries = new ArrayList<>();
                            List<Integer> colors = new ArrayList<>();
                            for (int i = 0; i < DatabaseUtils.categoryNames.length; i++) {
                                String currentCategory = DatabaseUtils.categoryNames[i];
                                if (pointsByCategory.get(currentCategory) != 0) {
                                    entries.add(new PieEntry(pointsByCategory.get(currentCategory),
                                            currentCategory));
                                    colors.add(topicColors[i]);
                                }
                            }
                            PieDataSet pieDataSet = new PieDataSet(entries,
                                    getString(R.string.pie_chart_label));
                            pieDataSet.setColors(colors);
                            pieDataSet.setValueFormatter(new LargeValueFormatter());
                            PieData data = new PieData(pieDataSet);
                            includedStatsLayout.topicsPieChart.setData(data);
                            includedStatsLayout.topicsPieChart.setHoleRadius(65f);
                            includedStatsLayout.topicsPieChart.setTransparentCircleRadius(70f);
                            includedStatsLayout.topicsPieChart.setHoleColor(ContextCompat
                                    .getColor(ProfileActivity.this, R.color.colorPrimary));
                            includedStatsLayout.topicsPieChart.setDrawEntryLabels(false);
                            // Set legend
                            Legend legend = includedStatsLayout.topicsPieChart.getLegend();
                            legend.setEnabled(false);
                            LinearLayoutManager layoutManager = new LinearLayoutManager(ProfileActivity.this);
                            mLegend.setLayoutManager(layoutManager);
                            LegendAdapter adapter = new LegendAdapter(topicNames);
                            mLegend.setAdapter(adapter);
                            mLegend.setNestedScrollingEnabled(false);
                            // Animate
                            includedStatsLayout.topicsPieChart.animateXY(1000, 1000);

                            // Adjust visibility
                            mLoadingIndicator.setVisibility(View.GONE);
                            mStatsLayout.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // An error has occurred
                    showError(getString(R.string.error_profile));
                }
            });
        } else {
            showError(getString(R.string.error_profile));
        }
    }

    @OnClick(R.id.edit_button)
    public void changeUsername() {
        if (!isDone) {
            mAddPhoto.setVisibility(View.VISIBLE);
            mUserNameEditText.setVisibility(View.VISIBLE);
            mUserNameEditText.requestFocus();
            mUserNameTextView.setVisibility(View.GONE);
            mEditButton.setImageResource(R.drawable.ic_done);
            mCancelButton.setVisibility(View.VISIBLE);
            isDone = true;
        } else {
            String newName = mUserNameEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(newName)) {
                mUserNameTextView.setText(newName);
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put(DatabaseUtils.USERNAME, newName);
                mDatabase.updateChildren(childUpdates);
            }
            if (mSelectedImage != null) {
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put(DatabaseUtils.PHOTO_URL, mSelectedImage);
                mDatabase.updateChildren(childUpdates);
                mDatabaseImage = mSelectedImage;
                mSelectedImage = null;
            }
            mAddPhoto.setVisibility(View.GONE);
            mUserNameEditText.setVisibility(View.GONE);
            mUserNameTextView.setVisibility(View.VISIBLE);
            mEditButton.setImageResource(R.drawable.ic_edit);
            mCancelButton.setVisibility(View.GONE);
            isDone = false;
            // Hide keyboard if open
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @OnClick(R.id.cancel_button)
    public void cancelEdit() {
        mAddPhoto.setVisibility(View.GONE);
        mUserNameEditText.setVisibility(View.GONE);
        mUserNameEditText.setText("");
        mUserNameTextView.setVisibility(View.VISIBLE);
        mEditButton.setImageResource(R.drawable.ic_edit);
        mCancelButton.setVisibility(View.GONE);
        mSelectedImage = null;
        if (!mDatabaseImage.isEmpty()) {
            Picasso.get().load(mDatabaseImage).error(R.drawable.ic_user)
                    .into(mProfilePicture);
        } else {
            Picasso.get().load(R.drawable.ic_user).into(mProfilePicture);
        }
        isDone = false;
        // Hide keyboard if open
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @OnClick(R.id.iv_add_profile_picture)
    public void changePicture() {
        // Check if the permission is granted in versions > Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                selectImageFromGallery();
            } else {
                Toast.makeText(getApplicationContext(),
                        getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        } else {
            selectImageFromGallery();
        }
    }

    // Helper method to select an image from the gallery
    private void selectImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)),
                REQUEST_IMAGE_OPEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_OPEN && resultCode == RESULT_OK) {
            mSelectedImage = data.getData().toString();
            // Set Image resource
            Picasso.get().load(mSelectedImage).error(R.drawable.ic_user).into(mProfilePicture);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                openSignOutDialog();
                return true;
            case R.id.credits:
                openCredits();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Open the sign out confirmation dialog
    private void openSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.want_sign_out);
        builder.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth.getInstance().signOut();
                dialog.dismiss();
                Toast.makeText(ProfileActivity.this,
                        getString(R.string.sign_out_toast), Toast.LENGTH_SHORT).show();
                openSignIn();
            }
        });
        builder.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog_sign_out = builder.create();
        dialog_sign_out.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                isOpenSignOut = false;
            }
        });
        dialog_sign_out.show();
        isOpenSignOut = true;
    }

    // Open the credits dialog
    private void openCredits() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.credits);
        builder.setNeutralButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog_credits = builder.create();
        dialog_credits.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                isOpenCredits = false;
            }
        });
        dialog_credits.show();
        isOpenCredits = true;
    }

    // Open the sign in activity on logging out
    private void openSignIn() {
        Intent openSignInActivity = new Intent(this, SignInActivity.class);
        openSignInActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(openSignInActivity);
    }

    // Handle back button on toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showError(String error) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mStatsLayout.setVisibility(View.INVISIBLE);
        mEmptyTextView.setVisibility(View.VISIBLE);
        mEmptyTextView.setText(error);
    }

    // Bind views in included layouts: https://github.com/JakeWharton/butterknife/issues/393
    static class IncludedLayout {
        @BindView(R.id.stats_bar)
        ProgressBar statsBar;
        @BindView(R.id.tv_correct_label)
        TextView correctLabel;
        @BindView(R.id.tv_incorrect_label)
        TextView incorrectLabel;
        @BindView(R.id.tv_correct_percentage)
        TextView correctPercentage;
        @BindView(R.id.tv_incorrect_percentage)
        TextView incorrectPercentage;
        @BindView(R.id.topics_pie_chart)
        PieChart topicsPieChart;
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(COLLAPSED_KEY, isCollapsed);
        savedInstanceState.putBoolean(EDIT_KEY, isDone);
        savedInstanceState.putBoolean(SIGN_OUT_KEY, isOpenSignOut);
        savedInstanceState.putBoolean(CREDITS_KEY, isOpenCredits);
        String username = mUserNameEditText.getText().toString();
        if (!TextUtils.isEmpty(username)) {
            savedInstanceState.putString(NAME_KEY, username);
        }
        if (mSelectedImage != null) {
            savedInstanceState.putString(PHOTO_KEY, mSelectedImage);
        }
    }

    /**
     * Helper method to check for Internet connection
     */
    private boolean checkInternetConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
