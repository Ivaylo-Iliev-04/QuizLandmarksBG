package com.example.quizlandmarksbg;

import android.graphics.Color;
import android.os.Bundle;
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
        TextView tv = new TextView(this);
        tv.setText(rank + ". " + username + " - " + points + " т. (" + time/1000 + "s)");
        tv.setTextSize(16);
        tv.setPadding(10, 10, 10, 10);
        
        if (username.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            tv.setTextColor(Color.YELLOW);
            tv.setBackgroundColor(Color.parseColor("#3300FF00"));
            if (isGlobal) {
                currentUserViewGlobal = tv;
            } else {
                currentUserViewWeekly = tv;
            }
        } else {
            tv.setTextColor(Color.BLACK);
        }
        container.addView(tv);
    }
}