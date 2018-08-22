package com.example.android.science.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.science.R;
import com.example.android.science.model.User;
import com.example.android.science.ui.MainActivity;
import com.example.android.science.utilities.DatabaseUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import static com.example.android.science.utilities.DatabaseUtils.USERS_REFERENCE;

/**
 * Implementation of App Widget functionality.
 */
public class StatsWidget extends AppWidgetProvider {
    private static final String LOG_TAG = StatsWidget.class.getSimpleName();
    private static final String NEXT_CLICKED = "next_topic";
    private static final String PREVIOUS_CLICKED = "previous_topic";
    private static final String WIDGET_KEY = "widget_id";

    // Firebase variables
    private static FirebaseAuth mAuth;
    private static FirebaseAuth.AuthStateListener mAuthListener;
    private static DatabaseReference mDatabase;

    private static String[] topicNames;
    private static TypedArray legendDrawables;
    private static Map<String, Integer> pointsByCategory;
    private static int totalPoints;
    private static int position = 0;

    static void updateAppWidget(final Context context, final AppWidgetManager appWidgetManager,
                         final int appWidgetId) {

        // Construct the RemoteViews object
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stats_widget);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    final String userId = user.getUid();
                    // Initialize Firebase Database
                    mDatabase = FirebaseDatabase.getInstance().getReference().child(USERS_REFERENCE)
                            .child(userId);
                    mDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                try {
                                    User databaseUser = dataSnapshot.getValue(User.class);
                                    if (databaseUser.getNumberOfGames() == 0) {
                                        views.setViewVisibility(R.id.tv_empty_stats, View.VISIBLE);
                                        views.setViewVisibility(R.id.stats_layout, View.INVISIBLE);
                                    } else {
                                        views.setViewVisibility(R.id.tv_empty_stats, View.GONE);
                                        views.setViewVisibility(R.id.stats_layout, View.VISIBLE);
                                        int correctAnswers = databaseUser.getCorrectAnswers();
                                        int incorrectAnswers = databaseUser.getIncorrectAnswers();
                                        // Calculate percentages
                                        int totalAnswers = correctAnswers + incorrectAnswers;
                                        int correctPercentage = (100 * correctAnswers / totalAnswers);
                                        int incorrectPercentage = 100 - correctPercentage;
                                        // Set stats bar
                                        views.setProgressBar(R.id.stats_bar, 100,
                                                correctPercentage, false);
                                        views.setTextViewText(R.id.tv_correct_percentage,
                                                context.getString(R.string.percentage,
                                                        String.valueOf(correctPercentage)));
                                        views.setTextViewText(R.id.tv_incorrect_percentage,
                                                context.getString(R.string.percentage,
                                                        String.valueOf(incorrectPercentage)));
                                        // Set topic points
                                        topicNames = context.getResources()
                                                .getStringArray(R.array.topic_names);
                                        legendDrawables = context.getResources()
                                                .obtainTypedArray(R.array.legend_drawables);
                                        pointsByCategory = databaseUser.getPointsByCategory();
                                        totalPoints = databaseUser.getTotalPoints();
                                        changeChart(context, views,position);
                                        // Set click: https://tinyurl.com/y6u2tofy
                                        views.setOnClickPendingIntent(R.id.next_topic,
                                                getPendingSelfIntent(context, NEXT_CLICKED,
                                                        appWidgetId));
                                        views.setOnClickPendingIntent(R.id.previous_topic,
                                                getPendingSelfIntent(context, PREVIOUS_CLICKED,
                                                        appWidgetId));
                                    }
                                    appWidgetManager.updateAppWidget(appWidgetId, views);
                                } catch (DatabaseException exception) {
                                    Log.e(LOG_TAG, exception.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            views.setViewVisibility(R.id.tv_empty_stats, View.VISIBLE);
                            views.setViewVisibility(R.id.stats_layout, View.INVISIBLE);
                            appWidgetManager.updateAppWidget(appWidgetId, views);
                        }
                    });
                } else {
                    // User is signed out
                    views.setViewVisibility(R.id.tv_empty_stats, View.VISIBLE);
                    views.setViewVisibility(R.id.stats_layout, View.INVISIBLE);
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                }
            }
        };

        // Open app on click
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.app_name, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    static void deleteAppWidget(final Context context, int appWidgetId) {
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        legendDrawables.recycle();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
            mAuth.addAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            deleteAppWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction() != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.stats_widget);
            int widgetId = intent.getIntExtra(WIDGET_KEY, 0);
            if (intent.getAction().equals(NEXT_CLICKED)) {
                if (position == topicNames.length - 1) {
                    position = 0;
                } else {
                    position++;
                }
                changeChart(context, views, position);
                appWidgetManager.updateAppWidget(widgetId, views);
            } else if (intent.getAction().equals(PREVIOUS_CLICKED)) {
                if (position == 0) {
                    position = topicNames.length - 1;
                } else {
                    position--;
                }
                changeChart(context, views, position);
                appWidgetManager.updateAppWidget(widgetId, views);
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static void changeChart(Context context, RemoteViews views, int position) {
        String currentCategory = DatabaseUtils.categoryNames[position];
        int categoryPoints = pointsByCategory.get(currentCategory);
        int categoryPercentage = (100 * categoryPoints / totalPoints);
        views.setProgressBar(R.id.topics_bar, 100,
                categoryPercentage, false);
        views.setTextViewText(R.id.tv_topic_points,
                context.getString(R.string.questions_number_text,
                        String.valueOf(categoryPoints),
                        String.valueOf(totalPoints)));
        views.setTextViewText(R.id.legend_label, topicNames[position]);
        views.setTextViewCompoundDrawablesRelative(R.id.legend_label,
                legendDrawables.getResourceId(position, 0), 0, 0, 0);
    }

    protected static PendingIntent getPendingSelfIntent(Context context, String action,
                                                        int widgetId) {
        Intent intent = new Intent(context, StatsWidget.class);
        intent.setAction(action);
        intent.putExtra(WIDGET_KEY, widgetId);
        return PendingIntent.getBroadcast(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}

