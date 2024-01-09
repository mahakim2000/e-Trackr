package com.example.e_trackr.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.example.e_trackr.databinding.ActivityMenuBinding;
import com.example.e_trackr.databinding.ActivityProfileBinding;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MenuActivity extends AppCompatActivity {

    private ActivityMenuBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        init();
        setListeners();
    }

    private void init() {
        database = FirebaseFirestore.getInstance();
    }

    private void setListeners() {
        //binding.buttonSignOut.setOnClickListener(v -> signOut());
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signOut() {
        showToast("Signing out");
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signOut();
        startActivity(new Intent(getApplicationContext(), SignInActivity.class));
        finish();
    }
}