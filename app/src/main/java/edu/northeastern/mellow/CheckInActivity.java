package edu.northeastern.mellow;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.ui.checkin.WaveView;
import edu.northeastern.mellow.ui.progress.ProgressViewModel;

@AndroidEntryPoint
public class CheckInActivity extends AppCompatActivity {

    private TextView tvPrompt;
    private TextView tvBreathInstruction;
    private TextView tvBreathCount;
    private TextView tvCoinsEarned;
    private View tvMilestone1;
    private View tvMilestone2;
    private View tvMilestone3;
    private View tvMilestone4;
    private View progressFill;
    private View cardBreath;
    private Button btnBegin;
    private Button btnDone;
    private WaveView waveView;

    private ProgressViewModel progressViewModel;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private int currentBreath = 0;
    private int coinsEarned = 0;
    private boolean isBreathing = false;

    private final int[] rewards = {2, 3, 3, 5};

    private ObjectAnimator idlePulseX;
    private ObjectAnimator idlePulseY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        tvPrompt = findViewById(R.id.tvPrompt);
        tvBreathInstruction = findViewById(R.id.tvBreathInstruction);
        tvBreathCount = findViewById(R.id.tvBreathCount);
        tvCoinsEarned = findViewById(R.id.tvCoinsEarned);
        tvMilestone1 = findViewById(R.id.tvMilestone1);
        tvMilestone2 = findViewById(R.id.tvMilestone2);
        tvMilestone3 = findViewById(R.id.tvMilestone3);
        tvMilestone4 = findViewById(R.id.tvMilestone4);
        progressFill = findViewById(R.id.progressFill);
        cardBreath = findViewById(R.id.cardBreath);
        btnBegin = findViewById(R.id.btnBegin);
        btnDone = findViewById(R.id.btnDone);
        waveView = findViewById(R.id.waveView);

        progressViewModel = new ViewModelProvider(this).get(ProgressViewModel.class);

        resetUI();
        startIdlePulse();

        btnBegin.setOnClickListener(v -> {
            if (!isBreathing && currentBreath < 4) {
                btnBegin.setVisibility(View.GONE);
                stopIdlePulse();
                runBreathCycle();
            }
        });

