package com.example.quizlandmarksbg;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    EditText etUsername, etEmail, etPassword, etRepeatPassword, etAge;
    RadioGroup rgGender;
    Button btnRegister;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db= FirebaseFirestore.getInstance();
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRepeatPassword = findViewById(R.id.etRepeatPassword);
        etAge = findViewById(R.id.etAge);
        rgGender = findViewById(R.id.rgGender);
        btnRegister = findViewById(R.id.btnRegister);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });

        btnRegister.setOnClickListener(v -> registerUser());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void registerWithFirebase(String email, String password, String username, int age, String gender) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser userAuth = mAuth.getCurrentUser();
                        if (userAuth != null) {
                            userAuth.reload().addOnCompleteListener(t -> {
                            String uid = mAuth.getCurrentUser().getUid();
                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);
                            user.put("email", email);
                            user.put("age", age);
                            user.put("gender", gender);

                            db.collection("users")
                                .document(uid)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Успешна регистрация!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Грешка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            });
                        }
                    }
                });
    }
    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String repeatPassword = etRepeatPassword.getText().toString();
        String ageStr = etAge.getText().toString();

        if (username.length() < 2 || username.length() > 7) {
            etUsername.setError("Потребителското име трябва да е между 2 и 7 символа!");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Невалиден email!");
            return;
        }

        if (!password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$")) {
            etPassword.setError("Паролата трябва да е поне 8 символа и да съдържа поне една цифра, " +
                    "една малка буква(a-z), една голяма буква(A-Z) и един от следните символи: @#$%^&+=!");
            return;
        }

        if (!password.equals(repeatPassword)) {
            etRepeatPassword.setError("Паролите не съвпадат!");
            return;
        }

        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Изберeте пол!", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadio = findViewById(selectedId);
        String gender = selectedRadio.getText().toString();

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            etAge.setError("Невалидна възраст! Въведете число.");
            return;
        }
        if (age < 12 || age > 100) {
            etAge.setError("Възрастта трябва да е между 12 и 100 години");
            return;
        }
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        etUsername.setError("Потребителското име вече съществува");
                        return;
                    }
                    registerWithFirebase(email, password, username, age, gender);
                });
    }
}
