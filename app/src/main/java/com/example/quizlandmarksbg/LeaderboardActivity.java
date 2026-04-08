package com.example.quizlandmarksbg;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
        TextView tv = new TextView(this);
        tv.setText(rank + ". " + username + " - " + points + " т. (" + time / 1000 + "s)");
        tv.setTextSize(16);
        tv.setPadding(10, 10, 10, 10);

        if (username.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            tv.setTextColor(Color.YELLOW);
            tv.setBackgroundColor(Color.parseColor("#3300FF00"));
            currentUserView = tv;
        } else {
            tv.setTextColor(Color.BLACK);
        }

        container.addView(tv);
    }
}
