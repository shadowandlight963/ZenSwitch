package com.example.ourmajor.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.text.TextUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.example.ourmajor.ui.activities.SmartShuffleViewModelNew
import com.example.ourmajor.ui.activities.SmartShuffleViewModelNewFactory
import com.example.ourmajor.R

class ActivitiesFragment : Fragment() {
    private lateinit var list: RecyclerView
    private lateinit var adapter: ActivityAdapter
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var surpriseButton: com.google.android.material.button.MaterialButton
    private var selectedCategory: String = "breathing" // Matches MainCategory id
    private lateinit var activitiesViewModel: ActivitiesViewModel
    private lateinit var smartShuffleViewModel: SmartShuffleViewModelNew
    private var allActivities: List<ActivityItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, _savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_activities, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)
        
        // Initialize RecyclerView
        list = view.findViewById(R.id.activitiesList)
        list.layoutManager = LinearLayoutManager(requireContext())
        adapter = ActivityAdapter()
        adapter.setOnItemClick { item ->
            // TODO: Navigate to ActivityDetailFragment
        }
        adapter.setOnFavoriteClick { activityId, isFavorite ->
            activitiesViewModel.toggleFavorite(activityId)
        }
        list.adapter = adapter

        // Initialize ChipGroups
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories)
        
        // Initialize Surprise Button
        surpriseButton = view.findViewById(R.id.btnSurprise)

        // Initialize ViewModels
        activitiesViewModel = ViewModelProvider(requireActivity())[ActivitiesViewModel::class.java]
        smartShuffleViewModel = SmartShuffleViewModelNewFactory().create(SmartShuffleViewModelNew::class.java)
        
        
        // Observe activities state
        activitiesViewModel.uiState.observe(viewLifecycleOwner) { state ->
            state.selectedMainCategory?.id?.let { selectedCategory = it }

            // Build main category chips using enhanced data
            chipGroupCategories.removeAllViews()
            
            state.mainCategories.forEach { category ->
                val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle).apply {
                    text = "${category.icon} ${category.title}"
                    tag = category.id
                }
                styleChip(chip, category.id == selectedCategory)
                chipGroupCategories.addView(chip)
            }
            
            // Store all sub-activities and apply filters
            allActivities = state.subActivities.map { 
                ActivityItem(it.title, it.description, it.durationMinutes, it.categoryId)
            }
            
            // Apply both category and search filters
            applyCategoryFilters()
            
            // Pass favorites to adapter
            adapter.setFavorites(state.favorites)
        }
        
        // Setup Surprise button click listener
        setupSurpriseButton()
    }

    private fun styleChip(chip: Chip, checked: Boolean) {
        chip.setOnCheckedChangeListener(null)
        chip.isCheckable = true
        chip.isChecked = checked
        
        // Optimize padding and text size for horizontal display
        chip.isSingleLine = true
        chip.setLines(1)
        chip.ellipsize = TextUtils.TruncateAt.END

        // Disable min touch target size for compact design
        chip.setEnsureMinTouchTargetSize(false)

        chip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selectCategory((chip.tag as? String) ?: chip.text.toString())
            // refresh styles for all chips
            for (i in 0 until chipGroupCategories.childCount) {
                val ch = chipGroupCategories.getChildAt(i) as Chip
                val cat = (ch.tag as? String) ?: ch.text.toString()
                styleChip(ch, cat == selectedCategory)
            }
        }
    }

    private fun selectCategory(category: String) {
        selectedCategory = category
        // Update ViewModel with selected category
        activitiesViewModel.selectMainCategory(category)
        
        // update chip styles
        for (i in 0 until chipGroupCategories.childCount) {
            val ch = chipGroupCategories.getChildAt(i) as Chip
            val cat = (ch.tag as? String) ?: ch.text.toString()
            ch.isChecked = cat == category
        }
        
        // Apply category filters only (no search)
        applyCategoryFilters()
    }

    private fun setupSurpriseButton() {
        surpriseButton.setOnClickListener {
            Log.d("ACTIVITIES_DEBUG", "Surprise button clicked")
            
            // Disable button to prevent multiple rapid clicks
            surpriseButton.isEnabled = false
            
            try {
                // Get next activity class from fragment-scoped ViewModel
                val nextActivityClass = smartShuffleViewModel.getNextActivity()
                Log.d("ACTIVITIES_DEBUG", "Launching activity: ${nextActivityClass.simpleName}")
                
                // Standardized launch using Intent
                val intent = android.content.Intent(requireActivity(), nextActivityClass)
                startActivity(intent)
                
                // Re-enable button after 500ms
                surpriseButton.postDelayed({
                    surpriseButton.isEnabled = true
                }, 500)
                
            } catch (e: Exception) {
                Log.e("ACTIVITIES_DEBUG", "Failed to launch activity", e)
                // Re-enable button on error
                surpriseButton.isEnabled = true
            }
        }
    }

    private fun applyCategoryFilters() {
        val filtered = allActivities.filter { it.category == selectedCategory }
        adapter.submitList(filtered)
    }
}