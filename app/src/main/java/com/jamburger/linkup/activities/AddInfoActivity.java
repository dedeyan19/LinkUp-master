package com.jamburger.linkup.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jamburger.linkup.R;
import com.jamburger.linkup.fragments.DetailsFragment;
import com.jamburger.linkup.fragments.ProfileImageFragment;
import com.jamburger.linkup.fragments.UsernameFragment;

import java.util.HashMap;

public class AddInfoActivity extends AppCompatActivity {

    public ImageView nextButton;
    public HashMap<String, Object> data;
    public Uri profileImageUri = null;

    public TextView headerText;
    Fragments current;
    DocumentReference userReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_info);
        userReference = FirebaseFirestore.getInstance().collection("Users").document(FirebaseAuth.getInstance().getUid());
        nextButton = findViewById(R.id.btn_next);
        headerText = findViewById(R.id.txt_header);

        data = new HashMap<>();
        current = Fragments.USERNAME;
        getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, new UsernameFragment(this)).commit();
        nextButton.setOnClickListener(view -> {
            nextFragment();
        });
    }

    public void nextFragment() {
        if (current == Fragments.USERNAME) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, new ProfileImageFragment(this)).commit();
            current = Fragments.PROFILE_IMAGE;
        } else if (current == Fragments.PROFILE_IMAGE) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, new DetailsFragment(this)).commit();
            current = Fragments.DETAILS;
        } else if (current == Fragments.DETAILS) {
            if (profileImageUri != null)
                updateDataWithImage();
            else
                updateData();
        }
    }

    void updateData() {
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(data.get("name").toString())
                .setPhotoUri(profileImageUri)
                .build();
        FirebaseAuth.getInstance().getCurrentUser().updateProfile(request);

        userReference.update(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    void updateDataWithImage() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Saving Profile...");
        progressDialog.show();

        String postId = userReference.getId();
        StorageReference ref = storageReference.child("Profile Pictures/" + postId);

        ref.putFile(profileImageUri).addOnCompleteListener(task0 -> {
            if (task0.isSuccessful()) {
                storageReference.child("Profile Pictures/").child(postId).getDownloadUrl().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        data.put("profileImageUrl", task.getResult().toString());
                        updateData();
                    }
                    progressDialog.dismiss();
                });
            } else {
                Toast.makeText(this, "Failed " + task0.getException().getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(taskSnapshot -> {
            double progress = ((100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
            progressDialog.setMessage("Saving " + (int) progress + "%");
        });
    }

    public enum Fragments {USERNAME, PROFILE_IMAGE, DETAILS}
}