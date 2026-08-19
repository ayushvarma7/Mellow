package edu.northeastern.mellow;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.ui.auth.SignInActivity;
import edu.northeastern.mellow.ui.onboarding.OnboardingActivity;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Inject
    AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!authRepository.isSignedIn()) {
                // Not signed in → go to sign-in
                startActivity(new Intent(this, SignInActivity.class));
                finish();
            } else {
                // Signed in — check if onboarding is complete
                String uid = authRepository.getCurrentUid();
                authRepository.checkOnboardingStatus(uid, result -> runOnUiThread(() -> {
                    Intent intent;
                    if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                        // First time — needs onboarding
                        intent = new Intent(this, OnboardingActivity.class);
                    } else {
                        // Returning user — go home
                        intent = new Intent(this, MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }));
            }
        }, 2000);
    }
}
