package com.example.quizlandmarksbg;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalLeaderboardActivity extends AppCompatActivity {
    Spinner spinnerCities;
    Button btnSearch, btnBack;
    LinearLayout globalContainer;
    LinearLayout weeklyContainer;
    View currentUserViewGlobal = null;
    View currentUserViewWeekly = null;
    ScrollView scrollGlobal, scrollWeekly;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_global_leaderboard);
        spinnerCities = findViewById(R.id.spinnerCities);
        btnSearch = findViewById(R.id.btnSearch);
        btnBack = findViewById(R.id.btnBack);
        globalContainer = findViewById(R.id.globalContainer);
        weeklyContainer = findViewById(R.id.weeklyContainer);
        scrollGlobal = findViewById(R.id.scrollGlobal);
        scrollWeekly = findViewById(R.id.scrollWeekly);
        db = FirebaseFirestore.getInstance();
        loadCities();
        loadGlobalLeaderboard();

        btnSearch.setOnClickListener(v -> {
            String city = spinnerCities.getSelectedItem().toString();
            loadWeeklyLeaderboard(city);
        });

        btnBack.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void loadCities() {
        List<String> cityList = new ArrayList<>();
        db.collection("leaderboards")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        cityList.add(doc.getId());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            cityList
                    );
                    spinnerCities.setAdapter(adapter);
                });
    }
    private void loadGlobalLeaderboard() {
        globalContainer.removeAllViews();
        currentUserViewGlobal = null;
        Map<String, Integer> pointsMap = new HashMap<>();
        Map<String, Long> timeMap = new HashMap<>();
        db.collection("leaderboards")
                .get()
                .addOnSuccessListener(cities -> {
                    int totalCities = cities.size();
                    final int[] processedCities = {0};
                    for (DocumentSnapshot cityDoc : cities) {
                        db.collection("leaderboards")
                                .document(cityDoc.getId())
                                .collection("results")
                                .get()
                                .addOnSuccessListener(results -> {
                                    for (DocumentSnapshot doc : results) {
                                        String user=doc.getString("username");
                                        Long points=doc.getLong("points");
                                        Long time=doc.getLong("time");
                                        if (user == null || points == null || time == null) continue;
                                        pointsMap.put(user, pointsMap.getOrDefault(user, 0) + points.intValue());

                                        timeMap.put(user, timeMap.getOrDefault(user, 0L) + time);
                                    }
                                    processedCities[0]++;

                                    //когато всички градове са обработени
                                    if (processedCities[0] == totalCities) {
                                        showGlobalSorted(pointsMap, timeMap);
                                    }
                                });
                    }
                });
    }
    private void showGlobalSorted(Map<String, Integer> pointsMap, Map<String, Long> timeMap) {
        List<String> users = new ArrayList<>(pointsMap.keySet());
        Collections.sort(users, (u1, u2) -> {
            int p1 = pointsMap.get(u1);
            int p2 = pointsMap.get(u2);
            if (p2 != p1) return p2 - p1;
            long t1 = timeMap.get(u1);
            long t2 = timeMap.get(u2);
            return Long.compare(t1, t2);
        });
        int rank = 1;
        for (String user : users) {
            addRow(globalContainer, rank, user, pointsMap.get(user), timeMap.get(user), true);
            rank++;
        }
        if (currentUserViewGlobal != null) {
            scrollGlobal.post(() -> {
                scrollGlobal.smoothScrollTo(0, currentUserViewGlobal.getTop());
            });
        }
    }
    private void loadWeeklyLeaderboard(String city) {
        weeklyContainer.removeAllViews();
        currentUserViewWeekly = null;
        
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.getWeekYear();
        String weekId = year + "_" + week;

        db.collection("leaderboards")
                .document(city)
                .collection("results")
                .whereEqualTo("week", weekId)
                .orderBy("points", Query.Direction.DESCENDING)
                .orderBy("time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        TextView tv = new TextView(this);
                        tv.setText("Няма данни за този град");
                        tv.setTextColor(Color.BLACK);
                        weeklyContainer.addView(tv);
                        return;
                    }
                    int rank = 1;
                    for (DocumentSnapshot doc : query) {
                        String user = doc.getString("username");
                        Long points = doc.getLong("points");
                        Long time = doc.getLong("time");
                        
                        if (user != null && points != null && time != null) {
                            addRow(weeklyContainer, rank, user, points, time, false);
                            rank++;
                        }
                    }
                    
                    if (currentUserViewWeekly != null) {
                        scrollWeekly.post(() -> {
                            scrollWeekly.smoothScrollTo(0, currentUserViewWeekly.getTop());
                        });
                    }
                });
    }

    private void addRow(LinearLayout container, int rank, String username, long points, long time, boolean isGlobal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Акцентиране на текущия потребител
        if (username.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            row.setBackgroundColor(Color.parseColor("#4D00FF00")); // Светло зелено
            if (isGlobal) currentUserViewGlobal = row; else currentUserViewWeekly = row;
        }

        float density = getResources().getDisplayMetrics().density;

        // 1. Колона за Ранг (с медали)
        TextView tvRank = new TextView(this);
        tvRank.setLayoutParams(new LinearLayout.LayoutParams((int)(45 * density), -2));
        tvRank.setGravity(Gravity.CENTER);
        if (rank == 1) tvRank.setText("🥇");
        else if (rank == 2) tvRank.setText("🥈");
        else if (rank == 3) tvRank.setText("🥉");
        else tvRank.setText(rank + ".");
        tvRank.setTextColor(Color.BLACK);
        tvRank.setTextSize(16);
        tvRank.setTypeface(null, Typeface.BOLD);
        row.addView(tvRank);

        // 2. Колона за Потребител
        TextView tvUser = new TextView(this);
        tvUser.setLayoutParams(new LinearLayout.LayoutParams((int)(170 * density), -2));
        tvUser.setText(username);
        tvUser.setTextColor(username.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()) ? Color.parseColor("#BF9B00") : Color.BLACK);
        tvUser.setTextSize(16);
        tvUser.setTypeface(null, Typeface.BOLD);
        tvUser.setPadding(10, 0, 10, 0);
        row.addView(tvUser);

        // 3. Колона за Точки
        TextView tvPoints = new TextView(this);
        tvPoints.setLayoutParams(new LinearLayout.LayoutParams((int)(65 * density), -2));
        tvPoints.setText(String.valueOf(points));
        tvPoints.setTextColor(Color.BLACK);
        tvPoints.setGravity(Gravity.CENTER);
        tvPoints.setTextSize(16);
        tvPoints.setTypeface(null, Typeface.BOLD);
        row.addView(tvPoints);

        // 4. Колона за Време
        TextView tvTime = new TextView(this);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams((int)(65 * density), -2));
        tvTime.setText(time / 1000 + "s");
        tvTime.setTextColor(Color.BLACK);
        tvTime.setGravity(Gravity.CENTER);
        tvTime.setTextSize(16);
        tvTime.setTypeface(null, Typeface.BOLD);
        row.addView(tvTime);

        container.addView(row);

        // Тънка разделителна линия
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#1A000000"));
        container.addView(divider, new LinearLayout.LayoutParams(-1, 1));
    }
}
