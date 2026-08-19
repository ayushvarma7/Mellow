package edu.northeastern.mellow.di;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.BuddyRepository;
import edu.northeastern.mellow.data.repository.JournalRepository;
import edu.northeastern.mellow.data.repository.MoodRepository;
import edu.northeastern.mellow.data.repository.ProgressRepository;
import edu.northeastern.mellow.data.repository.impl.AuthRepositoryImpl;
import edu.northeastern.mellow.data.repository.impl.BuddyRepositoryImpl;
import edu.northeastern.mellow.data.repository.impl.JournalRepositoryImpl;
import edu.northeastern.mellow.data.repository.impl.MoodRepositoryImpl;
import edu.northeastern.mellow.data.repository.impl.ProgressRepositoryImpl;

@Module
@InstallIn(SingletonComponent.class)
public abstract class AuthModule {

    @Binds
    @Singleton
    public abstract AuthRepository bindAuthRepository(AuthRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract ProgressRepository bindProgressRepository(ProgressRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract MoodRepository bindMoodRepository(MoodRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract BuddyRepository bindBuddyRepository(BuddyRepositoryImpl impl);

    @Binds
    @Singleton
    public abstract JournalRepository bindJournalRepository(JournalRepositoryImpl impl);

    @Provides
    @Singleton
    public static FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public static FirebaseFirestore provideFirebaseFirestore() {
        return FirebaseFirestore.getInstance();
    }

}
