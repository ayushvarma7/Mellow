package edu.northeastern.mellow.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

import edu.northeastern.mellow.MainActivity;
import edu.northeastern.mellow.R;

public class GoalsFragment extends Fragment {

    private static final List<String> GOALS = Arrays.asList(
            "Reduce stress",
            "Build mindfulness",
            "Improve sleep",
            "Track my mood",
            "Practice gratitude",
            "Stay accountable",
            "Manage anxiety",
            "General wellness"
    );

    private OnboardingViewModel viewModel;
    private GoalsAdapter adapter;
    private MaterialButton btnFinish;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnFinish = view.findViewById(R.id.btn_finish);
        RecyclerView rvGoals = view.findViewById(R.id.rv_goals);

        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        adapter = new GoalsAdapter(GOALS, selectedGoals -> {
            btnFinish.setEnabled(!selectedGoals.isEmpty());
        });

        rvGoals.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvGoals.setAdapter(adapter);

        btnFinish.setOnClickListener(v -> {
            List<String> selected = adapter.getSelectedGoals();
            if (selected.isEmpty()) return;
            viewModel.completeOnboarding(selected);
        });

        viewModel.getIsSaving().observe(getViewLifecycleOwner(), saving ->
                btnFinish.setEnabled(!saving && !adapter.getSelectedGoals().isEmpty()));

        viewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                startActivity(new Intent(requireContext(), MainActivity.class));
                requireActivity().finish();
            } else if (result.isError()) {
                Toast.makeText(requireContext(),
                        "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
