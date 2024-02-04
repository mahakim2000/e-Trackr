package com.example.e_trackr.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import com.example.e_trackr.databinding.ActivityFileListBinding;
import com.example.e_trackr.databinding.ActivityOutGoingBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.File;
import com.example.e_trackr.utilities.FileListener;
import com.example.e_trackr.utilities.FilesAdapter2;
import com.example.e_trackr.utilities.FilesAdapter4;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OutGoingActivity extends AppCompatActivity implements FileListener {

    private ActivityOutGoingBinding binding;
    private PreferenceManager preferenceManager;
    private List<File> fileList;
    private FirebaseFirestore firestore;
    private String userId;
    private String userImage;
    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOutGoingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        firestore = FirebaseFirestore.getInstance();
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        setListeners();
        retrieveUserDetails();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.outgoingListActivityRecyclerView.setLayoutManager(layoutManager);
        getFileList();
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
                            Toast.makeText(OutGoingActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(OutGoingActivity.this, "Boolean status and borrower's name updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("infodia", "Boolean status and borrower's name update failed: " + e.getMessage());
                                    Toast.makeText(OutGoingActivity.this, "Boolean status and borrower's name update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // No matching document found
                        Log.d("infodia", "No matching document found");
                        Toast.makeText(OutGoingActivity.this, "No matching document found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("infodia", "Firestore query failed: " + e.getMessage());
                    Toast.makeText(OutGoingActivity.this, "Firestore query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void getFileList() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference userRef = firestore.collection(Constants.KEY_COLLECTION_USERS).document(userId);
        database.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .whereEqualTo(Constants.KEY_OUTGOING, true) // Add this condition
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

                            // Retrieve and format the timestamp
                            Timestamp timestamp = queryDocumentSnapshot.getTimestamp(Constants.KEY_TIMESTAMP);
                            if (timestamp != null) {
                                Date date = timestamp.toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                                file.timeStamp = sdf.format(date);
                            } else {
                                file.timeStamp = "";  // Set default value if timestamp is null
                            }

                            file.outgoing = Boolean.TRUE.equals(queryDocumentSnapshot.getBoolean(Constants.KEY_OUTGOING));
                            file.incoming = Boolean.TRUE.equals(queryDocumentSnapshot.getBoolean(Constants.KEY_INCOMING));
                            file.id = queryDocumentSnapshot.getId();
                            files.add(file);
                        }

                        if (files.size() > 0) {
                            FilesAdapter4 filesAdapter4 = new FilesAdapter4(files, this);
                            binding.outgoingListActivityRecyclerView.setAdapter(filesAdapter4);
                            binding.outgoingListActivityRecyclerView.setVisibility(View.VISIBLE);
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

    @Override
    public void onFileClicked(File file) {
        Intent intent = new Intent(getApplicationContext(), FileDetailsActivity.class);
        intent.putExtra(Constants.KEY_FILE, file);
        startActivity(intent);
        finish();
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