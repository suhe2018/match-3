package com.fruitblast.game;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HighScoreAdapter extends RecyclerView.Adapter<HighScoreAdapter.VH> {

    private static final String[] RANK_MEDALS = {"🥇", "🥈", "🥉", "🏅", "🏅"};
    private static final String[] RANK_BADGES = {"传奇", "钻石", "黄金", "白银", "青铜"};

    private List<String[]> entries;  // each String[4]: score, maxCombo, totalMatches, date

    public HighScoreAdapter(List<String[]> entries) {
        this.entries = entries;
    }

    public void setEntries(List<String[]> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_high_score, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String[] entry = entries.get(position);
        int score = 0;
        int maxCombo = 0;
        try { score = Integer.parseInt(entry[0]); } catch (NumberFormatException ignored) {}
        try { maxCombo = Integer.parseInt(entry[1]); } catch (NumberFormatException ignored) {}

        holder.tvRank.setText(position < RANK_MEDALS.length ? RANK_MEDALS[position] : "🏅");
        holder.tvScore.setText(String.valueOf(score));
        holder.tvCombo.setText("最高连击 ×" + maxCombo);
        holder.tvBadge.setText(getRankBadge(score));
        holder.tvDate.setText(entry.length > 3 ? entry[3] : "");
    }

    @Override
    public int getItemCount() {
        return entries == null ? 0 : entries.size();
    }

    private String getRankBadge(int score) {
        if (score >= 5000) return "🏆 传奇";
        if (score >= 2000) return "💎 钻石";
        if (score >= 1000) return "🥇 黄金";
        if (score >= 500)  return "🥈 白银";
        if (score >= 100)  return "🥉 青铜";
        return "🌱 新手";
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvRank, tvScore, tvCombo, tvBadge, tvDate;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRank  = itemView.findViewById(R.id.tvRank);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvCombo = itemView.findViewById(R.id.tvCombo);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvDate  = itemView.findViewById(R.id.tvDate);
        }
    }
}
