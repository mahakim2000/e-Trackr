package com.example.e_trackr.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.e_trackr.databinding.ActivityScanQrBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

public class ScanQRActivity extends AppCompatActivity {

    private ActivityScanQrBinding binding;
    private boolean isPermissionGranted = false;
    private PreferenceManager preferenceManager;
    private String fileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanQrBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        fileId = preferenceManager.getString(Constants.KEY_FILE);
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
                            updateChosenOption(fileName, fileDescription, selectedOption);
                            dialog.dismiss();
                        }
                    })
                    .show();
        } else {
            Toast.makeText(this, "Invalid QR Code contents", Toast.LENGTH_SHORT).show();
        }
    }


    private void updateChosenOption(String fileName, String fileDescription, String selectedOption) {
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

                        // Update the boolean status based on the selected option
                        boolean isIncoming = selectedOption.equals("Incoming");
                        boolean isOutgoing = selectedOption.equals("Outgoing");

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put(Constants.KEY_INCOMING, isIncoming);
                        updateData.put(Constants.KEY_OUTGOING, isOutgoing);

                        documentReference.update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("infodia", "Boolean status updated successfully");
                                    Toast.makeText(ScanQRActivity.this, "Boolean status updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("infodia", "Boolean status update failed: " + e.getMessage());
                                    Toast.makeText(ScanQRActivity.this, "Boolean status update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // No matching document found
                        Log.d("infodia", "No matching document found");
                        Toast.makeText(ScanQRActivity.this, "No matching document found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("infodia", "Firestore query failed: " + e.getMessage());
                    Toast.makeText(ScanQRActivity.this, "Firestore query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}