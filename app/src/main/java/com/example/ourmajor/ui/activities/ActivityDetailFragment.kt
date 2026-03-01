package com.example.ourmajor.ui.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ourmajor.R
import com.example.ourmajor.data.catalog.Activity
import com.example.ourmajor.data.progress.ActivitySession
import com.example.ourmajor.ui.activities.ActivitiesViewModel

class ActivityDetailFragment : Fragment() {
    private lateinit var activitiesViewModel: ActivitiesViewModel
    private var activityId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityId = arguments?.getString("activityId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, _savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_activity_detail, container, false)
    }

    override fun onViewCreated(view: View, _savedInstanceState: Bundle?) {
        super.onViewCreated(view, _savedInstanceState)

        activitiesViewModel = ViewModelProvider(requireActivity())[ActivitiesViewModel::class.java]

        // TODO: Find activity from catalog and display details
        // TODO: Add start button and favorite toggle
        // TODO: Load and display history for this activity
        // TODO: Handle start/abandon actions
    }

    companion object {
        fun newInstance(activityId: String): ActivityDetailFragment {
            return ActivityDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("activityId", activityId)
                }
            }
        }
    }
}
