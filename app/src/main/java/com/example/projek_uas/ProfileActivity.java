package com.example.projek_uas;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projek_uas.databinding.ActivityProfileBinding;
import com.example.projek_uas.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private User currentUser;

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

        mUserRef = FirebaseDatabase.getInstance().getReference("users").child(mAuth.getUid());
        loadUserData();

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

        binding.btnUpdateName.setOnClickListener(v -> updateName());
        binding.btnTopUp.setOnClickListener(v -> showTopUpDialog());
        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void loadUserData() {
        mUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    binding.etProfileName.setText(currentUser.name);
                    binding.tvProfileBalance.setText("Current Balance: Rp " + String.format("%,d", currentUser.balance));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateName() {
        String newName = binding.etProfileName.getText().toString().trim();
        if (!newName.isEmpty()) {
            mUserRef.child("name").setValue(newName)
                    .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Name updated!", Toast.LENGTH_SHORT).show());
        }
    }

    private void showTopUpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Top Up Saldo");

        final EditText input = new EditText(this);
        input.setHint("Nominal (e.g. 50000)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String amountStr = input.getText().toString();
            if (!amountStr.isEmpty()) {
                long amount = Long.parseLong(amountStr);
                showPaymentMethodDialog(amount);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showPaymentMethodDialog(long amount) {
        String[] methods = {"QRIS", "Transfer Bank"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pilih Metode Pembayaran");
        builder.setItems(methods, (dialog, which) -> {
            // Demo only: immediately add balance
            if (currentUser != null) {
                long newBalance = currentUser.balance + amount;
                mUserRef.child("balance").setValue(newBalance)
                        .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Top up berhasil! Saldo bertambah.", Toast.LENGTH_SHORT).show());
            }
        });
        builder.show();
    }
}
