package com.example.e_trackr.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddFileActivity extends AppCompatActivity {

    private ActivityFileAddBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firestore;
    private EditText etFileName, etFileDescription;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
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

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put(Constants.KEY_FILENAME, fileName);
        fileInfo.put(Constants.KEY_FILEDESCRIPTION, fileDescription);

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
}
