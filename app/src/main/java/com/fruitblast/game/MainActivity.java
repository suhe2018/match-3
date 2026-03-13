package com.fruitblast.game;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.fruitblast.game.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PreferenceHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PreferenceHelper(this);

        binding.btnEasy.setOnClickListener(v -> launchGame(Difficulty.EASY));
        binding.btnMedium.setOnClickListener(v -> launchGame(Difficulty.MEDIUM));
        binding.btnHard.setOnClickListener(v -> launchGame(Difficulty.HARD));
        binding.btnHighScores.setOnClickListener(v ->
                startActivity(new Intent(this, HighScoreActivity.class)));
        binding.btnSound.setOnClickListener(v -> { /* reserved for sound toggle */ });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBestScores();
    }

    private void refreshBestScores() {
        int easy   = prefs.getBestScore(Difficulty.EASY);
        int medium = prefs.getBestScore(Difficulty.MEDIUM);
        int hard   = prefs.getBestScore(Difficulty.HARD);

        binding.tvEasyBest.setText(easy > 0
                ? getString(R.string.easy_desc) + "  |  最高: " + easy
                : getString(R.string.easy_desc));
        binding.tvMediumBest.setText(medium > 0
                ? getString(R.string.medium_desc) + "  |  最高: " + medium
                : getString(R.string.medium_desc));
        binding.tvHardBest.setText(hard > 0
                ? getString(R.string.hard_desc) + "  |  最高: " + hard
                : getString(R.string.hard_desc));
    }

    private void launchGame(Difficulty difficulty) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name());
        startActivity(intent);
    }
}
