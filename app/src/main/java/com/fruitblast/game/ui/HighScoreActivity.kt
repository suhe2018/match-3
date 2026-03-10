package com.fruitblast.game.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fruitblast.game.R
import com.fruitblast.game.data.Difficulty
import com.fruitblast.game.data.HighScore
import com.fruitblast.game.databinding.ActivityHighScoreBinding
import com.fruitblast.game.databinding.ItemHighScoreBinding
import com.fruitblast.game.utils.PreferenceHelper
import com.google.android.material.tabs.TabLayout

/**
 * High score leaderboard screen.
 * Shows top-5 scores per difficulty, switchable via tabs.
 */
class HighScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHighScoreBinding
    private lateinit var prefs: PreferenceHelper
    private lateinit var adapter: HighScoreAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHighScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceHelper(this)
        adapter = HighScoreAdapter(emptyList())

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupTabs()
        loadScores(Difficulty.EASY)

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🌱 简单"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🌿 普通"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🔥 困难"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val difficulty = when (tab.position) {
                    0 -> Difficulty.EASY
                    1 -> Difficulty.MEDIUM
                    else -> Difficulty.HARD
                }
                loadScores(difficulty)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun loadScores(difficulty: Difficulty) {
        val scores = prefs.getHighScores(difficulty)
        adapter.updateData(scores)

        if (scores.isEmpty()) {
            binding.tvEmpty.visibility = android.view.View.VISIBLE
            binding.recyclerView.visibility = android.view.View.GONE
        } else {
            binding.tvEmpty.visibility = android.view.View.GONE
            binding.recyclerView.visibility = android.view.View.VISIBLE
        }
    }
}

class HighScoreAdapter(private var scores: List<HighScore>) :
    RecyclerView.Adapter<HighScoreAdapter.VH>() {

    inner class VH(val binding: ItemHighScoreBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHighScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val hs = scores[position]
        val medal = when (position) {
            0 -> "🥇"
            1 -> "🥈"
            2 -> "🥉"
            else -> "${position + 1}."
        }
        holder.binding.tvRank.text = medal
        holder.binding.tvScore.text = hs.score.toString()
        holder.binding.tvCombo.text = "最高连击: ×${hs.maxCombo}"
        holder.binding.tvDate.text = hs.formattedDate
        holder.binding.tvBadge.text = hs.rank
    }

    override fun getItemCount() = scores.size

    fun updateData(newScores: List<HighScore>) {
        scores = newScores
        notifyDataSetChanged()
    }
}
