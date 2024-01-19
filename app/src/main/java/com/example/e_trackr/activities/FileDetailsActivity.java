package com.example.e_trackr.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.e_trackr.databinding.ActivityFileDetailsBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.File;
import com.example.e_trackr.utilities.FileListener;
import com.example.e_trackr.utilities.FilesAdapter3;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FileDetailsActivity extends AppCompatActivity implements FileListener {

    private ActivityFileDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private File receiveFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        loadUserDetails();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.fileDetailsActivityRecyclerView.setLayoutManager(layoutManager);
        getFiles();
    }

    private void getFiles() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        receiveFile = (File) getIntent().getSerializableExtra(Constants.KEY_FILE);
        database.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<File> files = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            File file = new File();
                            file.fileName = queryDocumentSnapshot.getString(Constants.KEY_FILENAME);
                            file.fileDescription = queryDocumentSnapshot.getString(Constants.KEY_FILEDESCRIPTION);
                            file.borrowerName = queryDocumentSnapshot.getString(Constants.KEY_BORROWERNAME);
                            file.timeStamp = queryDocumentSnapshot.getString(Constants.KEY_TIMESTAMP);
                            file.outgoing = Boolean.TRUE.equals(queryDocumentSnapshot.getBoolean(Constants.KEY_OUTGOING));
                            file.incoming = Boolean.TRUE.equals(queryDocumentSnapshot.getBoolean(Constants.KEY_INCOMING));
                            file.id = queryDocumentSnapshot.getId();
                            files.add(file);
                        }

                        List<File> clickedFileList = new ArrayList<>();
                        for (File file : files) {
                            if (file.id.equals(receiveFile.id)) {
                                clickedFileList.add(file);
                                break;
                            }
                        }

                        if (clickedFileList.size() > 0) {
                            FilesAdapter3 filesAdapter3 = new FilesAdapter3(clickedFileList, this);
                            binding.fileDetailsActivityRecyclerView.setAdapter(filesAdapter3);
                            binding.fileDetailsActivityRecyclerView.setVisibility(View.VISIBLE);
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
        binding.textErrorMessage.setText(String.format("%s", "No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void loadUserDetails() {
        String userName = preferenceManager.getString(Constants.KEY_NAME);
        if (userName != null) {
            binding.tvUserName.setText(userName);
        } else {
            binding.tvUserName.setText("Guest");
        }

        String userEmail = preferenceManager.getString(Constants.KEY_EMAIL);
        if (userEmail != null) {
            binding.tvUserEmail.setText(userEmail);
        } else {
            binding.tvUserEmail.setText("Guest");
        }

        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.ivUser.setImageBitmap(bitmap);
    }

    public void onFileClicked(File file) {
        //Intent intent = new Intent(getApplicationContext(), FileDetailsActivity.class);
        //intent.putExtra(Constants.KEY_FILE, file);
        //startActivity(intent);
        finish();
    }
}