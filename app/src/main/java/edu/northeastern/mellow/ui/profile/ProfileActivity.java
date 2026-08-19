package edu.northeastern.mellow.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.R;
import edu.northeastern.mellow.data.model.MellowUser;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.ui.auth.SignInActivity;
import android.widget.TextView;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;

    private TextView tvAvatarInitial, tvDisplayName, tvUsername, tvEmail;
    private TextView tvStatCoins, tvStatStreak, tvStatBest;
    private ChipGroup chipGroupGoals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvAvatarInitial = findViewById(R.id.tv_avatar_initial);
        tvDisplayName   = findViewById(R.id.tv_display_name);
        tvUsername      = findViewById(R.id.tv_username);
        tvEmail         = findViewById(R.id.tv_email);
        tvStatCoins     = findViewById(R.id.tv_stat_coins);
        tvStatStreak    = findViewById(R.id.tv_stat_streak);
        tvStatBest      = findViewById(R.id.tv_stat_best);
        chipGroupGoals  = findViewById(R.id.chip_group_goals);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.loadProfile();

        observeProfile();
        observeProgress();

        findViewById(R.id.btn_sign_out).setOnClickListener(v -> {
            viewModel.signOut();
            startActivity(new Intent(this, SignInActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }

    private void observeProfile() {
        viewModel.getUserProfile().observe(this, result -> {
            if (!result.isSuccess() || result.getData() == null) return;
            MellowUser user = result.getData();

            String name = user.getDisplayName();
            tvAvatarInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
            tvDisplayName.setText(name);

            if (user.getUsername() != null) {
                tvUsername.setText("@" + user.getUsername());
            }
            if (user.getEmail() != null) {
                tvEmail.setText(user.getEmail());
            }

            buildGoalChips(user.getGoals());
        });
    }

    private void observeProgress() {
        viewModel.getProgress().observe(this, result -> {
            if (!result.isSuccess() || result.getData() == null) return;
            UserProgress p = result.getData();
            tvStatCoins.setText(String.valueOf(p.getTotalCoins()));
            tvStatStreak.setText(String.valueOf(p.getCurrentStreakDays()));
            tvStatBest.setText(String.valueOf(p.getLongestStreakDays()));
        });
    }

    private void buildGoalChips(List<String> goals) {
        chipGroupGoals.removeAllViews();
        if (goals == null || goals.isEmpty()) {
            Chip chip = new Chip(this);
            chip.setText("No goals set");
            chip.setClickable(false);
            chipGroupGoals.addView(chip);
            return;
        }
        for (String goal : goals) {
            Chip chip = new Chip(this);
            chip.setText(goal);
            chip.setClickable(false);
            chip.setChipBackgroundColorResource(R.color.mellow_honey_soft);
            chip.setChipStrokeWidth(0f);
            chip.setTextColor(getColor(R.color.mellow_honey_deep));
            chipGroupGoals.addView(chip);
        }
    }
}
