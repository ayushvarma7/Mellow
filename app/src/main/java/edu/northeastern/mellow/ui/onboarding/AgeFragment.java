package edu.northeastern.mellow.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import edu.northeastern.mellow.R;

/**
 * Step 2 — age, picked on a snapping wheel. Items scale and fade with their
 * distance from the centre, so the chosen age reads big inside the pill.
 */
public class AgeFragment extends Fragment {

    private static final int MIN_AGE = 13;
    private static final int MAX_AGE = 99;

    private RecyclerView rv;
    private OnboardingViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_age, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        rv = view.findViewById(R.id.rvAge);
        MaterialButton btn = view.findViewById(R.id.btnAgeContinue);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false);
        rv.setLayoutManager(lm);
        rv.setAdapter(new AgeAdapter());
        new LinearSnapHelper().attachToRecyclerView(rv);

        // pad by half the viewport so the first/last ages can reach the centre
        rv.post(() -> {
            int pad = (rv.getHeight() - dp(76)) / 2;
            rv.setPadding(0, pad, 0, pad);
            int start = vm.getAge() > 0 ? vm.getAge() : 18;
            lm.scrollToPositionWithOffset(start - MIN_AGE, 0);
            rv.post(this::styleItems);
        });

        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView r, int dx, int dy) { styleItems(); }
            @Override public void onScrollStateChanged(@NonNull RecyclerView r, int state) {
                if (state == RecyclerView.SCROLL_STATE_IDLE) commitCentered();
            }
        });

        btn.setOnClickListener(v -> {
            commitCentered();
            ((OnboardingActivity) requireActivity()).nextPage();
        });
    }

    /** Scale + fade every visible row by how far it sits from the centre. */
    private void styleItems() {
        float mid = rv.getHeight() / 2f;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            float centre = (child.getTop() + child.getBottom()) / 2f;
            float dist = Math.min(1f, Math.abs(centre - mid) / (rv.getHeight() / 2.2f));
            float scale = 1f - 0.45f * dist;
            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setAlpha(1f - 0.72f * dist);

            TextView tv = child.findViewById(R.id.tvAge);
            boolean selected = dist < 0.12f;
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                    selected ? R.color.white : R.color.mellow_ink));
        }
    }

    private void commitCentered() {
        float mid = rv.getHeight() / 2f;
        View best = null; float bestDist = Float.MAX_VALUE;
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            float centre = (child.getTop() + child.getBottom()) / 2f;
            float d = Math.abs(centre - mid);
            if (d < bestDist) { bestDist = d; best = child; }
        }
        if (best == null) return;
        int pos = rv.getChildAdapterPosition(best);
        if (pos != RecyclerView.NO_POSITION) vm.setAge(MIN_AGE + pos);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private class AgeAdapter extends RecyclerView.Adapter<AgeAdapter.VH> {
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_age, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            h.tv.setText(String.valueOf(MIN_AGE + position));
        }
        @Override public int getItemCount() { return MAX_AGE - MIN_AGE + 1; }

        class VH extends RecyclerView.ViewHolder {
            final TextView tv;
            VH(@NonNull View v) { super(v); tv = v.findViewById(R.id.tvAge); }
        }
    }
}
