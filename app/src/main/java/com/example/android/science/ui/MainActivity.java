package com.example.android.science.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.example.android.science.R;
import com.example.android.science.ui.adapters.TopicsAdapter;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String DIALOG_TAG = "topic_dialog";

    @BindView(R.id.rv_topics_list) RecyclerView mTopicsRecyclerView;
    @BindArray(R.array.topic_names) String[] mTopicNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Bind the views
        ButterKnife.bind(this);

        // RecyclerView settings
        // Use a grid layout manager
        final GridLayoutManager mLayoutManager = new GridLayoutManager(this,
                getResources().getInteger(R.integer.column_number));
        mTopicsRecyclerView.setLayoutManager(mLayoutManager);
        TopicsAdapter adapter = new TopicsAdapter(mTopicNames,
                new TopicsAdapter.TopicsAdapterListener() {
            @Override
            public void OnClick(View v, int position) {
                PreQuestionDialog dialog = PreQuestionDialog.getInstance(position);
                dialog.show(getSupportFragmentManager(), DIALOG_TAG);
            }
        });
        // Set the adapter on the {@link RecyclerView} so the list can be populated in the UI
        mTopicsRecyclerView.setAdapter(adapter);
    }

    // Open ProfileActivity on clicking the bottom TextView
    @OnClick(R.id.tv_profile_access)
    public void openProfile() {
        Intent openProfileActivityIntent = new Intent(this, ProfileActivity.class);
        startActivity(openProfileActivityIntent);
        // Activity transition for older devices
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        }
    }
}
