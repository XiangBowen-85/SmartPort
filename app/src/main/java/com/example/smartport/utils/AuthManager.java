package com.example.smartport.utils;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

public class AuthManager {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static int totalRolesToCheck = 4;

    public interface AuthCallback {
        void onResult(boolean success, String role);
    }

    public static void verifyCredentials(String username, String password, AuthCallback callback) {
        // Check in all user collections
        String[] roles = {"transportista", "seguridad", "controlador", "capitan"};
        final boolean[] checkedRoles = new boolean[roles.length];
        final boolean[] foundUser = {false};

        for (int i = 0; i < roles.length; i++) {
            final String role = roles[i];
            final int index = i;

            String documentId = role + "_" + username;

            db.collection("users").document(documentId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        checkedRoles[index] = true;

                        if (!foundUser[0] && documentSnapshot.exists()) {
                            String storedPassword = documentSnapshot.getString("password");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                foundUser[0] = true;
                                callback.onResult(true, role);
                                return;
                            }
                        }

                        // 检查是否所有角色都已检查完毕
                        if (allRolesChecked(checkedRoles) && !foundUser[0]) {
                            callback.onResult(false, null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        checkedRoles[index] = true;
                        Log.e("AuthManager", "Error checking role: " + role, e);

                        // 检查是否所有角色都已检查完毕
                        if (allRolesChecked(checkedRoles) && !foundUser[0]) {
                            callback.onResult(false, null);
                        }
                    });
        }

        /*for (String role : roles) {
            db.collection("users")
                    .document(role + "_" + username)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedPassword = documentSnapshot.getString("password");
                            if (storedPassword != null && storedPassword.equals(password)) {
                                callback.onResult(true, role);
                                return;
                            }
                        }
                        callback.onResult(false, null);
                    })
                    .addOnFailureListener(e -> {
                        callback.onResult(false, null);
                    });
        }*/
    }

    private static boolean allRolesChecked(boolean[] checkedRoles) {
        for (boolean checked : checkedRoles) {
            if (!checked) return false;
        }
        return true;
    }

    public static String getRoleDisplayName(String role) {
        switch (role) {
            case "transportista":
                return "Transportista";
            case "seguridad":
                return "Encargado de Seguridad";
            case "controlador":
                return "Controlador de Barcos";
            case "capitan":
                return "Capitán";
            default:
                return "Usuario";
        }
    }
}