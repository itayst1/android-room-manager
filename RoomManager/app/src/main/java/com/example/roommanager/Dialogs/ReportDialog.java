package com.example.roommanager.Dialogs;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.roommanager.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReportDialog extends DialogFragment {

    private EditText reportEditText;
    private ImageView imagePreview;
    private Uri imageUri = null;
    private Bitmap cameraBitmap = null;

    private ActivityResultLauncher<String> requestMediaPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.dialog_report, null);
        dialog.setContentView(view);
        setupDialogWindow(dialog);

        reportEditText = view.findViewById(R.id.reportEditText);
        imagePreview = view.findViewById(R.id.imagePreview);
        Button selectImageButton = view.findViewById(R.id.selectImageButton);
        Button takePhotoButton = view.findViewById(R.id.openCameraButton);
        Button sendReportButton = view.findViewById(R.id.sendReportButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        setupPermissionLaunchers();

        selectImageButton.setOnClickListener(v -> requestImagePermission());
        takePhotoButton.setOnClickListener(v -> requestCameraPermission());
        sendReportButton.setOnClickListener(v -> sendReport());
        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void setupDialogWindow(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void setupPermissionLaunchers() {
        requestMediaPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openImagePicker();
                    else Toast.makeText(getContext(), "Storage permission denied.", Toast.LENGTH_SHORT).show();
                });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imagePreview.setImageURI(imageUri);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                        imagePreview.setImageBitmap(bitmap);
                        cameraBitmap = bitmap;
                    }
                });
    }

    private void requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestMediaPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            requestMediaPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 102); // fallback for camera
        } else {
            openCamera();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void sendReport() {
        String reportText = reportEditText.getText().toString().trim();

        if (reportText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a description.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("message", reportText);
        reportData.put("userEmail", email);
        reportData.put("timestamp", System.currentTimeMillis());

        if (cameraBitmap != null) {
            reportData.put("imageUrl", encodeImageToBase64(cameraBitmap));
        } else if (imageUri != null) {
            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= 29) {
                    bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContext().getContentResolver(), imageUri));
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                }
                reportData.put("imageUrl", encodeImageToBase64(bitmap));
            } catch (IOException e) {
                Toast.makeText(getContext(), "Image processing failed.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        FirebaseDatabase.getInstance().getReference("reports").push().setValue(reportData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Report sent successfully.", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to send report.", Toast.LENGTH_SHORT).show());
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    // Optional fallback if camera permission is requested with onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 102 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(getContext(), "Camera permission denied.", Toast.LENGTH_SHORT).show();
        }
    }
}

