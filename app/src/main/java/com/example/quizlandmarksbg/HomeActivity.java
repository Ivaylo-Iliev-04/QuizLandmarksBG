package com.example.quizlandmarksbg;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    TextView tvWelcome;
    ImageView btnLogout, btnInfo;
    Button btnCompetition, btnNormalGame, btnLeaderboard;
    Spinner spinnerCities;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        /*db.collection("leaderboards")
                .document("Stara Zagora")
                .collection("results")
                .get()
                .addOnSuccessListener(query -> {

                    for (DocumentSnapshot doc : query) {

                        Map<String, Object> data = doc.getData();

                        db.collection("leaderboards")
                                .document("Стара Загора")
                                .collection("results")
                                .add(data);
                    }

                    Toast.makeText(this, "Копирано!", Toast.LENGTH_LONG).show();
                });*/

        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        btnInfo = findViewById(R.id.btnInfo);
        btnCompetition = findViewById(R.id.btnCompetition);
        btnNormalGame = findViewById(R.id.btnNormalGame);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        spinnerCities = findViewById(R.id.spinnerCities);
        String username = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();//!

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username1 = doc.getString("username");
                        tvWelcome.setText("Здравейте, " + username1);
                    }
                });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnInfo.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });
        List<String> cityList = new ArrayList<>();
        db.collection("cities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        cityList.add(doc.getId());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cityList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCities.setAdapter(adapter);
                });
        btnCompetition.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Местоположение")
                    .setMessage("Искате ли да използвате вашето местоположение?")
                    .setPositiveButton("Да", (dialog, which) -> {
                        Intent intent = new Intent(this, LocationActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    })
                    .setNegativeButton("Не", null)
                    .show();
        });
        btnNormalGame.setOnClickListener(v -> {
            String selectedCity = spinnerCities.getSelectedItem().toString();
            Intent intent = new Intent(this, NormalQuiz.class);
            intent.putExtra("city", selectedCity);
            startActivity(intent);
        });
        btnLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(this, GlobalLeaderboardActivity.class));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}