package com.example.quizlandmarksbg;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {
    TextView tvUsername, tvEmail, tvAge, tvGender;
    LinearLayout resultsContainer;
    Button btnBack;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    String currentEmail;
    String currentUid;

    // Помощен клас за сортиране
    private static class GameResult {
        String city, week;
        long points, time;
        int rank, yearNum, weekNum;

        GameResult(String city, long points, long time, String week) {
            this.city = city; this.points = points; this.time = time; this.week = week;
            try {
                String[] parts = week.split("_");
                yearNum = Integer.parseInt(parts[0]);
                weekNum = Integer.parseInt(parts[1]);
            } catch (Exception e) { yearNum = weekNum = 0; }
        }
    }

    private List<GameResult> resultsList = new ArrayList<>();
    private int activeTasks = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvAge = findViewById(R.id.tvAge);
        tvGender = findViewById(R.id.tvGender);
        resultsContainer = findViewById(R.id.resultsContainer);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() != null) {
            currentEmail = mAuth.getCurrentUser().getEmail();
            currentUid = mAuth.getCurrentUser().getUid();
            loadUserData();
            loadAllResults();
        } else { finish(); }

        btnBack.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadUserData() {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvUsername.setText("Потребител: " + doc.getString("username"));
                tvEmail.setText("Email: " + doc.getString("email"));
                tvAge.setText("Възраст: " + doc.getLong("age"));
                tvGender.setText("Пол: " + doc.getString("gender"));
            }
        });
    }

    private void loadAllResults() {
        resultsList.clear();
        db.collection("leaderboards").get().addOnSuccessListener(cityDocs -> {
            for (DocumentSnapshot cityDoc : cityDocs) {
                String cityName = cityDoc.getId();
                db.collection("leaderboards").document(cityName).collection("results")
                        .whereEqualTo("username", currentEmail).get().addOnSuccessListener(results -> {
                    for (DocumentSnapshot doc : results) {
                        fetchRankAndStore(new GameResult(cityName, doc.getLong("points"), doc.getLong("time"), doc.getString("week")));
                    }
                });
            }
        });
    }

    private void fetchRankAndStore(GameResult gr) {
        activeTasks++;
        db.collection("leaderboards").document(gr.city).collection("results")
                .whereEqualTo("week", gr.week).orderBy("points", Query.Direction.DESCENDING)
                .orderBy("time", Query.Direction.ASCENDING).get().addOnSuccessListener(query -> {
            int rank = 1;
            for (DocumentSnapshot doc : query) {
                if (doc.getString("username").equals(currentEmail) && doc.getLong("points") == gr.points && doc.getLong("time") == gr.time) break;
                rank++;
            }
            gr.rank = rank; resultsList.add(gr); activeTasks--;
            if (activeTasks == 0) displayResults();
        });
    }

    private void displayResults() {
        Collections.sort(resultsList, (r1, r2) -> {
            if (r1.yearNum != r2.yearNum) return r2.yearNum - r1.yearNum;
            if (r1.weekNum != r2.weekNum) return r2.weekNum - r1.weekNum;
            return r1.city.compareTo(r2.city);
        });
        resultsContainer.removeAllViews();
        for (GameResult gr : resultsList) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#F2FFFFFF")); // 95% непрозрачност
            card.setPadding(35, 35, 35, 35);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.setMargins(0, 0, 0, 30); card.setLayoutParams(p);

            TextView title = new TextView(this);
            title.setText("📍 " + gr.city); title.setTextSize(20);
            title.setTypeface(null, Typeface.BOLD); title.setTextColor(Color.BLACK);
            card.addView(title);

            View div = new View(this); div.setBackgroundColor(Color.LTGRAY);
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, 2);
            dp.setMargins(0, 10, 0, 10); card.addView(div, dp);

            TextView det = new TextView(this);
            det.setText("🏆 Точки: " + gr.points + "\n⏱️ Време: " + (gr.time / 1000) + " сек.\n📅 " + gr.yearNum + "г., седм. " + gr.weekNum + "\n🏅 Място: #" + gr.rank);
            det.setTextSize(16); det.setTextColor(Color.parseColor("#212121"));
            card.addView(det);
            resultsContainer.addView(card);
        }
    }
}
