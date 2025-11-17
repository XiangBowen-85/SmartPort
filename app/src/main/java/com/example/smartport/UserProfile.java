package com.example.smartport;

public class UserProfile {
    private String username;
    private String password;
    private String phone;
    private String email;
    private String photoUrl;
    private String role;

    // Required empty constructor for Firestore
    public UserProfile() {}

    public UserProfile(String username, String password, String phone, String email, String photoUrl) {
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
