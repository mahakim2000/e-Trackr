package com.example.e_trackr.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.e_trackr.R;
import com.example.e_trackr.databinding.ActivityFileAddBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddFileActivity extends AppCompatActivity {

    private ActivityFileAddBinding binding;
    private PreferenceManager preferenceManager;
    private EditText etFileName, etFileDescription;
    private ProgressBar progressBar;
    private FirebaseFirestore firestore;
    private String userId;
    private String userImage;
    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        setListeners();
        retrieveUserDetails();
        etFileName = findViewById(R.id.etFileName);
        etFileDescription = findViewById(R.id.etFileDescription);
        progressBar = findViewById(R.id.progressBar);

        binding.buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFileInfo();
            }
        });
    }

    private void uploadFileInfo() {
        String fileName = etFileName.getText().toString().trim();
        String fileDescription = etFileDescription.getText().toString().trim();
        String borrowerName = null;
        String timeStamp = null;
        boolean outgoing = false;
        boolean incoming = true;
        String fileStatus = null;

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put(Constants.KEY_FILENAME, fileName);
        fileInfo.put(Constants.KEY_FILEDESCRIPTION, fileDescription);
        fileInfo.put(Constants.KEY_BORROWERNAME, borrowerName);
        fileInfo.put(Constants.KEY_TIMESTAMP, timeStamp);
        fileInfo.put(Constants.KEY_OUTGOING, outgoing);
        fileInfo.put(Constants.KEY_INCOMING, incoming);

        firestore.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .add(fileInfo)
                .addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (task.isSuccessful()) {
                            Toast.makeText(AddFileActivity.this, "File info uploaded successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddFileActivity.this, "Error uploading file info", Toast.LENGTH_SHORT).show();
                        }
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
