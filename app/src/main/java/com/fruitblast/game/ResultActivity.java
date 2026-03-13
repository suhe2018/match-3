package com.fruitblast.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.fruitblast.game.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int score          = getIntent().getIntExtra(GameActivity.EXTRA_SCORE, 0);
        String diffName    = getIntent().getStringExtra(GameActivity.EXTRA_DIFFICULTY_NAME);
        int maxCombo       = getIntent().getIntExtra(GameActivity.EXTRA_MAX_COMBO, 0);
        int totalMatches   = getIntent().getIntExtra(GameActivity.EXTRA_TOTAL_MATCHES, 0);
        boolean isRecord   = getIntent().getBooleanExtra(GameActivity.EXTRA_IS_RECORD, false);

        if (diffName == null) diffName = Difficulty.MEDIUM.name();
        Difficulty difficulty = Difficulty.valueOf(diffName);

        setupUI(score, difficulty, maxCombo, totalMatches, isRecord);

        // Back press → main menu
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToMainMenu();
            }
        });
    }

    private void setupUI(int score, Difficulty difficulty,
                         int maxCombo, int totalMatches, boolean isRecord) {

        binding.tvDifficulty.setText(difficulty.emoji + " " + difficulty.displayName);
        binding.tvRank.setText(getRankLabel(score));
        binding.tvMaxCombo.setText(getString(R.string.result_max_combo, maxCombo));
        binding.tvTotalMatches.setText(getString(R.string.result_total_matches, totalMatches));

        if (isRecord) {
            binding.tvNewRecord.setVisibility(View.VISIBLE);
        }

        animateScore(score);

        binding.btnPlayAgain.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name());
            startActivity(intent);
            finish();
        });

        binding.btnHighScores.setOnClickListener(v ->
                startActivity(new Intent(this, HighScoreActivity.class)));

        binding.btnMainMenu.setOnClickListener(v -> goToMainMenu());
    }

    private void goToMainMenu() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String getRankLabel(int score) {
        if (score >= 5000) return "🏆 传奇";
        if (score >= 2000) return "💎 钻石";
        if (score >= 1000) return "🥇 黄金";
        if (score >= 500)  return "🥈 白银";
        if (score >= 100)  return "🥉 青铜";
        return "🌱 新手";
    }

    private void animateScore(int target) {
        long start = System.currentTimeMillis();
        long duration = 1200L;
        binding.tvScore.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - start;
                float fraction = Math.min(1f, (float) elapsed / duration);
                binding.tvScore.setText(String.valueOf((int) (target * fraction)));
                if (fraction < 1f) {
                    binding.tvScore.postDelayed(this, 16);
                } else {
                    binding.tvScore.setText(String.valueOf(target));
                }
            }
        });
    }
}
