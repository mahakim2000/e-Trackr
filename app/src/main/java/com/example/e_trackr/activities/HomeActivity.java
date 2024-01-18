package com.example.e_trackr.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.e_trackr.databinding.ActivityHomeBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.File;
import com.example.e_trackr.utilities.FileListener;
import com.example.e_trackr.utilities.FilesAdapter;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements FileListener {

    private ActivityHomeBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.homeActivityRecyclerView.setLayoutManager(layoutManager);
        getFiles();
    }

    private void getFiles() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<File> files = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            File file = new File();
                            file.fileName = queryDocumentSnapshot.getString(Constants.KEY_FILENAME);
                            file.borrowerName = queryDocumentSnapshot.getString(Constants.KEY_BORROWERNAME);
                            file.timeStamp = queryDocumentSnapshot.getString(Constants.KEY_TIMESTAMP);
                            file.fileStatus = queryDocumentSnapshot.getString(Constants.KEY_FILESTATUS);
                            file.id = queryDocumentSnapshot.getId();
                            files.add(file);
                        }
                        if (files.size() > 0) {
                            FilesAdapter filesAdapter = new FilesAdapter(files, this);
                            binding.homeActivityRecyclerView.setAdapter(filesAdapter);
                            binding.homeActivityRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        Log.e("Firestore", "Error getting documents: ", task.getException());
                        showErrorMessage();
                    }
                });
    }

    private void setListeners() {
        binding.tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), FileListActivity.class)));
        binding.ivHome.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), HomeActivity.class)));
        binding.ivFiles.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), FileListActivity.class)));
        binding.ivProfile.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
        binding.ivMenu.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), MenuActivity.class)));
    }

    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", "There is no recent activity"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public void onFileClicked(File file) {
        Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
        //intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}