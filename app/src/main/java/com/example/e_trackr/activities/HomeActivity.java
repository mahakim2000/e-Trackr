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

import com.example.e_trackr.databinding.ActivityHomeBinding;
import com.example.e_trackr.utilities.Constants;
import com.example.e_trackr.utilities.File;
import com.example.e_trackr.utilities.FileListener;
import com.example.e_trackr.utilities.FilesAdapter;
import com.example.e_trackr.utilities.PreferenceManager;
import com.google.firebase.FirebaseApp;
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

public class HomeActivity extends AppCompatActivity implements FileListener {

    private ActivityHomeBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firestore;
    private String userId;
    private String userImage;
    private boolean isPermissionGranted = false;
    private String fileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        fileId = preferenceManager.getString(Constants.KEY_FILE);
        firestore = FirebaseFirestore.getInstance();
        setListeners();
        loadUserDetails();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.homeActivityRecyclerView.setLayoutManager(layoutManager);
        getFiles();
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
                            Toast.makeText(HomeActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(HomeActivity.this, "Boolean status and borrower's name updated successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("infodia", "Boolean status and borrower's name update failed: " + e.getMessage());
                                    Toast.makeText(HomeActivity.this, "Boolean status and borrower's name update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        // No matching document found
                        Log.d("infodia", "No matching document found");
                        Toast.makeText(HomeActivity.this, "No matching document found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("infodia", "Firestore query failed: " + e.getMessage());
                    Toast.makeText(HomeActivity.this, "Firestore query failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user details when the activity resumes
        loadUserDetails();
    }

    private void getFiles() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_FILE_INFO)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<File> outgoingFiles = new ArrayList<>();
                        List<File> incomingFiles = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            File file = new File();
                            file.fileName = queryDocumentSnapshot.getString(Constants.KEY_FILENAME);
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

                            if (file.outgoing) {
                                outgoingFiles.add(file);
                            } else if (file.incoming) {
                                incomingFiles.add(file);
                            }
                        }

                        // Combine outgoing and incoming files
                        List<File> files = new ArrayList<>();
                        files.addAll(outgoingFiles);
                        files.addAll(incomingFiles);

                        int outgoingCount = outgoingFiles.size();
                        int incomingCount = incomingFiles.size();
                        int totalFilesCount = files.size();

                        if (files.size() > 0) {
                            FilesAdapter filesAdapter = new FilesAdapter(files, this);
                            binding.homeActivityRecyclerView.setAdapter(filesAdapter);
                            binding.homeActivityRecyclerView.setVisibility(View.VISIBLE);

                            binding.tvOutgoing.setText(String.valueOf(outgoingCount));
                            binding.tvIncoming.setText(String.valueOf(incomingCount));
                            binding.tvRemainingFiles.setText(String.valueOf(incomingCount));
                            binding.tvTotalFiles.setText(String.valueOf(totalFilesCount));
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
        binding.fabScan.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ScanQRActivity.class)));
        binding.tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), FileListActivity.class)));
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
        binding.outgoingInfo.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), OutGoingActivity.class)));
        binding.incomingInfo.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), InComingActivity.class)));
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
        //Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
        //intent.putExtra(Constants.KEY_USER, user);
        //startActivity(intent);
        //finish();
    }

    private void loadUserDetails() {
        String userName = preferenceManager.getString(Constants.KEY_NAME);
        if (userName != null) {
            binding.tvUserName.setText("Hi, " + userName);
        } else {
            binding.tvUserName.setText("Hi, Guest");
        }

        String userImage = preferenceManager.getString(Constants.KEY_IMAGE);
        if (userImage != null) {
            byte[] bytes = Base64.decode(userImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.ivUser.setImageBitmap(bitmap);
        } else {
            showToast("ERROR BABI");
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signOut() {
        showToast("Signing out");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }
}