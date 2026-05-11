package com.androidapp.SmartTask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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

        // Check nếu đã login
        if (prefs.getBoolean("isLoggedIn", false)) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Vui long nhap email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui long nhap mat khau");
            return;
        }

        // Check trong database
        if (dbHelper.checkUser(email, password)) {
            prefs.edit().putBoolean("isLoggedIn", true).apply();
            prefs.edit().putString("currentUser", email).apply();
            Toast.makeText(this, "Dang nhap thanh cong!", Toast.LENGTH_SHORT).show();
            goToMain();
        } else {
            Toast.makeText(this, "Sai email hoac mat khau!", Toast.LENGTH_SHORT).show();
        }
    }

    private void register() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Vui long nhap email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui long nhap mat khau");
            return;
        }

        if (password.length() < 4) {
            etPassword.setError("Mat khau it nhat 4 ky tu");
            return;
        }

        // Check email đã tồn tại chưa
        if (dbHelper.checkEmailExists(email)) {
            Toast.makeText(this, "Email da duoc dang ky!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Đăng ký
        String name = email.split("@")[0]; // Lấy phần trước @ làm tên
        if (dbHelper.registerUser(email, password, name)) {
            prefs.edit().putBoolean("isLoggedIn", true).apply();
            prefs.edit().putString("currentUser", email).apply();
            Toast.makeText(this, "Dang ky thanh cong!", Toast.LENGTH_SHORT).show();
            goToMain();
        } else {
            Toast.makeText(this, "Dang ky that bai!", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}