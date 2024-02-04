package com.example.e_trackr.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.e_trackr.databinding.ActivityChangePasswordBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthWebException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private String userId;
    private FirebaseFirestore firestore;
    private String currentUserEmail;
    private String userName;
    private String userEmail;
    private String userImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        loadUserDetails();

        binding.etUserEmail.setText(currentUserEmail);

        binding.btnChangePassword.setOnClickListener(v -> {
            String email = binding.etUserEmail.getText().toString().trim();
            if (isValidEmail(email)) {
                changePassword(email);
            }
        });

        setListeners();
    }

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

    private void retrieveUserDetails() {
        //showProgress();
        DocumentReference userRef = firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId);
        userRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> userData = document.getData();
                            if (userData != null) {
                                userImage = (String) userData.get(Constants.KEY_IMAGE);
                                userName = (String) userData.get(Constants.KEY_NAME);
                                userEmail = (String) userData.get(Constants.KEY_EMAIL);
                                loadUserDetails();
                                //hideProgress();
                            }
                        }
                    } else {
                        showToast("Failed to retrieve user details. Please try again later.");
                        //hideProgress();
                    }
                });
    }

    private void loadUserDetails() {
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.ivUser.setImageBitmap(bitmap);
    }

    private void setListeners() {
        binding.ivHome.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), HomeActivity.class)));
        binding.tvHome.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), HomeActivity.class)));
        binding.ivFiles.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), FileListActivity.class)));
        binding.tvFiles.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), FileListActivity.class)));
        binding.ivProfile.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
        binding.tvProfile.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
        binding.ivMenu.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), MenuActivity.class)));
        binding.tvMenu.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), MenuActivity.class)));
        binding.ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void signOut() {
        showToast("Signing out");
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();
        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
        finish();
    }

    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class); // Replace "PreviousActivity" with the name of the previous activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void changePassword(String email) {
        binding.progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        showToast("Password reset email sent. Check your inbox.");
                        signOut();
                        //finish();
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