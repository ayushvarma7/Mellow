package edu.northeastern.mellow.ui.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.R;
import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.model.MellowUser;
import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.JournalRepository;
import edu.northeastern.mellow.data.repository.MoodRepository;
import edu.northeastern.mellow.ui.auth.SignInActivity;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    @Inject MoodRepository moodRepository;
    @Inject JournalRepository journalRepository;
    @Inject AuthRepository authRepository;

    private ProfileViewModel viewModel;

    private TextView tvAvatarInitial, tvDisplayName, tvUsername, tvEmail;
    private TextView tvStatCoins, tvStatStreak, tvStatBest;
    private TextView tvLevelChip, tvLevelNext, tvBadgeCount;
    private TextView tvActMoods, tvActJournals, tvActJars, tvActSince;
    private View levelFill;
    private LinearLayout badgeRow;
    private ChipGroup chipGroupGoals;

    private final int[] weekFaceIds = {R.id.week1, R.id.week2, R.id.week3, R.id.week4,
                                       R.id.week5, R.id.week6, R.id.week7};
    private final int[] weekLabelIds = {R.id.week1Label, R.id.week2Label, R.id.week3Label,
                                        R.id.week4Label, R.id.week5Label, R.id.week6Label, R.id.week7Label};

    private static final int[] FACE_RES = {
            R.drawable.mood_face_depressed, R.drawable.mood_face_sad, R.drawable.mood_face_neutral,
            R.drawable.mood_face_happy, R.drawable.mood_face_overjoyed};

    /** Level thresholds in total coins, with the name earned at each. */
    private static final int[] LEVEL_AT = {0, 50, 150, 300, 500, 800};
    private static final String[] LEVEL_NAME = {
            "Seedling", "Sprouting", "Calm Cultivator", "Serene Soul", "Zen Gardener", "Mellow Master"};

    private int moodCount = 0, journalCount = 0;
    private long jarsFilled = 0;
    private int currentStreak = 0, bestStreak = 0;

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
        tvLevelChip     = findViewById(R.id.tv_level_chip);
        tvLevelNext     = findViewById(R.id.tv_level_next);
        tvBadgeCount    = findViewById(R.id.tv_badge_count);
        tvActMoods      = findViewById(R.id.tv_act_moods);
        tvActJournals   = findViewById(R.id.tv_act_journals);
        tvActJars       = findViewById(R.id.tv_act_jars);
        tvActSince      = findViewById(R.id.tv_act_since);
        levelFill       = findViewById(R.id.level_fill);
        badgeRow        = findViewById(R.id.badgeRow);
        chipGroupGoals  = findViewById(R.id.chip_group_goals);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.loadProfile();

        observeProfile();
        observeProgress();
        loadActivity();

        findViewById(R.id.btn_sign_out).setOnClickListener(v -> {
            viewModel.signOut();
            startActivity(new Intent(this, SignInActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        });
    }

    // ---------- profile ----------

    private void observeProfile() {
        viewModel.getUserProfile().observe(this, result -> {
            if (!result.isSuccess() || result.getData() == null) return;
            MellowUser user = result.getData();

            String name = user.getDisplayName();
            tvAvatarInitial.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase(Locale.US));
            tvDisplayName.setText(name);
            if (user.getUsername() != null) tvUsername.setText("@" + user.getUsername());
            if (user.getEmail() != null) tvEmail.setText(user.getEmail());

            if (user.getCreatedAt() > 0) {
                tvActSince.setText(java.time.Instant.ofEpochMilli(user.getCreatedAt())
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())));
            } else {
                tvActSince.setText("—");
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

            currentStreak = p.getCurrentStreakDays();
            bestStreak = p.getLongestStreakDays();
            jarsFilled = p.getContainersOpened();
            tvActJars.setText(String.valueOf(jarsFilled));

            applyLevel(p.getTotalCoins());
            buildBadges();
        });
    }

    /** Maps total coins onto a level, its name, and progress toward the next one. */
    private void applyLevel(long coins) {
        int idx = 0;
        for (int i = 0; i < LEVEL_AT.length; i++) if (coins >= LEVEL_AT[i]) idx = i;

        tvLevelChip.setText("Level " + (idx + 1) + " · " + LEVEL_NAME[idx]);

        float fraction;
        if (idx >= LEVEL_AT.length - 1) {
            fraction = 1f;
            tvLevelNext.setText("Max level reached");
        } else {
            int from = LEVEL_AT[idx], to = LEVEL_AT[idx + 1];
            fraction = Math.max(0f, Math.min(1f, (coins - from) / (float) (to - from)));
            tvLevelNext.setText((to - coins) + " coins to " + LEVEL_NAME[idx + 1]);
        }

        View track = (View) levelFill.getParent();
        final float f = fraction;
        track.post(() -> {
            ViewGroup.LayoutParams lp = levelFill.getLayoutParams();
            lp.width = Math.round(track.getWidth() * f);
            levelFill.setLayoutParams(lp);
        });
    }

    // ---------- activity + week strip ----------

    private void loadActivity() {
        String uid = authRepository.getCurrentUid();
        if (uid == null) return;

        moodRepository.getMoodHistory(uid, 366, mres -> {
            List<MoodEntry> moods = (mres.isSuccess() && mres.getData() != null)
                    ? mres.getData() : new ArrayList<>();
            moodCount = moods.size();

            Map<LocalDate, Integer> byDay = new HashMap<>();
            for (MoodEntry m : moods) {
                if (m.getDate() == null || m.getMoodScore() < 1 || m.getMoodScore() > 5) continue;
                try {
                    LocalDate d = LocalDate.parse(m.getDate());
                    if (!byDay.containsKey(d)) byDay.put(d, m.getMoodScore()); // newest-first
                } catch (Exception ignored) {}
            }

            journalRepository.getJournalHistory(uid, 366, jres -> {
                List<JournalEntry> journals = (jres.isSuccess() && jres.getData() != null)
                        ? jres.getData() : new ArrayList<>();
                journalCount = journals.size();

                runOnUiThread(() -> {
                    tvActMoods.setText(String.valueOf(moodCount));
                    tvActJournals.setText(String.valueOf(journalCount));
                    renderWeek(byDay);
                    buildBadges();
                });
            });
        });
    }

    private void renderWeek(Map<LocalDate, Integer> byDay) {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            Integer score = byDay.get(day);

            ImageView face = findViewById(weekFaceIds[i]);
            TextView label = findViewById(weekLabelIds[i]);

            face.setImageResource(score == null ? R.drawable.mood_face_neutral : FACE_RES[score - 1]);
            face.setAlpha(score == null ? 0.3f : 1f);
            label.setText(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            label.setTextColor(getColor(day.equals(today) ? R.color.mellow_honey_deep : R.color.mellow_ink_2));
        }
    }

    // ---------- achievements ----------

    private void buildBadges() {
        if (badgeRow == null) return;
        badgeRow.removeAllViews();

        List<Object[]> badges = new ArrayList<>(); // {label, icon, tintColor, softColor, earned}
        badges.add(new Object[]{"First\nbreath", R.drawable.ic_leaf, R.color.mellow_coral_deep,
                R.color.mellow_coral_soft, jarsFilled > 0 || currentStreak > 0});
        badges.add(new Object[]{"3-day\nstreak", R.drawable.ic_flame, R.color.mellow_honey_deep,
                R.color.mellow_honey_soft, bestStreak >= 3});
        badges.add(new Object[]{"7-day\nstreak", R.drawable.ic_flame, R.color.mellow_coral_deep,
                R.color.mellow_coral_soft, bestStreak >= 7});
        badges.add(new Object[]{"Jar\nfilled", R.drawable.ic_jar, R.color.mellow_honey_deep,
                R.color.mellow_honey_soft, jarsFilled >= 1});
        badges.add(new Object[]{"Mood\ntracker", R.drawable.ic_mood, R.color.mellow_sky_deep,
                R.color.mellow_sky_soft, moodCount >= 7});
        badges.add(new Object[]{"Journal\nkeeper", R.drawable.ic_journal, R.color.mellow_lav_deep,
                R.color.mellow_lav_soft, journalCount >= 5});
        badges.add(new Object[]{"30-day\nstreak", R.drawable.ic_trophy, R.color.mellow_sage_deep,
                R.color.mellow_sage_soft, bestStreak >= 30});

        int earned = 0;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Object[] b : badges) {
            boolean isEarned = (Boolean) b[4];
            if (isEarned) earned++;

            View item = inflater.inflate(R.layout.item_badge, badgeRow, false);
            View circle = item.findViewById(R.id.badgeCircle);
            ImageView icon = item.findViewById(R.id.badgeIcon);
            TextView label = item.findViewById(R.id.badgeLabel);

            label.setText((String) b[0]);
            icon.setImageResource((Integer) b[1]);

            if (isEarned) {
                circle.setBackgroundTintList(ColorStateList.valueOf(getColor((Integer) b[3])));
                icon.setImageTintList(ColorStateList.valueOf(getColor((Integer) b[2])));
                icon.setAlpha(1f);
                label.setTextColor(getColor(R.color.mellow_ink));
            } else {
                circle.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.mellow_canvas_2)));
                icon.setImageTintList(ColorStateList.valueOf(getColor(R.color.mellow_ink_3)));
                icon.setAlpha(0.45f);
                label.setTextColor(getColor(R.color.mellow_ink_3));
            }
            badgeRow.addView(item);
        }
        tvBadgeCount.setText(earned + " of " + badges.size());
    }

    // ---------- goals ----------

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
