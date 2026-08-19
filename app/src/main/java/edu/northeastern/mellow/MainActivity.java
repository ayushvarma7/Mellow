package edu.northeastern.mellow;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import java.util.Calendar;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.data.model.UserProgress;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.ui.auth.AuthViewModel;
import edu.northeastern.mellow.ui.auth.SignInActivity;
import edu.northeastern.mellow.ui.buddy.BuddyActivity;
import edu.northeastern.mellow.ui.journal.JournalViewModel;
import edu.northeastern.mellow.ui.profile.ProfileActivity;
import edu.northeastern.mellow.ui.progress.ProgressViewModel;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    // UI views
    private TextView tvGreeting, tvAffirmation, tvCoinsCount, tvStreak;
    private String baseGreeting = "Hello";
    private TextView tvDateLabel, tvJarCoins, tvJarCap, tvJarSub, tvJarNote;
    private View heroFill, heroSpacer;
    private View btnStartCheckin;
    private LinearLayout cardJournal;
    private TextView btnViewJournalHistory;

    @Inject
    AuthRepository authRepository;

    // ViewModels
    private AuthViewModel authViewModel;
    private ProgressViewModel progressViewModel;
    private JournalViewModel journalViewModel;

    // State
    private boolean observingStarted = false;
    private boolean isDark = true;
    private Dialog currentDialog;
    private boolean isDialogShowing = false;
    private String savedTitle = "";
    private String savedContent = "";
    private int savedMood = 3;

    private static final String PREFS_NAME = "mellow_prefs";
    private static final String KEY_IS_DARK = "isDark";

    private static final String[] AFFIRMATIONS = {
            "Every breath you take is a step toward inner peace.",
            "You are doing better than you think.",
            "Small steps every day lead to big changes.",
            "Be gentle with yourself — you are growing.",
            "Today is a fresh start. Make it yours.",
            "Your calm is your superpower.",
            "Progress, not perfection.",
            "You deserve rest as much as you deserve success."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        initViewModels();
        restoreDialogIfNeeded(savedInstanceState);
        setupGreeting();
        setupAffirmation();
        setupAuthObserver();
        setupProgressObserver();
        setupClickListeners();
        setupThemeToggle();
    }

    private void bindViews() {
        tvGreeting            = findViewById(R.id.tvGreeting);
        tvAffirmation         = findViewById(R.id.tvAffirmation);
        tvCoinsCount          = findViewById(R.id.tvCoinsCount);
        tvStreak              = findViewById(R.id.tvStreak);
        tvDateLabel           = findViewById(R.id.tvDateLabel);
        tvJarCoins            = findViewById(R.id.tvJarCoins);
        tvJarCap              = findViewById(R.id.tvJarCap);
        tvJarSub              = findViewById(R.id.tvJarSub);
        tvJarNote             = findViewById(R.id.tvJarNote);
        heroFill              = findViewById(R.id.heroFill);
        heroSpacer            = findViewById(R.id.heroSpacer);
        btnStartCheckin       = findViewById(R.id.btnStartCheckin);
        cardJournal           = findViewById(R.id.cardJournal);
        btnViewJournalHistory = findViewById(R.id.btnViewJournalHistory);
    }

    private void initViewModels() {
        authViewModel     = new ViewModelProvider(this).get(AuthViewModel.class);
        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);
        journalViewModel  = new ViewModelProvider(this).get(JournalViewModel.class);
    }

    private void restoreDialogIfNeeded(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isDialogShowing = savedInstanceState.getBoolean("isDialogShowing", false);
            savedTitle      = savedInstanceState.getString("savedTitle", "");
            savedContent    = savedInstanceState.getString("savedContent", "");
            savedMood       = savedInstanceState.getInt("savedMood", 3);
            if (isDialogShowing) showJournalDialog(savedTitle, savedContent, savedMood);
        }
    }

    private void setupGreeting() {
        setupEntranceAnimations();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12)       baseGreeting = "Good morning";
        else if (hour < 17)  baseGreeting = "Good afternoon";
        else                 baseGreeting = "Good evening";
        tvGreeting.setText(baseGreeting);
        loadGreetingName();

        String date = new java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvDateLabel.setText(date.toUpperCase(java.util.Locale.getDefault()));
    }

    private void setupAffirmation() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        tvAffirmation.setText(AFFIRMATIONS[dayOfYear % AFFIRMATIONS.length]);
    }

    private void setupAuthObserver() {
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) {
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return;
            }
            if (!observingStarted) {
                observingStarted = true;
                progressViewModel.startObserving();
                journalViewModel.startObserving();
            }
        });
    }

    private void setupProgressObserver() {
        progressViewModel.getProgress().observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                UserProgress p = result.getData();

                tvCoinsCount.setText(String.valueOf(p.getTotalCoins()));

                int streak = p.getCurrentStreakDays();
                tvStreak.setText(streak + (streak == 1 ? " day" : " days"));
                if (streak > 0) pulseView(tvStreak);

                updateHero(p.getCurrentContainerCoins(), p.getContainerCapacity());
            }
        });
    }

    private void setupClickListeners() {
        btnStartCheckin.setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, CheckInActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        cardJournal.setOnClickListener(v ->
                animatePress(v, () -> showJournalDialog("", "", 3)));

        btnViewJournalHistory.setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, JournalHistoryActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.fabMenu).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, ProfileActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.navHome).setOnClickListener(v ->
                animatePress(v, () ->
                        Toast.makeText(this, "You're already home 🏠", Toast.LENGTH_SHORT).show()));

        findViewById(R.id.navFriends).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, BuddyActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.navRewards).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, MoodTrendActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.navCheckIn).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, CheckInActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.navProfile).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, ProfileActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.tileMood).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, MoodTrendActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));

        findViewById(R.id.tileBuddies).setOnClickListener(v ->
                animatePress(v, () -> {
                    startActivity(new Intent(this, BuddyActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }));
    }

    private void setupThemeToggle() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDark = prefs.getBoolean(KEY_IS_DARK, true);

        TextView btnThemeToggle = findViewById(R.id.btnThemeToggle);
        btnThemeToggle.setText(isDark ? "☀️" : "🌙");

        btnThemeToggle.setOnClickListener(v ->
                animatePress(v, this::showSettingsDialog));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentDialog != null && currentDialog.isShowing()) {
            EditText etTitle   = currentDialog.findViewById(R.id.etJournalTitle);
            EditText etContent = currentDialog.findViewById(R.id.etJournalContent);
            outState.putBoolean("isDialogShowing", true);
            outState.putString("savedTitle",   etTitle   != null ? etTitle.getText().toString() : "");
            outState.putString("savedContent", etContent != null ? etContent.getText().toString() : "");
            outState.putInt("savedMood", savedMood);
        }
    }

    private void setupEntranceAnimations() {
        tvGreeting.setAlpha(0f);
        tvAffirmation.setAlpha(0f);
        tvCoinsCount.setAlpha(0f);
        tvStreak.setAlpha(0f);
        btnStartCheckin.setAlpha(0f);

        tvGreeting.setTranslationY(30f);
        tvAffirmation.setTranslationY(30f);
        tvCoinsCount.setTranslationY(30f);
        tvStreak.setTranslationY(30f);
        btnStartCheckin.setTranslationY(40f);

        tvGreeting.animate().alpha(1f).translationY(0f).setDuration(350).start();
        tvAffirmation.animate().alpha(1f).translationY(0f).setStartDelay(100).setDuration(350).start();
        tvCoinsCount.animate().alpha(1f).translationY(0f).setStartDelay(180).setDuration(350).start();
        tvStreak.animate().alpha(1f).translationY(0f).setStartDelay(260).setDuration(350).start();
        btnStartCheckin.animate().alpha(1f).translationY(0f).setStartDelay(340).setDuration(400).start();
    }

    /** Greets the user by the name they gave during onboarding. */
    private void loadGreetingName() {
        String uid = authRepository.getCurrentUid();
        if (uid == null) return;
        authRepository.fetchUserProfile(uid, result -> runOnUiThread(() -> {
            if (!result.isSuccess() || result.getData() == null) return;
            String name = result.getData().getDisplayName();
            if (name == null || name.trim().isEmpty()) return;
            String first = name.trim().split("\\s+")[0];
            tvGreeting.setText(baseGreeting + ", " + first);
        }));
    }

    private void showJournalDialog(String title, String content, int mood) {
        if (currentDialog != null && currentDialog.isShowing()) currentDialog.dismiss();

        currentDialog = new Dialog(this);
        currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        currentDialog.setContentView(R.layout.dialog_journal_entry);
        currentDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.view.WindowManager.LayoutParams params = currentDialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
        currentDialog.getWindow().setAttributes(params);

        EditText etTitle   = currentDialog.findViewById(R.id.etJournalTitle);
        EditText etContent = currentDialog.findViewById(R.id.etJournalContent);
        etTitle.setText(title);
        etContent.setText(content);

        CardView[] moodCards = {
                currentDialog.findViewById(R.id.cardMood1),
                currentDialog.findViewById(R.id.cardMood2),
                currentDialog.findViewById(R.id.cardMood3),
                currentDialog.findViewById(R.id.cardMood4),
                currentDialog.findViewById(R.id.cardMood5)
        };

        savedMood = mood;
        highlightMoodCard(moodCards, mood - 1);

        for (int i = 0; i < moodCards.length; i++) {
            int moodScore = i + 1;
            int index = i;
            moodCards[i].setOnClickListener(v -> {
                savedMood = moodScore;
                highlightMoodCard(moodCards, index);
            });
        }

        Button btnCancel = currentDialog.findViewById(R.id.btnCancel);
        Button btnSave   = currentDialog.findViewById(R.id.btnSave);

        // Date picker — defaults to today, capped at today (no future entries)
        final String[] pickedDate = { edu.northeastern.mellow.data.util.DateUtils.today() };
        TextView btnPickDate = currentDialog.findViewById(R.id.btnPickDate);
        if (btnPickDate != null) {
            btnPickDate.setText("Today");
            btnPickDate.setOnClickListener(v -> {
                java.time.LocalDate cur = java.time.LocalDate.parse(pickedDate[0]);
                android.app.DatePickerDialog dp = new android.app.DatePickerDialog(this, (view, y, m, d) -> {
                    java.time.LocalDate sel = java.time.LocalDate.of(y, m + 1, d);
                    pickedDate[0] = sel.toString();
                    btnPickDate.setText(sel.isEqual(java.time.LocalDate.now()) ? "Today"
                            : sel.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")));
                }, cur.getYear(), cur.getMonthValue() - 1, cur.getDayOfMonth());
                dp.getDatePicker().setMaxDate(System.currentTimeMillis());
                dp.show();
            });
        }

        btnCancel.setOnClickListener(v -> {
            isDialogShowing = false;
            currentDialog.dismiss();
        });

        btnSave.setOnClickListener(v -> {
            String c = etContent.getText().toString().trim();
            if (c.isEmpty()) {
                Toast.makeText(this, "Please write something in your journal", Toast.LENGTH_SHORT).show();
                return;
            }
            String t = etTitle.getText().toString().trim();
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");
            journalViewModel.saveJournal(t.isEmpty() ? null : t, c, savedMood, pickedDate[0]);
            journalViewModel.getIsSaving().observe(this, isSaving -> {
                if (!isSaving) {
                    Toast.makeText(this, "Journal saved! +10-15 coins earned 🪙", Toast.LENGTH_SHORT).show();
                    isDialogShowing = false;
                    currentDialog.dismiss();
                }
            });
        });

        currentDialog.setOnDismissListener(d -> isDialogShowing = false);
        isDialogShowing = true;
        currentDialog.show();
    }

    private void highlightMoodCard(CardView[] cards, int selectedIndex) {
        for (int i = 0; i < cards.length; i++) {
            cards[i].setCardBackgroundColor(getResources().getColor(
                    i == selectedIndex ? R.color.mellow_accent_gold : R.color.mellow_bg_mid, null));
        }
    }

    private void updateHero(long current, long capacity) {
        if (capacity <= 0) capacity = 7;
        long shown = Math.max(0, Math.min(current, capacity));
        float ratio = (float) shown / (float) capacity;
        long remaining = Math.max(0, capacity - shown);

        tvJarCoins.setText(String.valueOf(shown));
        tvJarCap.setText("/ " + capacity + " coins");

        if (remaining == 0) {
            tvJarSub.setText("Your jar is full — reward ready!");
            tvJarNote.setText("TAP CHECK-IN TO CLAIM ✨");
        } else if (ratio >= 0.6f) {
            tvJarSub.setText("Your jar is almost full ✨");
            tvJarNote.setText(remaining + " MORE TO UNLOCK A REWARD");
        } else {
            tvJarSub.setText("Complete check-ins to fill your jar");
            tvJarNote.setText(remaining + " MORE TO UNLOCK A REWARD");
        }

        setHeroFill(ratio);
    }

    private void setHeroFill(float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        LinearLayout.LayoutParams fp = (LinearLayout.LayoutParams) heroFill.getLayoutParams();
        LinearLayout.LayoutParams sp = (LinearLayout.LayoutParams) heroSpacer.getLayoutParams();
        fp.weight = ratio * 100f;
        sp.weight = (1f - ratio) * 100f;
        heroFill.setLayoutParams(fp);
        heroSpacer.setLayoutParams(sp);
    }

    private void pulseView(View view) {
        ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 1f).setDuration(220).start();
        ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 1f).setDuration(220).start();
    }

    private void animatePress(View view, Runnable action) {
        view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f).scaleY(1f).setDuration(70)
                        .withEndAction(action).start())
                .start();
    }

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_settings);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setAttributes(params);

        TextView tvThemeValue = dialog.findViewById(R.id.tvThemeValue);
        Button btnToggleTheme = dialog.findViewById(R.id.btnToggleTheme);
        Button btnCloseSettings = dialog.findViewById(R.id.btnCloseSettings);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        tvThemeValue.setText(isDark ? "Dark" : "Light");
        btnToggleTheme.setText(isDark ? "Switch to Light Mode" : "Switch to Dark Mode");

        btnToggleTheme.setOnClickListener(v -> {
            isDark = !isDark;
            prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            dialog.dismiss();
        });

        btnCloseSettings.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}