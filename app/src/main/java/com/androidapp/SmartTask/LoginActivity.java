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
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
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

        if (dbHelper.checkUser(email, password)) {
            saveLogin(email);
            Toast.makeText(this, "Dang nhap thanh cong!", Toast.LENGTH_SHORT).show();
            goToMain();
        } else {
            Toast.makeText(this, "Sai email hoac mat khau!", Toast.LENGTH_SHORT).show();
        }
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
        if (dbHelper.checkEmailExists(email)) {
            Toast.makeText(this, "Email da ton tai!", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = email.split("@")[0];
        boolean success = dbHelper.registerUser(email, password, name);
        if (success) {
            saveLogin(email);
            Toast.makeText(this, "Dang ky thanh cong!", Toast.LENGTH_SHORT).show();
            goToMain();
        } else {
            Toast.makeText(this, "Dang ky that bai!", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLogin(String email) {
        prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("currentUser", email)
                .apply();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}