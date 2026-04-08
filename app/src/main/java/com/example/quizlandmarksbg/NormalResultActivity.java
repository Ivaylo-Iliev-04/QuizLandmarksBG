package com.example.quizlandmarksbg;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class NormalResultActivity extends AppCompatActivity {
    TextView tvPoints, tvCorrect;
    Button btnBack;
    LinearLayout answersContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_normal_result);
        tvPoints = findViewById(R.id.tvPoints);
        tvCorrect = findViewById(R.id.tvCorrect);
        btnBack = findViewById(R.id.btnBack);
        answersContainer = findViewById(R.id.answersContainer);
        int points = getIntent().getIntExtra("points", 0);
        int correct = getIntent().getIntExtra("correct", 0);
        tvPoints.setText("Точки: " + points);
        tvCorrect.setText("Верни отговори: " + correct);
        ArrayList<String> reviewList = getIntent().getStringArrayListExtra("review");

        if (reviewList != null) {
            for (String item : reviewList) {
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
                tvQuestion.setTypeface(null, Typeface.BOLD);
                tvQuestion.setText(item.split("✔")[0]);
                tvQuestion.setTextSize(16);
                tvQuestion.setTextColor(Color.BLACK);

                TextView tvAnswer = new TextView(this);
                tvAnswer.setText("✔ " + item.split("✔")[1]);
                tvAnswer.setTextSize(16);
                tvAnswer.setTextColor(Color.parseColor("#2E7D32"));

                card.addView(tvQuestion);
                card.addView(tvAnswer);
                answersContainer.addView(card);
            }
        }

        btnBack.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}