package com.example.quizlandmarksbg;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

import java.util.Calendar;

public class LeaderboardActivity extends AppCompatActivity {
    TextView tvYourResult, tvYourPlace;
    LinearLayout leaderboardContainer;
    ScrollView scrollLeaderboard;
    Button btnHome;
    FirebaseFirestore db;
    String username, city;
    int userPoints;
    long userTime;
    View currentUserView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);
        tvYourResult = findViewById(R.id.tvYourResult);
        tvYourPlace = findViewById(R.id.tvYourPlace);
        leaderboardContainer = findViewById(R.id.leaderboardContainer);
        scrollLeaderboard = findViewById(R.id.scrollLeaderboard);
        btnHome = findViewById(R.id.btnHome);

        db = FirebaseFirestore.getInstance();

        username = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        city = getIntent().getStringExtra("city");
        userPoints = getIntent().getIntExtra("points", 0);
        userTime = getIntent().getLongExtra("time", 0);

        tvYourResult.setText("Точки: " + userPoints);

        loadLeaderboard();

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadLeaderboard() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.getWeekYear();
        String currentWeek = year + "_" + week;

        db.collection("leaderboards")
                .document(city)
                .collection("results")
                .whereEqualTo("week", currentWeek)
                .orderBy("points", Query.Direction.DESCENDING)
                .orderBy("time", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int rank = 1;
                    leaderboardContainer.removeAllViews();
                    currentUserView = null;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String uname = doc.getString("username");
                        Long pointsObj = doc.getLong("points");
                        Long timeObj = doc.getLong("time");

                        if (pointsObj == null || timeObj == null) continue;

                        long points = pointsObj;
                        long time = timeObj;

                        if (uname.equals(username) && points == userPoints && time == userTime) {
                            tvYourPlace.setText("Вашето място: " + rank);
                        }

                        addRow(leaderboardContainer, rank, uname, points, time);
                        rank++;
                    }

                    if (currentUserView != null) {
                        scrollLeaderboard.post(() -> {
                            scrollLeaderboard.smoothScrollTo(0, currentUserView.getTop());
                        });
                    }
                });
    }

    private void addRow(LinearLayout container, int rank, String username, long points, long time) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);
        row.setGravity(Gravity.CENTER_VERTICAL);

        if (username.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            row.setBackgroundColor(Color.parseColor("#4D00FF00")); 
            currentUserView = row;
        }

        float density = getResources().getDisplayMetrics().density;

        // 1. Rank Column - 45dp
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

        // 2. User Column - 170dp
        TextView tvUser = new TextView(this);
        tvUser.setLayoutParams(new LinearLayout.LayoutParams((int)(170 * density), -2));
        tvUser.setText(username);
        tvUser.setTextColor(username.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail()) ? Color.parseColor("#BF9B00") : Color.BLACK);
        tvUser.setTextSize(16);
        tvUser.setTypeface(null, Typeface.BOLD);
        tvUser.setPadding(10, 0, 10, 0);
        row.addView(tvUser);

        // 3. Points Column - 65dp
        TextView tvPoints = new TextView(this);
        tvPoints.setLayoutParams(new LinearLayout.LayoutParams((int)(65 * density), -2));
        tvPoints.setText(String.valueOf(points));
        tvPoints.setTextColor(Color.BLACK);
        tvPoints.setGravity(Gravity.CENTER);
        tvPoints.setTextSize(16);
        tvPoints.setTypeface(null, Typeface.BOLD);
        row.addView(tvPoints);

        // 4. Time Column - 65dp
        TextView tvTime = new TextView(this);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams((int)(65 * density), -2));
        tvTime.setText(time / 1000 + "s");
        tvTime.setTextColor(Color.BLACK);
        tvTime.setGravity(Gravity.CENTER);
        tvTime.setTextSize(16);
        tvTime.setTypeface(null, Typeface.BOLD);
        row.addView(tvTime);

        container.addView(row);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#1A000000"));
        container.addView(divider, new LinearLayout.LayoutParams(-1, 1));
    }
}
