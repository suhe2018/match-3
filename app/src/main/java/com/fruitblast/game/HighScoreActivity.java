package com.fruitblast.game;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fruitblast.game.databinding.ActivityHighScoreBinding;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class HighScoreActivity extends AppCompatActivity {

    private ActivityHighScoreBinding binding;
    private HighScoreAdapter adapter;
    private PreferenceHelper prefs;
    private Difficulty currentDifficulty = Difficulty.EASY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHighScoreBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PreferenceHelper(this);

        // Setup RecyclerView
        adapter = new HighScoreAdapter(prefs.getScoreList(currentDifficulty));
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        // Setup Tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🌱 简单"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🌿 普通"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🔥 困难"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentDifficulty = Difficulty.EASY; break;
                    case 1: currentDifficulty = Difficulty.MEDIUM; break;
                    case 2: currentDifficulty = Difficulty.HARD; break;
                }
                refreshList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnBack.setOnClickListener(v -> finish());

        refreshList();
    }

    private void refreshList() {
        List<String[]> scores = prefs.getScoreList(currentDifficulty);
        adapter.setEntries(scores);
        binding.tvEmpty.setVisibility(scores.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(scores.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
