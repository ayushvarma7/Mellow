package edu.northeastern.mellow.widget;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import edu.northeastern.mellow.data.repository.MoodRepository;

/**
 * Lets the (non-Hilt) widget BroadcastReceiver reach the app's singleton
 * MoodRepository, so a widget tap logs through the exact same path as the app
 * (one-per-day dedupe + coin award).
 */
@EntryPoint
@InstallIn(SingletonComponent.class)
public interface MoodWidgetEntryPoint {
    MoodRepository moodRepository();
}
