package edu.northeastern.mellow.ui.onboarding;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.northeastern.mellow.R;

/**
 * Step 3 — a searchable cloud of things that make you happy. Picked tags take
 * a colour from the mood palette and mirror into the "Selected" row.
 */
public class HappyThingsFragment extends Fragment {

    private static final List<String> TAGS = Arrays.asList(
            "Dialogue", "Mastery", "Progress", "Creation", "Connection", "Empathy",
            "Stability", "Challenge", "Validation", "Growth", "Clarity", "Exploration",
            "Evolution", "Impact", "Playfulness", "Learning", "Improvement", "Nature",
            "Music", "Pets", "Family", "Friends", "Travel", "Reading", "Cooking",
            "Exercise", "Sleep", "Jokes", "Art", "Quiet", "Sunshine", "Rain",
            "Coffee", "Movies", "Dancing", "Writing", "Gaming", "Volunteering");

    /** Selected chips cycle through these so the cloud looks alive. */
    private static final int[] PICKED = {
            R.color.mood_overjoyed, R.color.mood_sad, R.color.mood_depressed,
            R.color.mood_happy, R.color.mellow_sky};

    private final Set<String> selected = new LinkedHashSet<>();
    private ChipGroup cloud, selectedGroup;
    private TextView tvTotal;
    private OnboardingViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_happy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        cloud = view.findViewById(R.id.chipsHappy);
        selectedGroup = view.findViewById(R.id.chipsSelected);
        tvTotal = view.findViewById(R.id.tvHappyTotal);
        EditText search = view.findViewById(R.id.etHappySearch);
        MaterialButton btn = view.findViewById(R.id.btnHappyContinue);

        if (vm.getHappyThings() != null) selected.addAll(vm.getHappyThings());

        buildCloud("");
        refreshSelectedRow();

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                buildCloud(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btn.setOnClickListener(v -> {
            vm.setHappyThings(new ArrayList<>(selected));
            ((OnboardingActivity) requireActivity()).nextPage();
        });
    }

    private void buildCloud(String query) {
        cloud.removeAllViews();
        String q = query.trim().toLowerCase(Locale.US);
        int shown = 0;

        for (String tag : TAGS) {
            if (!q.isEmpty() && !tag.toLowerCase(Locale.US).contains(q)) continue;
            shown++;
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setChecked(selected.contains(tag));
            chip.setChipStrokeWidth(0f);
            chip.setCheckedIconVisible(false);
            chip.setTextSize(13.5f);
            paintChip(chip, tag, selected.contains(tag));

            chip.setOnClickListener(v -> {
                if (selected.contains(tag)) selected.remove(tag);
                else selected.add(tag);
                paintChip(chip, tag, selected.contains(tag));
                refreshSelectedRow();
            });
            cloud.addView(chip);
        }
        tvTotal.setText(shown + " Total");
    }

    private void paintChip(Chip chip, String tag, boolean isSelected) {
        chip.setChecked(isSelected);
        if (isSelected) {
            int color = PICKED[Math.abs(tag.hashCode()) % PICKED.length];
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), color)));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.mellow_surface)));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.mellow_ink));
        }
    }

    private void refreshSelectedRow() {
        selectedGroup.removeAllViews();
        for (String tag : selected) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setTextSize(12f);
            chip.setCloseIconVisible(true);
            chip.setChipStrokeWidth(0f);
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.mellow_canvas_2)));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.mellow_ink_2));
            chip.setOnCloseIconClickListener(v -> {
                selected.remove(tag);
                buildCloud("");
                refreshSelectedRow();
            });
            selectedGroup.addView(chip);
        }
    }
}
