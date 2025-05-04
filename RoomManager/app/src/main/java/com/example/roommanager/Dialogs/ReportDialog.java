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

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int STORAGE_PERMISSION_REQUEST = 101;
    private static final int CAMERA_PERMISSION_REQUEST = 102;


    private EditText reportEditText;
    private ImageView imagePreview;
    private Uri imageUri = null;

    private Bitmap cameraBitmap = null;

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

        selectImageButton.setOnClickListener(v -> checkPermissionsAndOpenImagePicker());
        takePhotoButton.setOnClickListener(v -> checkPermissionsAndOpenCamera());
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

    private void checkPermissionsAndOpenImagePicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Only check permission below API 33
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST);
                return;
            }
        }
        openImagePicker();  // Safe to call directly in API 33+
    }

    private void checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST && data.getData() != null) {
                imageUri = data.getData();
                imagePreview.setImageURI(imageUri); // <-- shows gallery image
            } else if (requestCode == CAMERA_REQUEST && data.getExtras() != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                imagePreview.setImageBitmap(bitmap); // <-- shows camera image
                cameraBitmap = bitmap;
            }
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap, "Captured Image", null);
        return Uri.parse(path);
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
            String imageString = encodeImageToBase64(cameraBitmap);
            reportData.put("imageUrl", imageString);
        } else if (imageUri != null) {
            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= 29) {
                    bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContext().getContentResolver(), imageUri));
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                }
                String imageString = encodeImageToBase64(bitmap);
                reportData.put("imageUrl", imageString);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == STORAGE_PERMISSION_REQUEST) {
                openImagePicker();
            } else if (requestCode == CAMERA_PERMISSION_REQUEST) {
                openCamera();
            }
        } else {
            Toast.makeText(getContext(), "Permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

}
