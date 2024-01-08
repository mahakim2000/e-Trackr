package com.example.e_trackr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.e_trackr.databinding.ActivityForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthWebException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseAuth = FirebaseAuth.getInstance();

        binding.buttonResetPassword.setOnClickListener(v -> {
            String email = binding.inputEmail.getText().toString().trim();
            if (isValidEmail(email)) {
                resetPassword(email);
            }
        });
    }

    // Check if the entered email is valid
    private boolean isValidEmail(String email) {
        if (email.isEmpty()) {
            showToast("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Enter valid email");
            return false;
        }
        return true;
    }

    private boolean isPasswordValid(String password) {
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_])[A-Za-z\\d@$!%*?&_]{10,}$";
        return password.matches(passwordPattern);
    }

    // Reset the password using Firebase Authentication's sendPasswordResetEmail method
    private void resetPassword(String email) {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        showToast("Password reset email sent. Check your inbox.");
                        finish();
                    } else {
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            showToast("Invalid email address");
                        } catch (FirebaseAuthRecentLoginRequiredException e) {
                            showToast("Recent login required. Please login again.");
                        } catch (FirebaseAuthUserCollisionException e) {
                            showToast("Email already exists. Choose a different email.");
                        } catch (FirebaseAuthWeakPasswordException e) {
                            showToast("Weak password. Choose a stronger password.");
                        } catch (FirebaseAuthWebException e) {
                            showToast("Web request failed. Check your internet connection.");
                        } catch (Exception e) {
                            showToast("Failed to reset password. Please try again later.");
                        }
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}