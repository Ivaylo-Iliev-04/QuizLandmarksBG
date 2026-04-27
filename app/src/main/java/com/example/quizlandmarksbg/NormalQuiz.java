package com.example.quizlandmarksbg;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NormalQuiz extends AppCompatActivity {
    LinearLayout questionsContainer;
    Button btnFinish, btnBack;
    FirebaseFirestore db;
    List<QuestionModel> questionList = new ArrayList<>();
    ArrayList<String> reviewList = new ArrayList<>();
    Map<Integer, RadioGroup> answersMap = new HashMap<>();
    int totalPoints = 0;
    int correctAnswers = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_normal_quiz);
        questionsContainer = findViewById(R.id.questionsContainer);
        btnFinish = findViewById(R.id.btnFinish);
        btnBack = findViewById(R.id.btnBack);
        db = FirebaseFirestore.getInstance();
        String city = getIntent().getStringExtra("city");
        loadQuestions(city);

        btnFinish.setOnClickListener(v -> calculatePoints());

        btnBack.setOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void loadQuestions(String city) {
        db.collection("normal_quizzes")
                .document(city)
                .collection("questions")
                .get()
                .addOnSuccessListener(query -> {
                    int index = 0;
                    for (DocumentSnapshot doc : query) {
                        try {
                            QuestionModel q = doc.toObject(QuestionModel.class);
                            if (q != null) {
                                questionList.add(q);
                                addQuestionToUI(q, index);
                                index++;
                            }
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                    }
                });
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

        RadioGroup rg = new RadioGroup(this);
        rg.addView(createRadio(q.option1));
        rg.addView(createRadio(q.option2));
        rg.addView(createRadio(q.option3));
        rg.addView(createRadio(q.option4));
        card.addView(tvQuestion);
        card.addView(rg);
        questionsContainer.addView(card, questionsContainer.getChildCount() - 1);
        answersMap.put(index, rg);
    }
    private RadioButton createRadio(String text) {
        RadioButton rb = new RadioButton(this);
        rb.setText(text);
        rb.setTextColor(Color.BLACK);
        return rb;
    }
    private void calculatePoints() {
        totalPoints = 0;
        correctAnswers = 0;
        reviewList.clear();
        for (int i = 0; i < questionList.size(); i++) {
            try {
                QuestionModel q = questionList.get(i);
                RadioGroup rg = answersMap.get(i);
                int selectedId = rg.getCheckedRadioButtonId();
                String correctAnswer = q.correctAnswer;
                reviewList.add((i + 1) + ". " + q.question + "\n✔ Верен отговор: " + correctAnswer);
                if (selectedId != -1) {
                    RadioButton selected = findViewById(selectedId);
                    if (selected != null && selected.getText().toString().equals(correctAnswer)) {
                        totalPoints += q.points;
                        correctAnswers++;
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        Intent intent = new Intent(this, NormalResultActivity.class);
        intent.putExtra("points", totalPoints);
        intent.putExtra("correct", correctAnswers);
        intent.putStringArrayListExtra("review", reviewList);
        startActivity(intent);
        finish();
    }
}
