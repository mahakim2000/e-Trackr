package com.example.e_trackr.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

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

        binding.btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFileInfo();
            }
        });
        initViews();
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if(isGranted) {
                    showCamera();
                }
                else {

                }
            });

    private ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
        } else {
            showOptionsDialog(result.getContents());
        }
    });

    private void setResult(String contents) {
        binding.textResult.setText(contents);
    }

    private void showCamera() {
        ScanOptions scanOptions = new ScanOptions();
        scanOptions.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        scanOptions.setPrompt("Scan QR Code");
        scanOptions.setCameraId(0);
        scanOptions.setBeepEnabled(false);
        scanOptions.setBarcodeImageEnabled(true);
        scanOptions.setOrientationLocked(false);

        qrCodeLauncher.launch(scanOptions);
    }

    private void initViews() {
        binding.fabScan.setOnClickListener(view -> {
            checkPermissionAndShowActivity(this);
        });
    }

    private void checkPermissionAndShowActivity(Context context) {
        if(ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            showCamera();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void showOptionsDialog(String qrCodeContents) {
        Log.d("infodia", "Scanned QR Code Contents: " + qrCodeContents);

        // Use regular expression to split on newline characters
        String[] qrCodeParts = qrCodeContents.split("\\r?\\n");

        if (qrCodeParts.length >= 2) {
            String fileName = qrCodeParts[0].trim();
            String fileDescription = qrCodeParts[1].trim();

            Constants.KEY_FILENAME = fileName;
            Constants.KEY_FILEDESCRIPTION = fileDescription;

            Log.d("infodia", "Scanned QR Code - FileName: " + Constants.KEY_FILENAME + ", FileDescription: " + Constants.KEY_FILEDESCRIPTION);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Option")
                    .setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                            new String[]{"Incoming", "Outgoing"}), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selectedOption = (which == 0) ? "Incoming" : "Outgoing";
                            setResult(qrCodeContents);
                            if (selectedOption.equals("Outgoing")) {
                                // If Outgoing is selected, show another dialog to input a name
                                showNameInputDialog();
                            } else {
                                // If Incoming is selected, update the chosen option immediately
                                updateChosenOption(fileName, fileDescription, selectedOption, null); // Pass null for the borrower's name
                            }
                            dialog.dismiss();
                        }
                    })
                    .show();
        } else {
            Toast.makeText(this, "Invalid QR Code contents", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNameInputDialog() {
        // Set up the input field in the dialog
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter Borrower's Name");

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("File Information")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get the entered borrower's name
                        String borrowerName = input.getText().toString().trim();

                        // Perform any validation on the entered name if needed
                        if (!borrowerName.isEmpty()) {
                            // Update the chosen option with "Outgoing" and store borrower's name
                            updateChosenOption(Constants.KEY_FILENAME, Constants.KEY_FILEDESCRIPTION, "Outgoing", borrowerName);
                        } else {
                            Toast.makeText(AddFileActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User canceled the input
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void updateChosenOption(String fileName, String fileDescription, String selectedOption, String borrowerName) {
        Log.d("infodia", "Updating option for \nFileName:" + fileName + "\nFileDescription:" + fileDescription);

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .whereEqualTo("fileName", fileName)
                .whereEqualTo("fileDescription", fileDescription)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Matching document found
                        Log.d("infodia", "Matching document found");
                        DocumentReference documentReference = queryDocumentSnapshots.getDocuments().get(0).getReference();

                        // Update the boolean status and store borrower's name
                        boolean isIncoming = selectedOption.equals("Incoming");
                        boolean isOutgoing = selectedOption.equals("Outgoing");

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put(Constants.KEY_INCOMING, isIncoming);
                        updateData.put(Constants.KEY_OUTGOING, isOutgoing);
                        updateData.put(Constants.KEY_BORROWERNAME, borrowerName);
                        updateData.put(Constants.KEY_TIMESTAMP, FieldValue.serverTimestamp());

                        documentReference.update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("infodia", "Boolean status and borrower's name updated successfully");
                                    Toast.makeText(AddFileActivity.this, "Boolean status and borrower's name updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("infodia", "Boolean status and borrower's name update failed: " + e.getMessage());
                                    Toast.makeText(AddFileActivity.this, "Boolean status and borrower's name update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // No matching document found
                        Log.d("infodia", "No matching document found");
                        Toast.makeText(AddFileActivity.this, "No matching document found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("infodia", "Firestore query failed: " + e.getMessage());
                    Toast.makeText(AddFileActivity.this, "Firestore query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        finish();
        startActivity(intent);
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
    }
}
