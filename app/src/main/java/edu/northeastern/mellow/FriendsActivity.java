package edu.northeastern.mellow;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import edu.northeastern.mellow.ui.buddy.BuddyActivity;

/**
 * Redirects to BuddyActivity — the full buddy system with
 * requests, connections, and nudge feature.
 */
public class FriendsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, BuddyActivity.class));
        finish();
    }
}