        btnDone.setOnClickListener(v -> showRewardDialog());

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }

    private void resetUI() {
        currentBreath = 0;
        coinsEarned = 0;
        isBreathing = false;

        tvBreathInstruction.setText("Tap to begin");
        tvBreathCount.setText("Breath 0 / 4");
        tvCoinsEarned.setText("0 / 13 coins earned");

        btnBegin.setText("Begin");
        btnBegin.setEnabled(true);
        btnBegin.setVisibility(View.VISIBLE);
        btnDone.setVisibility(View.GONE);

        cardBreath.setScaleX(1f);
        cardBreath.setScaleY(1f);

        tvMilestone1.setAlpha(0.35f);
        tvMilestone2.setAlpha(0.35f);
        tvMilestone3.setAlpha(0.35f);
        tvMilestone4.setAlpha(0.35f);

        progressFill.post(() -> {
            progressFill.getLayoutParams().width = 0;
            progressFill.requestLayout();
        });

        if (waveView != null) waveView.animateLevel(0f, 400);
    }

    private void runBreathCycle() {
        isBreathing = true;

        tvBreathInstruction.setText("Breathe in");
        animateBreath(true);
        waveView.animateLevel(Math.min(0.8f, (currentBreath + 1) / 4f * 0.8f), 2000);

        handler.postDelayed(() -> tvBreathInstruction.setText("Hold"), 2000);

        handler.postDelayed(() -> {
            tvBreathInstruction.setText("Breathe out");
            animateBreath(false);
            waveView.animateLevel(Math.max(0f, (currentBreath + 1) / 4f * 0.8f - 0.08f), 2000);
        }, 3500);

        handler.postDelayed(() -> {
            completeBreath();
            if (currentBreath < 4) {
                // auto-advance — no tapping needed
                tvBreathInstruction.setText("Well done");
                handler.postDelayed(this::runBreathCycle, 900);
            } else {
                isBreathing = false;
                tvBreathInstruction.setText("All done 🌿");
                btnDone.setVisibility(View.VISIBLE);
                waveView.animateLevel(0.92f, 1200); // fill to the brim
            }
        }, 5500);
    }

    private void completeBreath() {
        if (currentBreath >= 4) return;

        coinsEarned += rewards[currentBreath];
        currentBreath++;

        tvBreathCount.setText("Breath " + currentBreath + " / 4");
        tvCoinsEarned.setText(coinsEarned + " / 13 coins earned");

        updateMilestones();
        updateProgressBar();
    }

    private void updateMilestones() {
        if (currentBreath >= 1) tvMilestone1.setAlpha(1f);
        if (currentBreath >= 2) tvMilestone2.setAlpha(1f);
        if (currentBreath >= 3) tvMilestone3.setAlpha(1f);
        if (currentBreath >= 4) tvMilestone4.setAlpha(1f);
    }

    private void updateProgressBar() {
        View parent = (View) progressFill.getParent();
        parent.post(() -> {
            int totalWidth = parent.getWidth();
            int targetWidth = (int) (totalWidth * (currentBreath / 4f));

            ValueAnimator animator = ValueAnimator.ofInt(progressFill.getLayoutParams().width, targetWidth);
            animator.setDuration(350);
            animator.addUpdateListener(animation -> {
                progressFill.getLayoutParams().width = (int) animation.getAnimatedValue();
                progressFill.requestLayout();
            });
            animator.start();
        });
    }

    private void animateBreath(boolean expand) {
        float startScale = expand ? cardBreath.getScaleX() : 1.18f;
        float endScale = expand ? 1.18f : 1f;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(cardBreath, "scaleX", startScale, endScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(cardBreath, "scaleY", startScale, endScale);

        scaleX.setDuration(1800);
        scaleY.setDuration(1800);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();
    }

    private void startIdlePulse() {
        stopIdlePulse();

        idlePulseX = ObjectAnimator.ofFloat(cardBreath, "scaleX", 1f, 1.04f);
        idlePulseY = ObjectAnimator.ofFloat(cardBreath, "scaleY", 1f, 1.04f);

        idlePulseX.setDuration(1400);
        idlePulseY.setDuration(1400);

        idlePulseX.setRepeatCount(ValueAnimator.INFINITE);
        idlePulseY.setRepeatCount(ValueAnimator.INFINITE);

        idlePulseX.setRepeatMode(ValueAnimator.REVERSE);
        idlePulseY.setRepeatMode(ValueAnimator.REVERSE);

        idlePulseX.setInterpolator(new AccelerateDecelerateInterpolator());
        idlePulseY.setInterpolator(new AccelerateDecelerateInterpolator());

        idlePulseX.start();
        idlePulseY.start();
    }

    private void stopIdlePulse() {
        if (idlePulseX != null) idlePulseX.cancel();
        if (idlePulseY != null) idlePulseY.cancel();
        cardBreath.setScaleX(1f);
        cardBreath.setScaleY(1f);
    }

    private void showRewardDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_breath_reward);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvRewardCoins = dialog.findViewById(R.id.tvRewardCoins);
        TextView tvRewardMessage = dialog.findViewById(R.id.tvRewardMessage);
        Button btnRewardDone = dialog.findViewById(R.id.btnRewardDone);

        tvRewardCoins.setText("+" + coinsEarned + " coins");
        tvRewardMessage.setText("You completed all 4 breaths and earned a calm reward moment.");

        btnRewardDone.setOnClickListener(v -> {
            progressViewModel.completeBreathingCheckIn(22000L);
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopIdlePulse();
        handler.removeCallbacksAndMessages(null);
    }
}