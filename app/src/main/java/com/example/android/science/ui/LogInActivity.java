package com.example.android.science.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.science.R;
import com.example.android.science.model.User;
import com.example.android.science.utilities.DatabaseUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.android.science.ui.SignInActivity.IS_NEW_LOGIN_MAIL_KEY;
import static com.example.android.science.utilities.DatabaseUtils.USERS_REFERENCE;

public class LogInActivity extends AppCompatActivity {

    private static final String LOG_TAG = LogInActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.et_username)
    EditText mUserNameEditText;
    @BindView(R.id.et_email)
    EditText mEmailEditText;
    @BindView(R.id.et_password)
    EditText mPasswordEditText;
    @BindView(R.id.sign_button)
    Button mSignButton;
    @BindView(R.id.tv_sign_up)
    TextView mSignUpNotAccountTextView;
    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingIndicator;
    @BindView(R.id.visibility_toggle)
    ImageView mVisibilityToggle;

    private boolean isNewLogin;

    // Firebase variables
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    /* Boolean to track whether the loading indicator is visible or not*/
    private boolean isLoading = false;

    /* Boolean to track whether the password is visible or not*/
    private boolean isVisible = false;

    private String mUsername;

    private static final String LOADING_KEY = "is_loading";
    private static final String USERNAME_KEY = "username";
    private static final String EMAIL_KEY = "email";
    private static final String PASSWORD_KEY = "password";
    private static final String VISIBILITY_KEY = "is_visible";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
        // Bind the views
        ButterKnife.bind(this);

        // Set the toolbar
        setSupportActionBar(mToolbar);
        // Hide the title
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Progress bar color: https://tinyurl.com/yatgcjmv
        if (mLoadingIndicator.getIndeterminateDrawable() != null) {
            mLoadingIndicator.getIndeterminateDrawable()
                    .setColorFilter(ContextCompat.getColor(this, R.color.colorAccent),
                            android.graphics.PorterDuff.Mode.SRC_IN);
        }

        // Initialize Authentication variables
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        isNewLogin = getIntent().getExtras().getBoolean(IS_NEW_LOGIN_MAIL_KEY);
        if (isNewLogin) {
            mSignUpNotAccountTextView.setVisibility(View.INVISIBLE);
        } else {
            mUserNameEditText.setVisibility(View.GONE);
            mSignButton.setText(getString(R.string.sign_in_button));
            // Underline and add link color
            String link = getString(R.string.sign_up_account_2);
            String signUp = getString(R.string.sign_up_account_1, link);
            SpannableStringBuilder spannable = new SpannableStringBuilder(signUp);
            spannable.setSpan(new UnderlineSpan(), signUp.indexOf(link),
                    signUp.indexOf(link) + link.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            int linkColor = ContextCompat.getColor(this, R.color.colorAccent);
            spannable.setSpan(new ForegroundColorSpan(linkColor), signUp.indexOf(link),
                    signUp.indexOf(link) + link.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mSignUpNotAccountTextView.setText(spannable);
        }

        if (savedInstanceState != null) {
            isLoading = savedInstanceState.getBoolean(LOADING_KEY);
            if (isLoading) mLoadingIndicator.setVisibility(View.VISIBLE);
            if (savedInstanceState.getString(USERNAME_KEY) != null) {
                mUserNameEditText.setText(savedInstanceState.getString(USERNAME_KEY));
            }
            if (savedInstanceState.getString(EMAIL_KEY) != null) {
                mEmailEditText.setText(savedInstanceState.getString(EMAIL_KEY));
            }
            if (savedInstanceState.getString(PASSWORD_KEY) != null) {
                mPasswordEditText.setText(savedInstanceState.getString(PASSWORD_KEY));
            }
            isVisible = savedInstanceState.getBoolean(VISIBILITY_KEY);
            if (isVisible) {
                isVisible = false;
                changeVisibility();
            }
        }
    }

    // Show/hide password: https://tinyurl.com/y9hrkmby
    @OnClick(R.id.visibility_toggle)
    public void changeVisibility() {
        if (!isVisible) {
            // Show password
            mPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mVisibilityToggle.setImageResource(R.drawable.ic_visibility_off);
            isVisible = true;
        } else {
            // Hide password
            mPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            mVisibilityToggle.setImageResource(R.drawable.ic_visibility);
            isVisible = false;
        }
    }

    @OnClick(R.id.tv_sign_up)
    public void signUp() {
        finish();
    }

    @OnClick(R.id.sign_button)
    public void sign() {
        // Check Internet connection
        if (!checkInternetConnection()) {
            Toast.makeText(LogInActivity.this, getString(R.string.error),
                    Toast.LENGTH_LONG).show();
            return;
        }
        // Check valid email
        String email = mEmailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            mEmailEditText.setError(getString(R.string.email_error));
            return;
        } else {
            // Validate email: https://tinyurl.com/ydflnzju
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mEmailEditText.setError(getString(R.string.invalid_email_error));
                return;
            } else {
                mEmailEditText.setError(null);
            }
        }
        // Check password is not empty
        String password = mPasswordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            mPasswordEditText.setError(getString(R.string.password_error));
            return;
        } else {
            mPasswordEditText.setError(null);
        }
        if (isNewLogin) {
            // Check username is not empty
            String username = mUserNameEditText.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                mUserNameEditText.setError(getString(R.string.username_error));
                return;
            } else {
                mUserNameEditText.setError(null);
            }
            // Sign Up new user
            mUsername = username;
            signUp(email, password);
        } else {
            // Sign In existing user
            signIn(email, password);
        }
    }

    private void signUp(String email, String password) {
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mSignButton.setVisibility(View.GONE);
        isLoading = true;
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(LOG_TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String uid = user.getUid();
                                String name;
                                if (mUsername != null) {
                                    name = mUsername;
                                } else {
                                    name = "";
                                }
                                String email = user.getEmail();
                                writeNewUser(uid, name, email);
                            }
                            // Start the app
                            startApp();
                        } else {
                            mLoadingIndicator.setVisibility(View.GONE);
                            mSignButton.setVisibility(View.VISIBLE);
                            isLoading = false;
                            Log.w(LOG_TAG, "createUserWithEmail:failure", task.getException());
                            // If sign in fails, display a message to the user.
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                // Email already exist: https://tinyurl.com/yatex4tl
                                Toast.makeText(LogInActivity.this,
                                        getString(R.string.existing_email_message),
                                        Toast.LENGTH_SHORT).show();
                            } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                // The password is not valid
                                Toast.makeText(LogInActivity.this,
                                        ((FirebaseAuthWeakPasswordException) task.getException())
                                                .getReason(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LogInActivity.this,
                                        getString(R.string.authentication_error),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void signIn(String email, String password) {
        mLoadingIndicator.setVisibility(View.VISIBLE);
        mSignButton.setVisibility(View.GONE);
        isLoading = true;
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(LOG_TAG, "signInWithEmail:success");
                            startApp();
                        } else {
                            mLoadingIndicator.setVisibility(View.GONE);
                            mSignButton.setVisibility(View.VISIBLE);
                            isLoading = false;
                            // If sign in fails, display a message to the user.
                            Log.w(LOG_TAG, "signInWithEmail:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(LogInActivity.this,
                                        task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LogInActivity.this,
                                        getString(R.string.authentication_error),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    private void startApp() {
        // Start the app
        Intent openMainActivity = new Intent(getApplicationContext(),
                MainActivity.class);
        openMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(openMainActivity);
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

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(LOADING_KEY, isLoading);
        if (mUserNameEditText.getVisibility() == View.VISIBLE) {
            String username = mUserNameEditText.getText().toString();
            if (!TextUtils.isEmpty(username)) {
                savedInstanceState.putString(USERNAME_KEY, username);
            }
        }
        String email = mEmailEditText.getText().toString();
        if (!TextUtils.isEmpty(email)) {
            savedInstanceState.putString(USERNAME_KEY, email);
        }
        String password = mPasswordEditText.getText().toString();
        if (!TextUtils.isEmpty(password)) {
            savedInstanceState.putString(USERNAME_KEY, password);
        }
        savedInstanceState.putBoolean(VISIBILITY_KEY, isVisible);
    }

    /**
     * Helper method to create new user node in the database
     */
    private void writeNewUser(String userId, String username, String email) {
        Map<String, Integer> pointsByCategory = new HashMap<>();
        for (int i = 0; i < DatabaseUtils.categoryNames.length; i++) {
            pointsByCategory.put(DatabaseUtils.categoryNames[i], 0);
        }
        User user = new User(username, email, "", "", 0,
                0, 0, 0, pointsByCategory);

        mDatabase.child(USERS_REFERENCE).child(userId).setValue(user);
    }
}
