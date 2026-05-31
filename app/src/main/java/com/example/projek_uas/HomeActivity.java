package com.example.projek_uas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.projek_uas.databinding.ActivityHomeBinding;
import com.example.projek_uas.models.Transaction;
import com.example.projek_uas.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private User currentUser;
    private static final String DB_URL = "https://uas-bobile-default-rtdb.asia-southeast1.firebasedatabase.app/";

    // UPDATE: Menggunakan nama file yang sudah kamu ganti di drawable
    private int[] memeIcons = {
            R.drawable.bombardino,
            R.drawable.tungtungtung,
            R.drawable.brrbrrpatapim,
            R.drawable.capucinoassassino,
            R.drawable.tralalelotralala
    };
    private int[] currentResults = new int[9];
    private boolean isSpinning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mUserRef = FirebaseDatabase.getInstance(DB_URL).getReference("users").child(mAuth.getUid());
        loadUserData();

        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            }
            return id == R.id.nav_home;
        });

        binding.btnSpin.setOnClickListener(v -> spin());
    }

    private void loadUserData() {
        mUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    binding.tvBalanceHome.setText("Balance: Rp " + String.format("%,d", currentUser.balance));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeActivity", "DB Error: " + error.getMessage());
            }
        });
    }

    private void spin() {
        if (isSpinning) return;
        if (currentUser == null) {
            Toast.makeText(this, "Data belum dimuat...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.balance < 10000) {
            Toast.makeText(this, "Saldo tidak cukup!", Toast.LENGTH_SHORT).show();
            return;
        }

        isSpinning = true;
        currentUser.balance -= 10000;
        mUserRef.child("balance").setValue(currentUser.balance);

        Random random = new Random();
        Handler handler = new Handler();
        
        for (int i = 0; i < 10; i++) {
            handler.postDelayed(() -> {
                for (int j = 0; j < 9; j++) {
                    int randIdx = random.nextInt(memeIcons.length);
                    getImageView(j).setImageResource(memeIcons[randIdx]);
                }
            }, i * 100);
        }

        handler.postDelayed(() -> {
            for (int i = 0; i < 9; i++) {
                currentResults[i] = random.nextInt(memeIcons.length);
                getImageView(i).setImageResource(memeIcons[currentResults[i]]);
            }
            calculateWin();
            isSpinning = false;
        }, 1100);
    }

    private ImageView getImageView(int index) {
        switch (index) {
            case 0: return binding.slot1; case 1: return binding.slot2; case 2: return binding.slot3;
            case 3: return binding.slot4; case 4: return binding.slot5; case 5: return binding.slot6;
            case 6: return binding.slot7; case 7: return binding.slot8; case 8: return binding.slot9;
            default: return binding.slot1;
        }
    }

    private void calculateWin() {
        int matches = 0;
        if (currentResults[0] == currentResults[1] && currentResults[1] == currentResults[2]) matches++;
        if (currentResults[3] == currentResults[4] && currentResults[4] == currentResults[5]) matches++;
        if (currentResults[6] == currentResults[7] && currentResults[7] == currentResults[8]) matches++;
        if (currentResults[0] == currentResults[3] && currentResults[3] == currentResults[6]) matches++;
        if (currentResults[1] == currentResults[4] && currentResults[4] == currentResults[7]) matches++;
        if (currentResults[2] == currentResults[5] && currentResults[5] == currentResults[8]) matches++;
        if (currentResults[0] == currentResults[4] && currentResults[4] == currentResults[8]) matches++;
        if (currentResults[2] == currentResults[4] && currentResults[4] == currentResults[6]) matches++;

        long winAmount = (long) matches * 50000;
        if (winAmount > 0) {
            currentUser.balance += winAmount;
            mUserRef.child("balance").setValue(currentUser.balance);
            Toast.makeText(this, "MENANG! Dapat Rp " + String.format("%,d", winAmount), Toast.LENGTH_LONG).show();
        }
        saveTransaction(1, 10000, winAmount, matches + " matches found");
    }

    private void saveTransaction(int pulls, long cost, long win, String details) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance(DB_URL).getReference("history").child(mAuth.getUid());
        String id = historyRef.push().getKey();
        if (id != null) {
            Transaction t = new Transaction(id, System.currentTimeMillis(), pulls, cost, win, details);
            historyRef.child(id).setValue(t);
        }
    }
}
