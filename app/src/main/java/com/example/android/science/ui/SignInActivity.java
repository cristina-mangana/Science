package com.example.android.science.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.science.R;
import com.example.android.science.model.User;
import com.example.android.science.utilities.DatabaseUtils;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.android.science.utilities.DatabaseUtils.USERS_REFERENCE;

public class SignInActivity extends AppCompatActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.tv_sign_in)
    TextView mSignInTextView;
    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingIndicator;
    @BindView(R.id.buttons_layout)
    LinearLayout mButtonsLayout;
    @BindView(R.id.tv_sign_up_label)
    TextView mLabelTextView;

    // Firebase variables
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN_GOOGLE = 9001;

    private CallbackManager mCallbackManager;

    private final static String LOG_TAG = SignInActivity.class.getSimpleName();
    public final static String IS_NEW_LOGIN_MAIL_KEY = "new_login";

    /* Boolean to track whether the loading indicator is visible or not*/
    private boolean isLoading = false;
    private static final String LOADING_KEY = "is_loading";

    private static final String FACEBOOK_URL = "graph.facebook.com";
    private static final String FACEBOOK_IMAGE_LARGE = "?type=large";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
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
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    // Start MainActivity
                    Intent openMainActivity = new Intent(getApplicationContext(),
                            MainActivity.class);
                    startActivity(openMainActivity);
                    finish();
                } else {
                    // User is signed out
                    Log.d(LOG_TAG, "onAuthStateChanged:signed_out");
                    // Load the UI
                    loadUI();
                }
            }
        };

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Facebook Login button
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(LOG_TAG, "facebook:onSuccess:" + loginResult);
                        handleFacebookAccessToken(loginResult.getAccessToken());
                        mLoadingIndicator.setVisibility(View.VISIBLE);
                        mButtonsLayout.setVisibility(View.INVISIBLE);
                        isLoading = true;
                    }

                    @Override
                    public void onCancel() {
                        Log.d(LOG_TAG, "facebook:onCancel");
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.login_canceled),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.d(LOG_TAG, "facebook:onError");
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.authentication_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Initialize Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (savedInstanceState != null) {
            isLoading = savedInstanceState.getBoolean(LOADING_KEY);
            if (isLoading) mLoadingIndicator.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Helper method to load the UI
     */
    private void loadUI() {
        mLoadingIndicator.setVisibility(View.GONE);
        isLoading = false;
        mLabelTextView.setVisibility(View.VISIBLE);
        mButtonsLayout.setVisibility(View.VISIBLE);
        mSignInTextView.setVisibility(View.VISIBLE);
        // Underline and add link color
        String link = getString(R.string.sign_in_2);
        String signIn = getString(R.string.sign_in_1, link);
        SpannableStringBuilder spannable = new SpannableStringBuilder(signIn);
        spannable.setSpan(new UnderlineSpan(), signIn.indexOf(link),
                signIn.indexOf(link) + link.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int linkColor = ContextCompat.getColor(this, R.color.colorAccent);
        spannable.setSpan(new ForegroundColorSpan(linkColor), signIn.indexOf(link),
                signIn.indexOf(link) + link.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        mSignInTextView.setText(spannable);
    }

    // Attach the listener to the FirebaseAuth instance
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    // Remove the listener from the FirebaseAuth instance
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @OnClick(R.id.fb_button)
    public void logWithFacebook() {
        // Check Internet connection
        if (!checkInternetConnection()) {
            Toast.makeText(SignInActivity.this, getString(R.string.error),
                    Toast.LENGTH_LONG).show();
            return;
        }
        LoginManager.getInstance().logInWithReadPermissions(this,
                Arrays.asList("email", "public_profile"));
    }

    @OnClick(R.id.google_button)
    public void logWithGoogle() {
        // Check Internet connection
        if (!checkInternetConnection()) {
            Toast.makeText(SignInActivity.this, getString(R.string.error),
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN_GOOGLE);
    }

    @OnClick(R.id.email_button)
    public void logWithMail() {
        Intent openLogInActivity = new Intent(this, LogInActivity.class);
        openLogInActivity.putExtra(IS_NEW_LOGIN_MAIL_KEY, true);
        startActivity(openLogInActivity);
    }

    @OnClick(R.id.tv_sign_in)
    public void signIn() {
        Intent openLogInActivity = new Intent(this, LogInActivity.class);
        openLogInActivity.putExtra(IS_NEW_LOGIN_MAIL_KEY, false);
        startActivity(openLogInActivity);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(mCallbackManager.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN_GOOGLE) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Signed in successfully, show authenticated UI.
                firebaseAuthWithGoogle(account);
                mLoadingIndicator.setVisibility(View.VISIBLE);
                mButtonsLayout.setVisibility(View.INVISIBLE);
                isLoading = true;
            } catch (ApiException e) {
                Log.w(LOG_TAG, "signInResult:failed code=" + e.getStatusCode());
                Toast.makeText(SignInActivity.this,
                        getString(R.string.authentication_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(LOG_TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(LOG_TAG, "signInWithCredential:success");
                            startApp();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(LOG_TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignInActivity.this,
                                    getString(R.string.authentication_error),
                                    Toast.LENGTH_SHORT).show();
                            mLoadingIndicator.setVisibility(View.GONE);
                            mButtonsLayout.setVisibility(View.VISIBLE);
                            isLoading = false;
                        }
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(LOG_TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(LOG_TAG, "signInWithCredential:success");
                            startApp();
                        } else {
                            Log.w(LOG_TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignInActivity.this,
                                    getString(R.string.authentication_error),
                                    Toast.LENGTH_SHORT).show();
                            mLoadingIndicator.setVisibility(View.GONE);
                            mButtonsLayout.setVisibility(View.VISIBLE);
                            isLoading = false;
                        }
                    }
                });
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

    private void startApp() {
        // Write the new user in the database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String name = user.getDisplayName();
            String email = user.getEmail();
            String photoUrl = String.valueOf(user.getPhotoUrl());
            if (photoUrl.contains(FACEBOOK_URL)) photoUrl = photoUrl + FACEBOOK_IMAGE_LARGE;
            writeNewUser(uid, name, email, photoUrl);
        }
        // Start the app
        Intent openMainActivity = new Intent(getApplicationContext(), MainActivity.class);
        openMainActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(openMainActivity);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(LOADING_KEY, isLoading);
    }

    /**
     * Helper method to create new user node in the database
     */
    private void writeNewUser(final String userId, String username, String email, String photoUrl) {
        Map<String, Integer> pointsByCategory = new HashMap<>();
        for (int i = 0; i < DatabaseUtils.categoryNames.length; i++) {
            pointsByCategory.put(DatabaseUtils.categoryNames[i], 0);
        }
        final User user = new User(username, email, "", photoUrl, 0,
                0, 0, 0, pointsByCategory);

        mDatabase.child(USERS_REFERENCE).child(userId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue() == null) {
                    mutableData.setValue(userId);
                    return Transaction.success(mutableData);
                }
                return Transaction.abort();
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean commited,
                                   @Nullable DataSnapshot dataSnapshot) {
                if (commited) {
                    // unique key saved
                    mDatabase.child(USERS_REFERENCE).child(userId).setValue(user);
                } else {
                    // unique key already exists
                    Log.d(LOG_TAG, "User with this key already exists");
                }
            }
        });
    }
}
