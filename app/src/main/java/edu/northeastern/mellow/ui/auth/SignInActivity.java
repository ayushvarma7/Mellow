package edu.northeastern.mellow.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.MainActivity;
import edu.northeastern.mellow.R;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.ui.onboarding.OnboardingActivity;

@AndroidEntryPoint
public class SignInActivity extends AppCompatActivity {

    @Inject
    AuthRepository authRepository;

    private AuthViewModel authViewModel;
    private GoogleSignInClient googleSignInClient;

    private MaterialButton btnGoogleSignIn;
    private MaterialButton btnGuestSignIn;
    private ProgressBar progressBar;
    private TextView tvError;

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() != null) {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                            handleGoogleSignInResult(task);
                        } else {
                            showError(getString(R.string.sign_in_error));
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        btnGuestSignIn  = findViewById(R.id.btn_guest_sign_in);
        progressBar     = findViewById(R.id.progress_bar);
        tvError         = findViewById(R.id.tv_error);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupGoogleSignIn();
        observeViewModel();

        if (authRepository.isSignedIn()) {
            // Already signed in — check onboarding status and route.
            // Show spinner while the Firestore check runs.
            btnGoogleSignIn.setVisibility(View.GONE);
            btnGuestSignIn.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            authViewModel.checkOnboardingStatus();
            return;
        }

        btnGoogleSignIn.setOnClickListener(v -> launchGoogleSignIn());
        btnGuestSignIn.setOnClickListener(v -> {
            hideError();
            authViewModel.signInAsGuest();
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void launchGoogleSignIn() {
        hideError();
        signInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            authViewModel.signInWithGoogle(credential);
        } catch (ApiException e) {
            showError("Google sign-in failed (code " + e.getStatusCode() + ")");
        }
    }

    private void observeViewModel() {
        authViewModel.getIsLoading().observe(this, isLoading -> {
            btnGoogleSignIn.setEnabled(!isLoading);
            btnGuestSignIn.setEnabled(!isLoading);
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // After sign-in completes, check onboarding status before routing.
        authViewModel.getSignInResult().observe(this, result -> {
            if (result.isSuccess()) {
                authViewModel.checkOnboardingStatus();
            } else if (result.isError()) {
                showError(result.getMessage() != null
                        ? result.getMessage()
                        : getString(R.string.sign_in_error));
            }
        });

        authViewModel.getDestination().observe(this, dest -> {
            if (dest == AuthViewModel.Destination.ONBOARDING) {
                navigateToOnboarding();
            } else if (dest == AuthViewModel.Destination.MAIN) {
                navigateToMain();
            }
        });
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void navigateToOnboarding() {
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
}
