package edu.northeastern.mellow;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.repository.AuthRepository;
import edu.northeastern.mellow.data.repository.JournalRepository;
import edu.northeastern.mellow.data.repository.MoodRepository;
import edu.northeastern.mellow.data.util.DateUtils;
import edu.northeastern.mellow.domain.analytics.MoodOverview;
import edu.northeastern.mellow.ui.mood.MoodBubbleView;
import edu.northeastern.mellow.ui.mood.MoodViewModel;
import edu.northeastern.mellow.ui.mood.MoodWaveView;

@AndroidEntryPoint
public class MoodTrendActivity extends AppCompatActivity {

    @Inject MoodRepository moodRepository;
    @Inject JournalRepository journalRepository;
    @Inject AuthRepository authRepository;

    /** Mood scale 1..5 — Depressed, Sad, Neutral, Happy, Overjoyed. */
    private static final String[] MOOD_NAMES = {"Depressed", "Sad", "Neutral", "Happy", "Overjoyed"};
    private static final int[] MOOD_FACE = {
            R.drawable.mood_face_depressed, R.drawable.mood_face_sad, R.drawable.mood_face_neutral,
            R.drawable.mood_face_happy, R.drawable.mood_face_overjoyed};
    private static final int[] MOOD_COLOR = {
            R.color.mood_depressed, R.color.mood_sad, R.color.mood_neutral,
            R.color.mood_happy, R.color.mood_overjoyed};

    private TextView tvMonthYear, tvTodayMood, tvAllMoodsCount, tvOverviewTotal;
    private LinearLayout calendarGrid, calendarSection, listSection;
    private CardView cardMood1, cardMood2, cardMood3, cardMood4, cardMood5;
    private View btnBack, btnPrevMonth, btnNextMonth;
    private TextView btnListView, btnCalendarView, btnOverviewView;
    private View historySection, overviewSection;
    private TextView tabDay, tabWeek, tabMonth, tabYear, tabAll;
    private MoodWaveView waveView;
    private MoodBubbleView bubbleView;

    private MoodViewModel moodViewModel;

    private Calendar currentMonth;
    private final Map<String, MoodEntry> moodMap = new HashMap<>();
    private int selectedMood = -1;
    private String selectedDate;
    /** 0 = list, 1 = calendar, 2 = overview. */
    private int tab = 1;

