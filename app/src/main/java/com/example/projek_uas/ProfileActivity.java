package com.example.projek_uas;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projek_uas.databinding.ActivityProfileBinding;
import com.example.projek_uas.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DocumentReference mUserRef;
    private User currentUser;
    private ListenerRegistration mUserListener;
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mFirestore = FirebaseFirestore.getInstance();
        mUserRef = mFirestore.collection("users").document(mAuth.getUid());
        loadUserData();

        binding.btnUpdateName.setOnClickListener(v -> {
            String newName = binding.etProfileName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            
            mUserRef.update("name", newName).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Nama diperbarui!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Update failed", task.getException());
                    Toast.makeText(this, "Gagal update: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        binding.btnTopUp.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Menghubungkan ke server...", Toast.LENGTH_SHORT).show();
                return;
            }
            showTopUpDialog();
        });

        binding.btnWithdraw.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(this, "Menghubungkan ke server...", Toast.LENGTH_SHORT).show();
                return;
            }
            showWithdrawDialog();
        });

        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }
            return id == R.id.nav_profile;
        });
    }

    private void loadUserData() {
        mUserListener = mUserRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                Toast.makeText(this, "Error Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentUser = snapshot.toObject(User.class);
                if (currentUser != null) {
                    Log.d(TAG, "User data loaded: " + currentUser.name);
                    if (!binding.etProfileName.hasFocus()) {
                        binding.etProfileName.setText(currentUser.name);
                    }
                    if (binding.etProfileEmail != null) {
                        binding.etProfileEmail.setText(currentUser.email);
                    }
                    binding.tvProfileBalance.setText("Current Balance: Rp " + String.format("%,d", currentUser.balance));
                }
            } else {
                Log.w(TAG, "No such document for UID: " + mAuth.getUid());
                Toast.makeText(this, "Data User tidak ditemukan di database!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showTopUpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Top Up Saldo");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setHint("Nominal Top Up");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(input);

        final TextView label = new TextView(this);
        label.setText("Pilih Metode:");
        label.setPadding(0, 20, 0, 10);
        layout.addView(label);

        final RadioGroup methods = new RadioGroup(this);
        RadioButton rbQris = new RadioButton(this);
        rbQris.setText("QRIS");
        RadioButton rbBank = new RadioButton(this);
        rbBank.setText("Transfer Bank");
        methods.addView(rbQris);
        methods.addView(rbBank);
        rbQris.setChecked(true);
        layout.addView(methods);

        builder.setView(layout);

        builder.setPositiveButton("Top Up", (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                try {
                    long amount = Long.parseLong(val);
                    long newBalance = currentUser.balance + amount;
                    mUserRef.update("balance", newBalance).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Top Up Berhasil!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(this, "Nominal tidak valid", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void showWithdrawDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tarik Saldo");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setHint("Nominal Penarikan");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(input);

        final TextView label = new TextView(this);
        label.setText("Pilih Metode:");
        label.setPadding(0, 20, 0, 10);
        layout.addView(label);

        final RadioGroup methods = new RadioGroup(this);
        RadioButton rbQris = new RadioButton(this);
        rbQris.setText("QRIS");
        RadioButton rbBank = new RadioButton(this);
        rbBank.setText("Transfer Bank");
        methods.addView(rbQris);
        methods.addView(rbBank);
        rbQris.setChecked(true);
        layout.addView(methods);

        builder.setView(layout);

        builder.setPositiveButton("Tarik", (dialog, which) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                try {
                    long amount = Long.parseLong(val);
                    if (amount > currentUser.balance) {
                        Toast.makeText(this, "Saldo tidak cukup!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount < 10000) {
                        Toast.makeText(this, "Minimal penarikan Rp 10.000", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    long newBalance = currentUser.balance - amount;
                    mUserRef.update("balance", newBalance).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Penarikan Berhasil Diproses!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(this, "Nominal tidak valid", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUserListener != null) {
            mUserListener.remove();
        }
    }
}
