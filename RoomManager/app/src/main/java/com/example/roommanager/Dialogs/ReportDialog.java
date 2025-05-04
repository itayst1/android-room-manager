package com.example.roommanager.Dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    private EditText reportEditText;
    private ImageView imagePreview;
    private Uri imageUri = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report, null);

        reportEditText = view.findViewById(R.id.reportEditText);
        imagePreview = view.findViewById(R.id.imagePreview);
        Button selectImageButton = view.findViewById(R.id.selectImageButton);
        Button sendReportButton = view.findViewById(R.id.sendReportButton);
        Button cancelButton = view.findViewById(R.id.cancelButton);

        selectImageButton.setOnClickListener(v -> openImagePicker());
        sendReportButton.setOnClickListener(v -> sendReport());
        cancelButton.setOnClickListener(v -> dismiss());

        return new Dialog(getActivity(), R.style.CustomDialogTheme) {{
            setContentView(view);
        }};
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imagePreview.setImageURI(imageUri);
        }
    }

    private void sendReport() {
        String reportText = reportEditText.getText().toString().trim();

        if (reportText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a description.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("text", reportText);
        reportData.put("userEmail", email);
        reportData.put("timestamp", System.currentTimeMillis());

        if (imageUri != null) {
            try {
                Bitmap bitmap;
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContext().getContentResolver(), imageUri));
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
                }
                String imageString = encodeImageToBase64(bitmap);
                reportData.put("imageBase64", imageString);
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
}
