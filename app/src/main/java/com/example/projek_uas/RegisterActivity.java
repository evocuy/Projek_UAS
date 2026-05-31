package com.example.projek_uas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projek_uas.databinding.ActivityRegisterBinding;
import com.example.projek_uas.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String TAG = "RegisterActivity";
    
    // FIX: Gunakan URL Region Singapore agar sinkron dengan Profile dan Home
    private static final String DB_URL = "https://uas-bobile-default-rtdb.asia-southeast1.firebasedatabase.app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        try {
            mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        } catch (Exception e) {
            Log.e(TAG, "DB Init Error: " + e.getMessage());
        }

        binding.btnRegisterNow.setOnClickListener(v -> register());
        binding.btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void register() {
        String email = binding.etEmailRegister.getText().toString().trim();
        String password = binding.etPasswordRegister.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email/Password wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnRegisterNow.setEnabled(false);
        Toast.makeText(this, "Mendaftarkan...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User user = new User(uid, "User " + uid.substring(0, 5), email, 100000);
                        
                        if (mDatabase != null) {
                            mDatabase.child("users").child(uid).setValue(user);
                        }
                        
                        Toast.makeText(RegisterActivity.this, "Berhasil Terdaftar!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        
                    } else {
                        binding.btnRegisterNow.setEnabled(true);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Gagal";
                        Toast.makeText(this, "Daftar Gagal: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
