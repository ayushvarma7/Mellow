package edu.northeastern.mellow;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MellowApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        configureFirestore();
    }

    private void configureFirestore() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    }
}
