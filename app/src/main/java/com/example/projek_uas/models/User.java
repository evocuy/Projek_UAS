package com.example.projek_uas.models;

public class User {
    public String uid;
    public String name;
    public String email;
    public long balance;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String uid, String name, String email, long balance) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.balance = balance;
    }
}
