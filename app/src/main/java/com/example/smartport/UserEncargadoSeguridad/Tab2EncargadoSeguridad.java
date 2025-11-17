package com.example.smartport.UserEncargadoSeguridad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.smartport.R;
import com.example.smartport.SelectionActivity;
import com.example.smartport.UserProfile;
import com.example.smartport.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Tab2EncargadoSeguridad extends Fragment {

    private static final String TAG = "Tab2EncargadoSeguridad";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Views
    private Toolbar toolbar;
    private ImageView ivProfilePhoto;
    private ImageButton btnEditPhoto;
    private TextInputEditText etUsername, etPassword, etPhone, etEmail;
    private Button btnEditUsername, btnEditPassword, btnSave;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentReference userDocRef;

    // User data
    private UserProfile currentUser;
    private String currentRole;
    private String currentUsername;

    // Session
    private SessionManager sessionManager;

    // Camera
    private Uri cameraImageUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        sessionManager = new SessionManager(getContext());

        // Get current user info from session
        currentRole = sessionManager.getUserRole();
        currentUsername = sessionManager.getUsername();

        Log.d(TAG, "User role: " + currentRole + ", username: " + currentUsername);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab2_encargadoseguridad, container, false);

        initViews(view);
        setupToolbar();
        setupClickListeners();
        loadUserDataFromFirestore();

        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        btnEditPhoto = view.findViewById(R.id.btnEditPhoto);
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        btnEditUsername = view.findViewById(R.id.btnEditUsername);
        btnEditPassword = view.findViewById(R.id.btnEditPassword);
        btnSave = view.findViewById(R.id.btnSave);

        // Initially disable editing for username and password
        etUsername.setEnabled(false);
        etPassword.setEnabled(false);
    }

    private void setupToolbar() {
        // 设置 Toolbar 的菜单项点击监听器
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_logout) {
                    showLogoutConfirmation();
                    return true;
                }
                return false;
            }
        });

        // 确保 Toolbar 显示菜单
        toolbar.inflateMenu(R.menu.profile_menu);
    }

    private void setupClickListeners() {
        // Edit Photo Button
        btnEditPhoto.setOnClickListener(v -> showImageSourceDialog());

        // Edit Username Button
        btnEditUsername.setOnClickListener(v -> {
            boolean enabled = !etUsername.isEnabled();
            etUsername.setEnabled(enabled);
            btnEditUsername.setText(enabled ? "Guardar" : "Editar");

            if (!enabled) {
                // Save username changes
                saveUsernameToFirestore();
            }
        });

        // Edit Password Button
        btnEditPassword.setOnClickListener(v -> {
            boolean enabled = !etPassword.isEnabled();
            etPassword.setEnabled(enabled);
            btnEditPassword.setText(enabled ? "Guardar" : "Editar");

            if (!enabled) {
                // Save password changes
                savePasswordToFirestore();
            }
        });

        // Save All Changes Button
        btnSave.setOnClickListener(v -> saveAllChangesToFirestore());
    }

    private void loadUserDataFromFirestore() {
        if (currentRole == null || currentUsername == null) {
            Toast.makeText(getContext(), "Error: No user information found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Loading user data for: " + currentRole + "_" + currentUsername);

        // Reference to user document in Firestore
        userDocRef = db.collection("users").document(currentRole + "_" + currentUsername);

        userDocRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed: " + error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                currentUser = documentSnapshot.toObject(UserProfile.class);
                if (currentUser != null) {
                    updateUIWithUserData();
                    Log.d(TAG, "User data loaded successfully");
                }
            } else {
                // Create new user profile if doesn't exist
                Log.d(TAG, "User document doesn't exist, creating new profile");
                createNewUserProfile();
            }
        });
    }

    private void updateUIWithUserData() {
        if (currentUser == null) return;

        // Set user data to views
        etUsername.setText(currentUser.getUsername());
        etPassword.setText(currentUser.getPassword());
        etPhone.setText(currentUser.getPhone());
        etEmail.setText(currentUser.getEmail());

        // Load profile photo if exists
        if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty()) {
            try {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.drawable.placeholder_profile)
                        .error(R.drawable.placeholder_profile)
                        .into(ivProfilePhoto);
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile photo from URL", e);
            }
        }
    }

    private void createNewUserProfile() {
        currentUser = new UserProfile(
                currentUsername,
                getDefaultPassword(currentRole),
                "",
                "",
                ""
        );

        userDocRef.set(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User profile created successfully");
                    updateUIWithUserData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user profile", e);
                    Toast.makeText(getContext(), "Error creating profile", Toast.LENGTH_SHORT).show();
                });
    }

    private String getDefaultPassword(String role) {
        switch (role) {
            case "transportista": return "trans123";
            case "seguridad": return "segur123";
            case "controlador": return "control123";
            case "capitan": return "capitan123";
            default: return "default123";
        }
    }

    private void showImageSourceDialog() {
        CharSequence[] options = {"Tomar Foto", "Elegir de Galería", "Cancelar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Elegir Foto de Perfil");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Camera
                if (checkCameraPermission()) {
                    openCamera();
                }
            } else if (which == 1) {
                // Gallery
                if (checkStoragePermission()) {
                    openGallery();
                }
            }
        });
        builder.show();
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{android.Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getActivity().getPackageName())));
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                }
                return false;
            }
        } else {
            // Android 10 and below
            if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permiso concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permiso denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Create a file to save the image
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(getContext(),
                        getContext().getPackageName() + ".fileprovider",
                        photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                } else {
                    Toast.makeText(getContext(), "No se puede abrir la cámara", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            Toast.makeText(getContext(), "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    private void openGallery() {
        try {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery", e);
            Toast.makeText(getContext(), "Error al abrir la galería", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                // Handle gallery selection
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    // Display the selected image
                    displayImage(selectedImageUri);
                    // Upload to Firebase
                    uploadImageToFirebase(selectedImageUri);
                }
            } else if (requestCode == CAMERA_REQUEST) {
                // Handle camera capture
                if (cameraImageUri != null) {
                    // Display the captured image
                    displayImage(cameraImageUri);
                    // Upload to Firebase
                    uploadImageToFirebase(cameraImageUri);
                } else if (data != null && data.getExtras() != null) {
                    // Fallback: get thumbnail from extras
                    android.graphics.Bitmap thumbnail = (android.graphics.Bitmap) data.getExtras().get("data");
                    if (thumbnail != null) {
                        ivProfilePhoto.setImageBitmap(thumbnail);
                        // Convert bitmap to file and upload
                        uploadBitmapToFirebase(thumbnail);
                    }
                }
            }
        }
    }

    private void displayImage(Uri imageUri) {
        try {
            // Use Glide to load and display the image
            Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.placeholder_profile)
                    .error(R.drawable.placeholder_profile)
                    .into(ivProfilePhoto);
        } catch (Exception e) {
            Log.e(TAG, "Error displaying image", e);
            // Fallback to setImageURI
            ivProfilePhoto.setImageURI(imageUri);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(getContext(), "Error: URI de imagen nulo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        Toast.makeText(getContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show();

        // Create unique filename
        String filename = "profile_" + currentUsername + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child("profile_images/" + filename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        updateProfilePhoto(downloadUrl);
                        Toast.makeText(getContext(), "Imagen subida exitosamente", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading image", e);
                    Toast.makeText(getContext(), "Error subiendo imagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Show upload progress if needed
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + progress + "%");
                });
    }

    private void uploadBitmapToFirebase(android.graphics.Bitmap bitmap) {
        // Convert bitmap to byte array
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        // Create unique filename
        String filename = "profile_" + currentUsername + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = storage.getReference().child("profile_images/" + filename);

        storageRef.putBytes(imageData)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        updateProfilePhoto(downloadUrl);
                        Toast.makeText(getContext(), "Imagen subida exitosamente", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading bitmap", e);
                    Toast.makeText(getContext(), "Error subiendo imagen", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfilePhoto(String imageUrl) {
        if (currentUser == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("photoUrl", imageUrl);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
                    currentUser.setPhotoUrl(imageUrl);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile photo", e);
                    Toast.makeText(getContext(), "Error actualizando foto", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUsernameToFirestore() {
        String newUsername = etUsername.getText().toString().trim();

        if (newUsername.isEmpty()) {
            Toast.makeText(getContext(), "El usuario no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Usuario actualizado", Toast.LENGTH_SHORT).show();
                    currentUser.setUsername(newUsername);

                    // Update session
                    sessionManager.saveUserCredentials(newUsername, currentUser.getPassword(), currentRole);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating username", e);
                    Toast.makeText(getContext(), "Error actualizando usuario", Toast.LENGTH_SHORT).show();
                });
    }

    private void savePasswordToFirestore() {
        String newPassword = etPassword.getText().toString().trim();

        if (newPassword.isEmpty()) {
            Toast.makeText(getContext(), "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("password", newPassword);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show();
                    currentUser.setPassword(newPassword);

                    // Update session
                    sessionManager.saveUserCredentials(currentUser.getUsername(), newPassword, currentRole);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating password", e);
                    Toast.makeText(getContext(), "Error actualizando contraseña", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveAllChangesToFirestore() {
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("phone", phone);
        updates.put("email", email);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cambios guardados", Toast.LENGTH_SHORT).show();
                    if (currentUser != null) {
                        currentUser.setPhone(phone);
                        currentUser.setEmail(email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving changes", e);
                    Toast.makeText(getContext(), "Error guardando cambios", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLogoutConfirmation() {
        Log.d(TAG, "Logout confirmation dialog shown");

        new AlertDialog.Builder(getContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    Log.d(TAG, "User confirmed logout");
                    logout();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    Log.d(TAG, "User cancelled logout");
                    dialog.dismiss();
                })
                .show();
    }

    private void logout() {
        Log.d(TAG, "Starting logout process");

        // Clear session
        sessionManager.clearSession();

        // Navigate to SelectionActivity
        Intent intent = new Intent(getActivity(), SelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Finish the current activity
        if (getActivity() != null) {
            getActivity().finish();
        }

        Toast.makeText(getContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Logout completed successfully");
    }
}