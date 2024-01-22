package com.example.e_trackr.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
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
        try {
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

            String fileData = fileName + "\n" + fileDescription;
            Bitmap qrCode = generateQRCode(fileData);

            if (qrCode != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                qrCode.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] qrCodeByteArray = baos.toByteArray();
                String qrCodeBase64 = Base64.encodeToString(qrCodeByteArray, Base64.DEFAULT);
                fileInfo.put(Constants.KEY_QRCODE, qrCodeBase64);

                firestore.collection(Constants.KEY_COLLECTION_FILE_INFO)
                        .add(fileInfo)
                        .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                progressBar.setVisibility(View.INVISIBLE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(AddFileActivity.this, "File info uploaded successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Log.e("Firestore", "Error uploading file info", task.getException());
                                    Toast.makeText(AddFileActivity.this, "Error uploading file info", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                // Handle the case where generating QR code fails
                progressBar.setVisibility(View.INVISIBLE);
                Log.e("QRCode", "Error generating QR code");
                Toast.makeText(AddFileActivity.this, "Error generating QR code", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Handle other exceptions
            e.printStackTrace();
            progressBar.setVisibility(View.INVISIBLE);
            Log.e("Upload", "Error uploading file info", e);
            Toast.makeText(AddFileActivity.this, "An error occurred", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap generateQRCode(String data) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    data,
                    BarcodeFormat.QR_CODE,
                    300,  // width and height of the QR code
                    300
            );

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
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
