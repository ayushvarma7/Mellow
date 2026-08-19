package edu.northeastern.mellow.ui.buddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.R;
import edu.northeastern.mellow.data.model.BuddyGroup;
import edu.northeastern.mellow.data.model.BuddyRequest;
import edu.northeastern.mellow.data.util.VibrationHelper;

import javax.inject.Inject;

import edu.northeastern.mellow.data.repository.AuthRepository;

@AndroidEntryPoint
public class BuddyActivity extends AppCompatActivity {

    @Inject
    AuthRepository authRepository;

    private BuddyViewModel viewModel;

    private TextInputEditText etBuddyUsername;
    private MaterialButton btnSendRequest;
    private TextView tvSendResult;
    private LinearLayout llRequests, llBuddies;
    private TextView tvNoRequests;
    private View emptyCircle, circleExtras;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buddy);

        etBuddyUsername = findViewById(R.id.et_buddy_username);
        btnSendRequest  = findViewById(R.id.btn_send_request);
        tvSendResult    = findViewById(R.id.tv_send_result);
        llRequests      = findViewById(R.id.ll_requests);
        llBuddies       = findViewById(R.id.ll_buddies);
        tvNoRequests    = findViewById(R.id.tv_no_requests);
        emptyCircle     = findViewById(R.id.empty_circle);
        circleExtras    = findViewById(R.id.circle_extras);

        viewModel = new ViewModelProvider(this).get(BuddyViewModel.class);
        viewModel.startObserving();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnSendRequest.setOnClickListener(v -> {
            String username = etBuddyUsername.getText() != null
                    ? etBuddyUsername.getText().toString().trim() : "";
            if (username.isEmpty()) return;
            viewModel.sendRequest(username);
        });

        viewModel.getIsSending().observe(this, sending ->
                btnSendRequest.setEnabled(!sending));

        viewModel.getSendRequestResult().observe(this, result -> {
            if (result.isSuccess()) {
                tvSendResult.setText("Request sent!");
                tvSendResult.setTextColor(ContextCompat.getColor(this, R.color.mellow_accent_gold));
                etBuddyUsername.setText("");
            } else if (result.isError()) {
                tvSendResult.setText(result.getMessage());
                tvSendResult.setTextColor(ContextCompat.getColor(this, R.color.mellow_error));
            }
        });

        viewModel.getIncomingRequests().observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                renderRequests(result.getData());
            }
        });

        viewModel.getBuddyGroups().observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                renderBuddies(result.getData());
            }
        });

        // Vibrate + toast when a nudge arrives
        viewModel.getIncomingNudgeFrom().observe(this, senderUsername -> {
            if (senderUsername == null) return;
            VibrationHelper.nudge(this);
            Toast.makeText(this, "\uD83D\uDC4B @" + senderUsername + " nudged you!", Toast.LENGTH_SHORT).show();
        });
    }

    private void renderRequests(List<BuddyRequest> requests) {
        // Remove all except the "no requests" placeholder
        for (int i = llRequests.getChildCount() - 1; i >= 0; i--) {
            if (llRequests.getChildAt(i) != tvNoRequests) {
                llRequests.removeViewAt(i);
            }
        }

        if (requests.isEmpty()) {
            tvNoRequests.setVisibility(View.VISIBLE);
            return;
        }

        tvNoRequests.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (BuddyRequest request : requests) {
            View item = inflater.inflate(R.layout.item_buddy_request, llRequests, false);

            TextView tvFrom = item.findViewById(R.id.tv_request_from);
            tvFrom.setText("@" + request.getFromUsername() + " wants to be your buddy");

            item.findViewById(R.id.btn_accept).setOnClickListener(v -> {
                viewModel.acceptRequest(request.getId());
            });
            item.findViewById(R.id.btn_decline).setOnClickListener(v -> {
                viewModel.declineRequest(request.getId());
            });

            llRequests.addView(item);
        }
    }

    private void renderBuddies(List<BuddyGroup> groups) {
        String currentUid = authRepository.getCurrentUid();

        llBuddies.removeAllViews();

        if (groups.isEmpty()) {
            emptyCircle.setVisibility(View.VISIBLE);
            circleExtras.setVisibility(View.GONE);
            llBuddies.setVisibility(View.GONE);
            return;
        }

        emptyCircle.setVisibility(View.GONE);
        circleExtras.setVisibility(View.VISIBLE);
        llBuddies.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (BuddyGroup group : groups) {
            View item = inflater.inflate(R.layout.item_buddy_group, llBuddies, false);

            String buddyUsername = group.getBuddyUsername(currentUid);

            TextView tvInitial  = item.findViewById(R.id.tv_buddy_initial);
            TextView tvUsername = item.findViewById(R.id.tv_buddy_username);

            tvInitial.setText(buddyUsername.isEmpty() ? "?"
                    : String.valueOf(buddyUsername.charAt(0)).toUpperCase());
            tvUsername.setText("@" + buddyUsername);

            View btnNudge = item.findViewById(R.id.btn_nudge);
            String buddyUid = group.getBuddyUid(currentUid);

            // Light buzz on finger-down; actual send only on long-press
            btnNudge.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    VibrationHelper.onPress(this);
                }
                return false; // let long-click handler fire too
            });
            btnNudge.setOnLongClickListener(v -> {
                VibrationHelper.onSent(this);
                viewModel.sendNudge(group.getId(), buddyUid);
                Toast.makeText(this, "\uD83D\uDC4B Nudged @" + buddyUsername + "!", Toast.LENGTH_SHORT).show();
                return true;
            });

            item.findViewById(R.id.btn_remove_buddy).setOnClickListener(v ->
                    viewModel.removeBuddy(group.getId()));

            llBuddies.addView(item);
        }
    }
}
