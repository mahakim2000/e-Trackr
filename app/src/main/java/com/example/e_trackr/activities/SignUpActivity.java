package com.example.e_trackr.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.e_trackr.databinding.ActivitySignUpBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firebaseAuth = FirebaseAuth.getInstance();
        setListeners();
    }

    private void setListeners() {
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Handle the sign-up process using Firebase Authentication
    private void signUp() {
        loading(true);

        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(binding.inputName.getText().toString())
                                .build();
                        firebaseUser.updateProfile(profileUpdate)
                                .addOnSuccessListener(aVoid -> {
                                    sendEmailVerificationLink(firebaseUser);
                                    saveUserDetailsToFirestore(firebaseUser);
                                })
                                .addOnFailureListener(e -> {
                                    loading(false);
                                    showToast("Failed to update user profile");
                                });
                    } else {
                        loading(false);
                        String error = "Sign up failed!";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            error = "Email is already registered. Please sign in.";
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            error = "Invalid email format. Please provide a valid email.";
                        } else if (task.getException() instanceof FirebaseAuthException) {
                            FirebaseAuthException exception = (FirebaseAuthException) task.getException();
                            error = exception.getMessage();
                        }
                        showToast(error);
                    }
                });
    }

    // Send the email verification link to the user's email
    private void sendEmailVerificationLink(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnSuccessListener(aVoid -> {
                    loading(false);
                    showToast("Verification link sent to your email. Please verify your email to continue.");
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showToast("Failed to send verification email. Please try again later.");
                });
    }

    // Save the user details to Firestore database after successful sign-up
    private void saveUserDetailsToFirestore(FirebaseUser firebaseUser) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, firebaseUser.getEmail());
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    //preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    //preferenceManager.putString(Constants.KEY_USER_ID, firebaseUser.getUid());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    loading(false);
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    loading(false);
                    showToast("Failed to save user details. Please try again later.");
                });
    }

    // Encode the selected image to Base64 string for storing in Firestore
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Handle the image pick result and update the profile image view
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    // Check if the entered sign-up details are valid
    private Boolean isValidSignUpDetails() {
        String name = binding.inputName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();
        String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();

        if (encodedImage == null) {
            showToast("Select a profile image");
            return false;
        } else if (name.isEmpty()) {
            showToast("Enter your name");
            return false;
        } else if (email.isEmpty()) {
            showToast("Enter your email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Enter a valid email");
            return false;
        } else if (password.isEmpty()) {
            showToast("Enter a password");
            return false;
        } else if (confirmPassword.isEmpty()) {
            showToast("Confirm your password");
            return false;
        } else if (!isPasswordValid(password)) {
            showToast("Password must contain at least 10 characters including 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character");
            return false;
        } else if (!password.equals(confirmPassword)) {
            showToast("Passwords do not match");
            return false;
        } else {
            return true;
        }
    }

    // Check if the password meets the required criteria
    private boolean isPasswordValid(String password) {
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_])[A-Za-z\\d@$!%*?&_]{10,}$";
        return password.matches(passwordPattern);
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
}