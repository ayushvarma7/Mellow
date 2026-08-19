package edu.northeastern.mellow;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.util.DateUtils;
import edu.northeastern.mellow.ui.journal.JournalViewModel;

@AndroidEntryPoint
public class JournalHistoryActivity extends AppCompatActivity {

    private static final String TAG = "JournalHistoryActivity";

    // Mood score (1..5) -> drawn face + soft chip colour
    private static final int[] FACE_RES = {
            R.drawable.mood_face_depressed, R.drawable.mood_face_sad, R.drawable.mood_face_neutral,
            R.drawable.mood_face_happy, R.drawable.mood_face_overjoyed};
    private static final int[] FACE_SOFT = {
            R.color.mellow_coral_soft, R.color.mellow_honey_soft, R.color.mellow_canvas_2,
            R.color.mellow_sky_soft, R.color.mellow_sage_soft};

    private RecyclerView rvJournals;
    private View emptyState;
    private ImageButton btnBack;
    private FloatingActionButton fabAddJournal;
    private TextView tvJEntries, tvJStreak;
    private ImageView ivJMood;

    private JournalViewModel journalViewModel;
    private JournalAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_history);

        rvJournals    = findViewById(R.id.rvJournals);
        emptyState    = findViewById(R.id.emptyState);
        btnBack       = findViewById(R.id.btnBack);
        fabAddJournal = findViewById(R.id.fabAddJournal);
        tvJEntries    = findViewById(R.id.tvJEntries);
        tvJStreak     = findViewById(R.id.tvJStreak);
        ivJMood       = findViewById(R.id.ivJMood);

        journalViewModel = new ViewModelProvider(this).get(JournalViewModel.class);
        journalViewModel.startObserving();

        adapter = new JournalAdapter();
        rvJournals.setLayoutManager(new LinearLayoutManager(this));
        rvJournals.setAdapter(adapter);

        btnBack.setOnClickListener(v -> onBackPressed());
        fabAddJournal.setOnClickListener(v -> openJournalDialog());
        findViewById(R.id.btnStartWriting).setOnClickListener(v -> openJournalDialog());
        findViewById(R.id.btnEmptyWrite).setOnClickListener(v -> openJournalDialog());

        journalViewModel.getRecentJournals().observe(this, result -> {
            if (result.isSuccess() && result.getData() != null) {
                List<JournalEntry> journals = result.getData();

                if (journals.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvJournals.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvJournals.setVisibility(View.VISIBLE);
                    adapter.setJournals(journals);
                }
                renderInsightStrip(journals);
            } else if (result.isError()) {
                Log.e(TAG, "Error loading journals: " + result.getMessage());
            }
        });
    }

    // --- Insight strip ---

    private void renderInsightStrip(List<JournalEntry> journals) {
        tvJEntries.setText(String.valueOf(journals.size()));
        tvJStreak.setText(String.valueOf(journalingStreak(journals)));
        applyMoodFace(ivJMood, dominantMood(journals));
    }

    private int journalingStreak(List<JournalEntry> journals) {
        Set<LocalDate> days = new HashSet<>();
        for (JournalEntry j : journals) {
            if (j.getDate() == null) continue;
            try { days.add(LocalDate.parse(j.getDate())); } catch (Exception ignored) {}
        }
        if (days.isEmpty()) return 0;
        LocalDate d = LocalDate.now();
        if (!days.contains(d)) d = d.minusDays(1); // allow a streak that ended yesterday
        int streak = 0;
        while (days.contains(d)) { streak++; d = d.minusDays(1); }
        return streak;
    }

    private int dominantMood(List<JournalEntry> journals) {
        int[] counts = new int[6]; // index 1..5
        for (JournalEntry j : journals) {
            int s = j.getMoodScore();
            if (s >= 1 && s <= 5) counts[s]++;
        }
        int best = 3, bestCount = -1;
        for (int s = 1; s <= 5; s++) {
            if (counts[s] > bestCount) { bestCount = counts[s]; best = s; }
        }
        return best;
    }

    static void applyMoodFace(ImageView iv, int score) {
        int s = Math.max(1, Math.min(5, score));
        iv.setImageResource(FACE_RES[s - 1]);
    }

    // --- New entry dialog ---

    private void openJournalDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_journal_entry);
        dialog.setCancelable(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etTitle = dialog.findViewById(R.id.etJournalTitle);
        EditText etContent = dialog.findViewById(R.id.etJournalContent);

        CardView cardMood1 = dialog.findViewById(R.id.cardMood1);
        CardView cardMood2 = dialog.findViewById(R.id.cardMood2);
        CardView cardMood3 = dialog.findViewById(R.id.cardMood3);
        CardView cardMood4 = dialog.findViewById(R.id.cardMood4);
        CardView cardMood5 = dialog.findViewById(R.id.cardMood5);

        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        // Date picker — defaults to today, never allows the future
        final String[] pickedDate = {DateUtils.today()};
        TextView btnPickDate = dialog.findViewById(R.id.btnPickDate);
        if (btnPickDate != null) {
            btnPickDate.setText("Today");
            btnPickDate.setOnClickListener(v -> showJournalDatePicker(pickedDate, btnPickDate));
        }

        CardView[] moodCards = {cardMood1, cardMood2, cardMood3, cardMood4, cardMood5};
        final int[] selectedMood = {3};

        View.OnClickListener moodClickListener = v -> {
            for (CardView card : moodCards) {
                card.setCardBackgroundColor(getColor(R.color.mellow_canvas_2));
            }
            ((CardView) v).setCardBackgroundColor(getColor(R.color.mellow_honey));
            if (v.getId() == R.id.cardMood1) selectedMood[0] = 1;
            else if (v.getId() == R.id.cardMood2) selectedMood[0] = 2;
            else if (v.getId() == R.id.cardMood3) selectedMood[0] = 3;
            else if (v.getId() == R.id.cardMood4) selectedMood[0] = 4;
            else if (v.getId() == R.id.cardMood5) selectedMood[0] = 5;
        };

        cardMood1.setOnClickListener(moodClickListener);
        cardMood2.setOnClickListener(moodClickListener);
        cardMood3.setOnClickListener(moodClickListener);
        cardMood4.setOnClickListener(moodClickListener);
        cardMood5.setOnClickListener(moodClickListener);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (content.isEmpty()) {
                etContent.setError("Write something small");
                return;
            }
            btnSave.setEnabled(false);
            btnSave.setText("Saving…");
            journalViewModel.saveJournal(title.isEmpty() ? null : title, content, selectedMood[0], pickedDate[0]);
            journalViewModel.getIsSaving().observe(this, isSaving -> {
                if (Boolean.FALSE.equals(isSaving)) dialog.dismiss();
            });
        });

        dialog.show();
    }

    /** Date picker for a journal entry — capped at today (no future entries). */
    private void showJournalDatePicker(String[] holder, TextView label) {
        java.time.LocalDate cur = java.time.LocalDate.parse(holder[0]);
        DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> {
            java.time.LocalDate sel = java.time.LocalDate.of(y, m + 1, d);
            holder[0] = sel.toString(); // ISO yyyy-MM-dd
            label.setText(sel.isEqual(java.time.LocalDate.now())
                    ? "Today"
                    : sel.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")));
        }, cur.getYear(), cur.getMonthValue() - 1, cur.getDayOfMonth());
        dp.getDatePicker().setMaxDate(System.currentTimeMillis());
        dp.show();
    }

    // --- Adapter ---

    private class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.JournalViewHolder> {

        private List<JournalEntry> journals = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.US);

        void setJournals(List<JournalEntry> journals) {
            this.journals = journals;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_journal_entry, parent, false);
            return new JournalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
            JournalEntry journal = journals.get(position);
            holder.tvDate.setText(dateFormat.format(new Date(journal.getTimestamp())).toUpperCase(Locale.US));
            applyMoodFace(holder.ivMood, journal.getMoodScore());

            if (journal.getTitle() != null && !journal.getTitle().isEmpty()) {
                holder.tvTitle.setVisibility(View.VISIBLE);
                holder.tvTitle.setText(journal.getTitle());
            } else {
                holder.tvTitle.setVisibility(View.GONE);
            }
            holder.tvContent.setText(journal.getContent());
            holder.tvTime.setText(timeFormat.format(new Date(journal.getTimestamp())));
        }

        @Override
        public int getItemCount() {
            return journals.size();
        }

        class JournalViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvTitle, tvContent, tvTime;
            ImageView ivMood;

            JournalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvJournalDate);
                ivMood = itemView.findViewById(R.id.ivJournalMood);
                tvTitle = itemView.findViewById(R.id.tvJournalTitle);
                tvContent = itemView.findViewById(R.id.tvJournalContent);
                tvTime = itemView.findViewById(R.id.tvJournalTime);
            }
        }
    }
}
