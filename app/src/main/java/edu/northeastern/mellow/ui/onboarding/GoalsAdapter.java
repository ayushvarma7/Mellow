package edu.northeastern.mellow.ui.onboarding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.northeastern.mellow.R;

public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {

    public interface OnSelectionChanged {
        void onChanged(List<String> selectedGoals);
    }

    private final List<String> goals;
    private final Set<Integer> selectedPositions = new HashSet<>();
    private final OnSelectionChanged listener;

    public GoalsAdapter(List<String> goals, OnSelectionChanged listener) {
        this.goals = goals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        String goal = goals.get(position);
        boolean selected = selectedPositions.contains(position);

        holder.tvGoal.setText(goal);
        applySelectionStyle(holder, selected);

        holder.card.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (selectedPositions.contains(pos)) {
                selectedPositions.remove(pos);
            } else {
                selectedPositions.add(pos);
            }
            notifyItemChanged(pos);
            listener.onChanged(getSelectedGoals());
        });
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public List<String> getSelectedGoals() {
        List<String> selected = new ArrayList<>();
        for (int pos : selectedPositions) {
            selected.add(goals.get(pos));
        }
        return selected;
    }

    private void applySelectionStyle(GoalViewHolder holder, boolean selected) {
        Context ctx = holder.card.getContext();
        int strokePx = Math.round(1.5f * ctx.getResources().getDisplayMetrics().density);
        if (selected) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.mellow_honey));
            holder.card.setStrokeWidth(0);
            holder.tvGoal.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.mellow_surface));
            holder.card.setStrokeColor(ContextCompat.getColor(ctx, R.color.mellow_line));
            holder.card.setStrokeWidth(strokePx);
            holder.tvGoal.setTextColor(ContextCompat.getColor(ctx, R.color.mellow_ink));
        }
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvGoal;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            card   = (MaterialCardView) itemView;
            tvGoal = itemView.findViewById(R.id.tv_goal);
        }
    }
}