    private List<MoodEntry> allMoods = new ArrayList<>();
    private List<JournalEntry> allJournals = new ArrayList<>();
    private MoodOverview.Span span = MoodOverview.Span.WEEK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_trend_new);

        tvMonthYear     = findViewById(R.id.tvMonthYear);
        tvTodayMood     = findViewById(R.id.tvTodayMood);
        tvAllMoodsCount = findViewById(R.id.tvAllMoodsCount);
        tvOverviewTotal = findViewById(R.id.tvOverviewTotal);
        calendarGrid    = findViewById(R.id.calendarGrid);
        calendarSection = findViewById(R.id.calendarSection);
        listSection     = findViewById(R.id.listSection);
        btnBack         = findViewById(R.id.btnBack);
        btnPrevMonth    = findViewById(R.id.btnPrevMonth);
        btnNextMonth    = findViewById(R.id.btnNextMonth);
        btnListView     = findViewById(R.id.btnListView);
        btnCalendarView = findViewById(R.id.btnCalendarView);
        btnOverviewView = findViewById(R.id.btnOverviewView);
        historySection  = findViewById(R.id.historySection);
        overviewSection = findViewById(R.id.overviewSection);
        cardMood1 = findViewById(R.id.cardMood1);
        cardMood2 = findViewById(R.id.cardMood2);
        cardMood3 = findViewById(R.id.cardMood3);
        cardMood4 = findViewById(R.id.cardMood4);
        cardMood5 = findViewById(R.id.cardMood5);
        tabDay   = findViewById(R.id.tabDay);
        tabWeek  = findViewById(R.id.tabWeek);
        tabMonth = findViewById(R.id.tabMonth);
        tabYear  = findViewById(R.id.tabYear);
        tabAll   = findViewById(R.id.tabAll);
        waveView   = findViewById(R.id.waveView);
        bubbleView = findViewById(R.id.bubbleView);

        moodViewModel = new ViewModelProvider(this).get(MoodViewModel.class);
        moodViewModel.startObserving();

        currentMonth = Calendar.getInstance();
        selectedDate = DateUtils.today();
        updateLogTargetLabel();
        loadMoodsForMonth();
        loadOverview();

        btnPrevMonth.setOnClickListener(v -> { currentMonth.add(Calendar.MONTH, -1); loadMoodsForMonth(); });
        btnNextMonth.setOnClickListener(v -> { currentMonth.add(Calendar.MONTH, 1); loadMoodsForMonth(); });
        btnBack.setOnClickListener(v -> finish());

        btnListView.setOnClickListener(v -> { tab = 0; applyViewToggle(); });
        btnCalendarView.setOnClickListener(v -> { tab = 1; applyViewToggle(); });
        btnOverviewView.setOnClickListener(v -> { tab = 2; applyViewToggle(); });
        applyViewToggle();

        tabDay.setOnClickListener(v -> selectSpan(MoodOverview.Span.DAY));
        tabWeek.setOnClickListener(v -> selectSpan(MoodOverview.Span.WEEK));
        tabMonth.setOnClickListener(v -> selectSpan(MoodOverview.Span.MONTH));
        tabYear.setOnClickListener(v -> selectSpan(MoodOverview.Span.YEAR));
        tabAll.setOnClickListener(v -> selectSpan(MoodOverview.Span.ALL));
        applySpanTabs();

        CardView[] moodCards = {cardMood1, cardMood2, cardMood3, cardMood4, cardMood5};
        for (int i = 0; i < moodCards.length; i++) {
            final int moodIndex = i;
            final int score = i + 1;
            moodCards[i].setOnClickListener(v -> {
                selectedMood = moodIndex;
                highlightCard(moodCards, moodIndex);
                tvTodayMood.setText("Saving " + MOOD_NAMES[moodIndex].toLowerCase(Locale.US) + "…");
                moodViewModel.logMood(score, selectedDate, null, null);
            });
        }

        moodViewModel.getLogResult().observe(this, result -> {
            if (result == null) return;
            if (result.isError() && result.getMessage() != null
                    && result.getMessage().contains("already logged")) {
                tvTodayMood.setText("A mood is already logged for that day");
                Toast.makeText(this, "One mood per day", Toast.LENGTH_LONG).show();
            } else if (result.isSuccess() && selectedMood >= 0) {
                int coins = selectedMood + 1;
                tvTodayMood.setText("Mood saved! +" + coins + " coin" + (coins == 1 ? "" : "s"));
                selectedDate = DateUtils.today();
                loadMoodsForMonth();
                loadOverview();
                selectedMood = -1;
            }
        });
    }

    // ===================== VIEW TOGGLE =====================

    private void applyViewToggle() {
        boolean overview = tab == 2;
        historySection.setVisibility(overview ? View.GONE : View.VISIBLE);
        overviewSection.setVisibility(overview ? View.VISIBLE : View.GONE);

        calendarSection.setVisibility(tab == 1 ? View.VISIBLE : View.GONE);
        listSection.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);

        styleToggle(btnListView, tab == 0);
        styleToggle(btnCalendarView, tab == 1);
        styleToggle(btnOverviewView, overview);

        if (tab == 0) renderList();
    }

    private void styleToggle(TextView v, boolean active) {
        v.setBackgroundTintList(ColorStateList.valueOf(
                getColor(active ? R.color.mellow_surface : android.R.color.transparent)));
        v.setTextColor(getColor(active ? R.color.mellow_ink : R.color.mellow_ink_2));
    }

    // ===================== CALENDAR =====================

    private void loadMoodsForMonth() {
        String uid = authRepository.getCurrentUid();
        if (uid == null) return;

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthFormat.format(currentMonth.getTime()));

        moodRepository.getMoodHistory(uid, 366, result -> {
            if (result.isSuccess() && result.getData() != null) {
                moodMap.clear();
                SimpleDateFormat ym = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                String target = ym.format(currentMonth.getTime());
                for (MoodEntry m : result.getData()) {
                    if (m.getDate() != null && m.getDate().startsWith(target)) {
                        moodMap.put(m.getDate(), m);
                    }
                }
                runOnUiThread(this::populateCalendarGrid);
            }
        });
    }

    private void populateCalendarGrid() {
        calendarGrid.removeAllViews();
        Calendar cal = (Calendar) currentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int week = 0; week < 6; week++) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setWeightSum(7);

            boolean hasDay = false;
            for (int dow = 1; dow <= 7; dow++) {
                int dayNumber = week * 7 + dow - (firstDayOfWeek - 1);
                if (dayNumber > 0 && dayNumber <= daysInMonth) {
                    row.addView(createDayCell(dayNumber));
                    hasDay = true;
                } else {
                    View empty = new View(this);
                    empty.setLayoutParams(new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.MATCH_PARENT, 1f));
                    row.addView(empty);
                }
            }
            if (hasDay) calendarGrid.addView(row);
        }
    }

    private View createDayCell(int dayNumber) {
        View cell = LayoutInflater.from(this).inflate(R.layout.item_calendar_day, calendarGrid, false);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        ImageView face = cell.findViewById(R.id.ivDayFace);
        TextView num = cell.findViewById(R.id.tvDayNumber);
        View ring = cell.findViewById(R.id.todayRing);
        num.setText(String.valueOf(dayNumber));

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar dayCal = (Calendar) currentMonth.clone();
        dayCal.set(Calendar.DAY_OF_MONTH, dayNumber);
        String key = fmt.format(dayCal.getTime());

        boolean future;
        try { future = LocalDate.parse(key).isAfter(LocalDate.now()); }
        catch (Exception e) { future = false; }

        MoodEntry mood = moodMap.get(key);
        if (mood != null && mood.getMoodScore() >= 1 && mood.getMoodScore() <= 5) {
            face.setImageResource(MOOD_FACE[mood.getMoodScore() - 1]);
            face.setAlpha(1f);
            num.setTextColor(getColor(R.color.mellow_ink));
        } else {
            face.setImageResource(R.drawable.mood_face_neutral);
            face.setAlpha(future ? 0.18f : 0.3f);
            num.setTextColor(getColor(R.color.mellow_ink_3));
        }

        ring.setVisibility(key.equals(selectedDate) ? View.VISIBLE : View.GONE);

        if (!future) {
            cell.setOnClickListener(v -> {
                selectedDate = key;
                updateLogTargetLabel();
                populateCalendarGrid();
            });
        }
        return cell;
    }

    private void updateLogTargetLabel() {
        String today = DateUtils.today();
        if (selectedDate == null || selectedDate.equals(today)) {
            tvTodayMood.setText("Tap a mood to log today");
        } else {
            tvTodayMood.setText("Logging for " + pretty(selectedDate));
        }
    }

    private String pretty(String ymd) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat out = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
            return out.format(in.parse(ymd));
        } catch (Exception e) { return ymd; }
    }

    private void highlightCard(CardView[] cards, int selected) {
        for (int i = 0; i < cards.length; i++) {
            cards[i].setCardBackgroundColor(getColor(
                    i == selected ? R.color.mellow_canvas_2 : R.color.mellow_surface));
        }
    }

    // ===================== LIST VIEW =====================

    private void renderList() {
        listSection.removeAllViews();
        if (allMoods.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No moods logged yet.");
            empty.setTextColor(getColor(R.color.mellow_ink_2));
            empty.setTextSize(13.5f);
            empty.setPadding(4, 12, 4, 12);
            listSection.addView(empty);
            return;
        }

        SimpleDateFormat mon = new SimpleDateFormat("MMM", Locale.US);
        SimpleDateFormat day = new SimpleDateFormat("d", Locale.US);
        SimpleDateFormat time = new SimpleDateFormat("hh:mm a", Locale.US);
        LayoutInflater inf = LayoutInflater.from(this);

        int shown = 0;
        for (MoodEntry m : allMoods) {
            if (shown++ >= 40) break; // keep the page light
            int s = Math.max(1, Math.min(5, m.getMoodScore()));
            View row = inf.inflate(R.layout.item_mood_row, listSection, false);
            java.util.Date d = new java.util.Date(m.getTimestamp());
            ((TextView) row.findViewById(R.id.tvRowMon)).setText(mon.format(d).toUpperCase(Locale.US));
            ((TextView) row.findViewById(R.id.tvRowDay)).setText(day.format(d));
            ((TextView) row.findViewById(R.id.tvRowMood)).setText(MOOD_NAMES[s - 1]);
            ((TextView) row.findViewById(R.id.tvRowTime)).setText(time.format(d));
            ((ImageView) row.findViewById(R.id.ivRowFace)).setImageResource(MOOD_FACE[s - 1]);
            listSection.addView(row);
        }
    }

    // ===================== OVERVIEW =====================

    private void loadOverview() {
        String uid = authRepository.getCurrentUid();
        if (uid == null) return;
        moodRepository.getMoodHistory(uid, 366, mres -> {
            allMoods = (mres.isSuccess() && mres.getData() != null) ? mres.getData() : new ArrayList<>();
            journalRepository.getJournalHistory(uid, 366, jres -> {
                allJournals = (jres.isSuccess() && jres.getData() != null) ? jres.getData() : new ArrayList<>();
                runOnUiThread(this::renderOverview);
            });
        });
    }

    private void renderOverview() {
        int[] counts = MoodOverview.moodCounts(allMoods, allJournals);
        int total = MoodOverview.total(counts);

        tvAllMoodsCount.setText("All Moods (" + allMoods.size() + ")");
        tvOverviewTotal.setText(total + " total mood check-in" + (total == 1 ? "" : "s") + " so far.");

        int[] colors = new int[5];
        for (int i = 0; i < 5; i++) colors[i] = getColor(MOOD_COLOR[i]);

        bubbleView.setData(counts, colors);

        MoodOverview.Series series = MoodOverview.series(allMoods, allJournals, span, LocalDate.now());
        waveView.setData(series.values, series.labels, colors);

        if (tab == 0) renderList();
    }

    private void selectSpan(MoodOverview.Span s) {
        span = s;
        applySpanTabs();
        renderOverview();
    }

    private void applySpanTabs() {
        styleTab(tabDay,   span == MoodOverview.Span.DAY);
        styleTab(tabWeek,  span == MoodOverview.Span.WEEK);
        styleTab(tabMonth, span == MoodOverview.Span.MONTH);
        styleTab(tabYear,  span == MoodOverview.Span.YEAR);
        styleTab(tabAll,   span == MoodOverview.Span.ALL);
    }

    private void styleTab(TextView v, boolean active) {
        v.setBackgroundTintList(ColorStateList.valueOf(
                getColor(active ? R.color.mellow_surface : android.R.color.transparent)));
        v.setTextColor(getColor(active ? R.color.mellow_ink : R.color.mellow_ink_2));
    }
}
