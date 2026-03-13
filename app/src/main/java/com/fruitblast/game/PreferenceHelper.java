package com.fruitblast.game;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PreferenceHelper {

    private static final String PREFS_NAME = "fruitblast_prefs";
    private static final int MAX_ENTRIES = 5;

    private final SharedPreferences prefs;

    public PreferenceHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getBestScore(Difficulty difficulty) {
        return prefs.getInt("best_" + difficulty.name(), 0);
    }

    /**
     * Saves a game result. Returns true if it is a new all-time best for this difficulty.
     */
    public boolean saveScore(Difficulty difficulty, int score, int maxCombo, int totalMatches) {
        boolean isRecord = score > getBestScore(difficulty);
        if (isRecord) {
            prefs.edit().putInt("best_" + difficulty.name(), score).apply();
        }

        // Build new entry: "score,maxCombo,totalMatches,date"
        String date = new SimpleDateFormat("MM/dd", Locale.getDefault()).format(new Date());
        String newEntry = score + "," + maxCombo + "," + totalMatches + "," + date;

        // Load existing entries
        List<String> entries = loadRawEntries(difficulty);
        entries.add(0, newEntry);

        // Sort descending by score, keep top MAX_ENTRIES
        Collections.sort(entries, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return Integer.compare(parseScore(b), parseScore(a));
            }
        });
        if (entries.size() > MAX_ENTRIES) {
            entries = entries.subList(0, MAX_ENTRIES);
        }

        prefs.edit()
                .putString("scores_" + difficulty.name(), join(entries, ";"))
                .apply();

        return isRecord;
    }

    /** Returns list of score entries for a difficulty. Each entry is String[4]: score, maxCombo, totalMatches, date */
    public List<String[]> getScoreList(Difficulty difficulty) {
        List<String> raw = loadRawEntries(difficulty);
        List<String[]> result = new ArrayList<>();
        for (String entry : raw) {
            String[] parts = entry.split(",", -1);
            if (parts.length == 4) {
                result.add(parts);
            }
        }
        return result;
    }

    private List<String> loadRawEntries(Difficulty difficulty) {
        String raw = prefs.getString("scores_" + difficulty.name(), "");
        List<String> list = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return list;
        for (String s : raw.split(";")) {
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    private int parseScore(String entry) {
        try {
            return Integer.parseInt(entry.split(",")[0]);
        } catch (Exception e) {
            return 0;
        }
    }

    private String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
