package com.androidapp.SmartTask;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {

    private static final String TAG = "FirebaseAuth";
    private FirebaseAuth mAuth;

    public FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password, AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Registered: " + user.getEmail());
                        callback.onSuccess(user);
                    } else {
                        Log.e(TAG, "Register failed", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d(TAG, "Logged in: " + user.getEmail());
                        callback.onSuccess(user);
                    } else {
                        Log.e(TAG, "Login failed", task.getException());
                        callback.onFailure(task.getException().getMessage());
                    }
                });
    }

    public void logout() {
        mAuth.signOut();
    }

    public boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String error);
    }
}