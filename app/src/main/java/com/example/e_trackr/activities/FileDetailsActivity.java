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

import com.bumptech.glide.Glide;
import com.example.e_trackr.databinding.ActivityFileDetailsBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.File;
import com.example.e_trackr.utilities.FileListener;
import com.example.e_trackr.utilities.FilesAdapter3;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileDetailsActivity extends AppCompatActivity implements FileListener {

    private ActivityFileDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private File receiveFile;
    private FirebaseFirestore firestore;
    private String userId;
    private String userImage;
    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        setListeners();
        retrieveUserDetails();
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


                            Bitmap qrCode = retrieveQRCodeFromFirebase(clickedFileList.get(0));

                            if (qrCode != null) {
                                binding.ivQRCode.setImageBitmap(qrCode);
                            } else {
                                // Handle failure to retrieve or display QR code
                            }

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

    private Bitmap retrieveQRCodeFromFirebase(File file) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .document(file.id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String qrCodeBase64 = document.getString(Constants.KEY_QRCODE);

                            if (qrCodeBase64 != null) {
                                // Decode the Base64 string to a byte array
                                byte[] qrCodeByteArray = Base64.decode(qrCodeBase64, Base64.DEFAULT);

                                // Convert the byte array to a Bitmap
                                Bitmap qrCodeBitmap = BitmapFactory.decodeByteArray(qrCodeByteArray, 0, qrCodeByteArray.length);

                                // Display the QR code using Glide
                                Glide.with(this)
                                        .load(qrCodeBitmap)
                                        .into(binding.ivQRCode);
                            }
                        }
                    } else {
                        Log.e("Firestore", "Error getting QR code: ", task.getException());
                    }
                });
        return null;
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

    public void onFileClicked(File file) {
        //Intent intent = new Intent(getApplicationContext(), FileDetailsActivity.class);
        //intent.putExtra(Constants.KEY_FILE, file);
        //startActivity(intent);
        //finish();
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
        binding.tvUserName.setText(userName);
        binding.tvUserEmail.setText(userEmail);
    }
}