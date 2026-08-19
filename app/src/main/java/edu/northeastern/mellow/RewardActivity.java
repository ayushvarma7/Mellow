package edu.northeastern.mellow;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RewardActivity extends AppCompatActivity {

    private TextView tvCoinsEarned, tvRewardMessage, tvRewardEmoji, tvJarEmoji, tvJarStatus, tvStreakBadge;
    private Button btnGoHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        tvCoinsEarned   = findViewById(R.id.tvCoinsEarned);
        tvRewardMessage = findViewById(R.id.tvRewardMessage);
        tvRewardEmoji   = findViewById(R.id.tvRewardEmoji);
        tvJarEmoji      = findViewById(R.id.tvJarEmoji);
        tvJarStatus     = findViewById(R.id.tvJarStatus);
        btnGoHome       = findViewById(R.id.btnGoHome);

        int coins      = getIntent().getIntExtra("coinsEarned", 0);
        boolean jarFull  = getIntent().getBooleanExtra("containerFull", false);
        boolean streakUp = getIntent().getBooleanExtra("streakUpdated", false);
        int newStreak  = getIntent().getIntExtra("newStreak", 0);

        tvCoinsEarned.setText("+" + coins + " Coins Earned! 🪙");

        // Set reward message and emoji
        if (jarFull) {
            tvRewardEmoji.setText("🎉");
            tvRewardMessage.setText("Your jar is full!\nA reward has been unlocked!\nKeep up the amazing work!");
            tvJarEmoji.setText("🎁");
            tvJarStatus.setText("Jar complete! Reward unlocked! 🎊");
        } else if (streakUp && newStreak > 1) {
            tvRewardEmoji.setText("🔥");
            tvRewardMessage.setText("Streak extended!\n" + newStreak + " days and counting.\nYou're on fire!");
            tvJarEmoji.setText("🫙✨");
            tvJarStatus.setText("🔥 " + newStreak + " day streak — incredible!");
        } else if (coins >= 5) {
            tvRewardEmoji.setText("🌟");
            tvRewardMessage.setText("Great session!\nEvery breath counts.\nKeep building momentum!");
            tvJarEmoji.setText("🫙🪙");
            tvJarStatus.setText("Your jar is filling up nicely!");
        } else {
            tvRewardEmoji.setText("🌱");
            tvRewardMessage.setText("Good start!\nSmall steps lead\nto big changes.");
            tvJarEmoji.setText("🫙");
            tvJarStatus.setText("Every check-in fills your jar!");
        }

        setupInitialState();
        playEntranceAnimations(jarFull, streakUp);

        btnGoHome.setOnClickListener(v -> {
            animateButtonPress(v);
            v.postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }, 120);
        });
    }

    private void setupInitialState() {
        tvRewardEmoji.setAlpha(0f);
        tvRewardEmoji.setScaleX(0.4f);
        tvRewardEmoji.setScaleY(0.4f);
        tvRewardEmoji.setTranslationY(-60f);

        tvCoinsEarned.setAlpha(0f);
        tvCoinsEarned.setTranslationY(40f);

        tvRewardMessage.setAlpha(0f);
        tvRewardMessage.setTranslationY(50f);

        btnGoHome.setAlpha(0f);
        btnGoHome.setTranslationY(80f);
    }

    private void playEntranceAnimations(boolean jarFull, boolean streakUp) {
        tvRewardEmoji.animate()
                .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(500).setInterpolator(new OvershootInterpolator()).start();

        tvCoinsEarned.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(180).setDuration(400).start();

        tvRewardMessage.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(320).setDuration(450).start();

        btnGoHome.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay(500).setDuration(400).start();

        if (jarFull || streakUp) pulseEmoji();
    }

    private void pulseEmoji() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvRewardEmoji, View.SCALE_X, 1f, 1.18f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvRewardEmoji, View.SCALE_Y, 1f, 1.18f, 1f);
        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.setStartDelay(750);
        pulse.setDuration(500);
        pulse.start();
    }

    private void animateButtonPress(View view) {
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(60).start())
                .start();
    }
}