package com.example.smartport.utils;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class UserInitializer {
    private static final String TAG = "UserInitializer";
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface InitializationCallback {
        void onComplete(boolean success, String message);
    }

    public static void initializeDefaultUsers(InitializationCallback callback) {
        Log.d(TAG, "Starting user initialization...");

        // 创建默认用户数据
        Map<String, Map<String, Object>> defaultUsers = createDefaultUsers();

        final int[] completed = {0};
        final int totalUsers = defaultUsers.size();
        final boolean[] hasError = {false};

        for (Map.Entry<String, Map<String, Object>> entry : defaultUsers.entrySet()) {
            String documentId = entry.getKey();
            Map<String, Object> userData = entry.getValue();

            db.collection("users").document(documentId)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        completed[0]++;
                        Log.d(TAG, "Successfully created user: " + documentId);

                        if (completed[0] == totalUsers) {
                            if (callback != null) {
                                callback.onComplete(true, "All users initialized successfully");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        completed[0]++;
                        hasError[0] = true;
                        Log.e(TAG, "Error creating user: " + documentId, e);

                        if (completed[0] == totalUsers) {
                            if (callback != null) {
                                callback.onComplete(false, "Some users failed to initialize");
                            }
                        }
                    });
        }
    }

    private static Map<String, Map<String, Object>> createDefaultUsers() {
        Map<String, Map<String, Object>> users = new HashMap<>();

        // Transportista 用户
        users.put("transportista_transportista1", createUserData(
                "transportista1", "trans123", "+34 600 000 001",
                "transportista1@smartport.com", "transportista"
        ));

        // Seguridad 用户
        users.put("seguridad_seguridad1", createUserData(
                "seguridad1", "segur123", "+34 600 000 002",
                "seguridad1@smartport.com", "seguridad"
        ));

        // Controlador 用户
        users.put("controlador_controlador1", createUserData(
                "controlador1", "control123", "+34 600 000 003",
                "controlador1@smartport.com", "controlador"
        ));

        // Capitán 用户
        users.put("capitan_capitan1", createUserData(
                "capitan1", "capitan123", "+34 600 000 004",
                "capitan1@smartport.com", "capitan"
        ));

        return users;
    }

    private static Map<String, Object> createUserData(String username, String password,
                                                      String phone, String email, String role) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", password);
        userData.put("phone", phone);
        userData.put("email", email);
        userData.put("photoUrl", "");
        userData.put("role", role);
        return userData;
    }

    public static void checkAndInitializeUsers(InitializationCallback callback) {
        // 检查是否已经有用户数据
        db.collection("users").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // 没有用户数据，进行初始化
                        initializeDefaultUsers(callback);
                    } else {
                        // 已经有用户数据
                        if (callback != null) {
                            callback.onComplete(true, "Users already exist");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking users", e);
                    if (callback != null) {
                        callback.onComplete(false, "Error checking users: " + e.getMessage());
                    }
                });
    }
}