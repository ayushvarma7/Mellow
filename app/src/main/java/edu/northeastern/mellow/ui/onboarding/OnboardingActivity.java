package edu.northeastern.mellow.ui.onboarding;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.R;

/**
 * Onboarding flow: welcome → name → age → what makes you happy → goals.
 * The top chrome (back, progress, skip) is shared and lives in the activity.
 */
@AndroidEntryPoint
public class OnboardingActivity extends AppCompatActivity {

    private static final int PAGE_COUNT = 5;

    private ViewPager2 viewPager;
    private View progress;
    private View btnBack;
    private View btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.view_pager);
        progress  = findViewById(R.id.onboardProgress);
        btnBack   = findViewById(R.id.btnOnboardBack);
        btnSkip   = findViewById(R.id.btnOnboardSkip);

        viewPager.setAdapter(new OnboardingPagerAdapter(this));
        viewPager.setUserInputEnabled(false);
        viewPager.setOffscreenPageLimit(1);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { updateChrome(position); }
        });

        btnBack.setOnClickListener(v -> previousPage());
        // Skip jumps to goals, the one step we need before we can finish.
        btnSkip.setOnClickListener(v -> viewPager.setCurrentItem(PAGE_COUNT - 1, true));

        updateChrome(0);
    }

    public void nextPage() {
        int next = viewPager.getCurrentItem() + 1;
        if (next < PAGE_COUNT) viewPager.setCurrentItem(next, true);
    }

    public void previousPage() {
        int prev = viewPager.getCurrentItem() - 1;
        if (prev >= 0) viewPager.setCurrentItem(prev, true);
        else finish();
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() > 0) previousPage();
        else super.onBackPressed();
    }

    private void updateChrome(int position) {
        // welcome has its own full-bleed hero, so hide the chrome there
        boolean showChrome = position > 0;
        btnBack.setVisibility(showChrome ? View.VISIBLE : View.INVISIBLE);
        btnSkip.setVisibility(showChrome && position < PAGE_COUNT - 1 ? View.VISIBLE : View.INVISIBLE);

        View track = (View) progress.getParent();
        track.post(() -> {
            int full = track.getWidth();
            float fraction = (position + 1) / (float) PAGE_COUNT;
            ViewGroup.LayoutParams lp = progress.getLayoutParams();
            lp.width = Math.round(full * fraction);
            progress.setLayoutParams(lp);
        });
    }

    private static class OnboardingPagerAdapter extends FragmentStateAdapter {

        private final List<Fragment> fragments = new ArrayList<>();

        OnboardingPagerAdapter(FragmentActivity activity) {
            super(activity);
            fragments.add(new WelcomeFragment());
            fragments.add(new NameFragment());
            fragments.add(new AgeFragment());
            fragments.add(new HappyThingsFragment());
            fragments.add(new GoalsFragment());
        }

        @Override public Fragment createFragment(int position) { return fragments.get(position); }
        @Override public int getItemCount() { return fragments.size(); }
    }
}
