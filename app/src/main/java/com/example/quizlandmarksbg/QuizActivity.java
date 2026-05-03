package com.example.quizlandmarksbg;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizActivity extends AppCompatActivity {
    LinearLayout questionsContainer;
    Button btnFinish, btnBack;
    FirebaseFirestore db;
    List<QuestionModel> questionList = new ArrayList<>();
    Map<Integer, RadioGroup> answersMap = new HashMap<>();
    int totalPoints = 0;
    TextView tvTimer;
    CountDownTimer timer;
    long timeLeft = 90000;// 90 секунди
    long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);
        questionsContainer = findViewById(R.id.questionsContainer);
        btnFinish = findViewById(R.id.btnFinish);
        btnBack = findViewById(R.id.btnBack);
        tvTimer = findViewById(R.id.tvTimer);
        startTime = System.currentTimeMillis();
        startTimer();
        db = FirebaseFirestore.getInstance();
        String city = getIntent().getStringExtra("city");
        loadQuestions(city);
        btnFinish.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            calculatePoints();
        });
        btnBack.setOnClickListener(v -> {
            if (timer != null) timer.cancel();
            finish();
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void startTimer() {
        timer=new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                tvTimer.setText(String.valueOf(seconds));
            }
            @Override
            public void onFinish() {
                tvTimer.setText("0");
                calculatePoints();
            }
        };
        timer.start();
    }
    private void loadQuestions(String city) {
        db.collection("quizzes")
                .document(city)
                .collection("questions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int index = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        QuestionModel q = doc.toObject(QuestionModel.class);
                        if (q != null) {
                            questionList.add(q);
                            addQuestionToUI(q, index);
                            index++;
                        }
                    }
                });
    }
    private RadioButton createStyledRadio(String text) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        rb.setTextColor(Color.BLACK);
        rb.setTextSize(16);
        return rb;
    }
    private void addQuestionToUI(QuestionModel q, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        card.setBackgroundColor(Color.parseColor("#CCFFFFFF"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);

        TextView tvQuestion = new TextView(this);
        tvQuestion.setText((index + 1) + ". " + q.question);
        tvQuestion.setTextColor(Color.BLACK);
        tvQuestion.setTextSize(18);
        tvQuestion.setPadding(0, 0, 0, 10);

        RadioGroup rg = new RadioGroup(this);
        RadioButton rb1 = createStyledRadio(q.option1);
        RadioButton rb2 = createStyledRadio(q.option2);
        RadioButton rb3 = createStyledRadio(q.option3);
        RadioButton rb4 = createStyledRadio(q.option4);

        rg.addView(rb1);
        rg.addView(rb2);
        rg.addView(rb3);
        rg.addView(rb4);

        card.addView(tvQuestion);
        card.addView(rg);

        questionsContainer.addView(card, questionsContainer.getChildCount() - 1);
        answersMap.put(index, rg);
    }
    private void calculatePoints() {
        totalPoints = 0;
        for (int i = 0; i < questionList.size(); i++) {
            QuestionModel q = questionList.get(i);
            RadioGroup rg = answersMap.get(i);
            if (rg == null) continue;
            int selectedId = rg.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selected = findViewById(selectedId);
                if (selected.getText().toString().equals(q.correctAnswer)) {
                    totalPoints += q.points;
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long timeTaken = endTime - startTime;
        String city = getIntent().getStringExtra("city");
        saveResult(city, totalPoints, timeTaken);
        Toast.makeText(this, "Вашите точки: " + totalPoints, Toast.LENGTH_LONG).show();
    }
    private void saveResult(String city, int points, long timeTaken) {
        String username = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("points", points);
        result.put("time", timeTaken);
        result.put("timestamp", System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);

        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.getWeekYear();
        String weekId = year + "_" + week;
        result.put("week", weekId);

        db.collection("leaderboards")
                .document(city)
                .collection("results")
                .add(result)
                .addOnSuccessListener(documentReference -> {
                    Intent intent = new Intent(this, LeaderboardActivity.class);
                    intent.putExtra("username", username);
                    intent.putExtra("city", city);
                    intent.putExtra("points", points);
                    intent.putExtra("time", timeTaken);
                    startActivity(intent);
                    finish();
                });
    }
}
