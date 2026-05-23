package com.androidapp.SmartTask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView btnLogin, tvRegister;
    private DatabaseHelper dbHelper;
    private FirebaseAuthManager firebaseAuth;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        firebaseAuth = new FirebaseAuthManager();
        prefs = getSharedPreferences("SmartTask", MODE_PRIVATE);

        if (prefs.getBoolean("isLoggedIn", false)) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> login());
        tvRegister.setOnClickListener(v -> register());
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui long nhap email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui long nhap mat khau");
            return;
        }

        // Login local first
        if (dbHelper.checkUser(email, password)) {
            saveLoginAndGo(email);
            return;
        }

        // Login Firebase
        firebaseAuth.loginUser(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                String name = email.split("@")[0];
                dbHelper.registerUser(email, password, name);
                saveLoginAndGo(email);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LoginActivity.this, "Sai email hoac mat khau!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void register() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui long nhap email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui long nhap mat khau");
            return;
        }
        if (password.length() < 4) {
            etPassword.setError("Mat khau it nhat 4 ky tu");
            return;
        }

        firebaseAuth.registerUser(email, password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                String name = email.split("@")[0];
                dbHelper.registerUser(email, password, name);
                saveLoginAndGo(email);
                Toast.makeText(LoginActivity.this, "Dang ky thanh cong!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(LoginActivity.this, "Loi: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveLoginAndGo(String email) {
        prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("currentUser", email)
                .apply();
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}