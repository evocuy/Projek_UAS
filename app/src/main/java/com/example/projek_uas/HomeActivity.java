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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Random;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private DocumentReference mUserRef;
    private User currentUser;
    private ListenerRegistration mUserListener;
    private static final String TAG = "HomeActivity";

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

        mFirestore = FirebaseFirestore.getInstance();
        mUserRef = mFirestore.collection("users").document(mAuth.getUid());
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
        mUserListener = mUserRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed.", e);
                Toast.makeText(this, "Error Database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentUser = snapshot.toObject(User.class);
                if (currentUser != null) {
                    binding.tvBalanceHome.setText("Balance: Rp " + String.format("%,d", currentUser.balance));
                    // Update header title with user name if you want
                    Log.d(TAG, "Data loaded: " + currentUser.name);
                }
            } else {
                Toast.makeText(this, "Profil belum dibuat di database!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void spin() {
        if (isSpinning) return;
        if (currentUser == null) {
            Toast.makeText(this, "Menghubungkan ke database...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.balance < 10000) {
            Toast.makeText(this, "Saldo tidak cukup!", Toast.LENGTH_SHORT).show();
            return;
        }

        isSpinning = true;
        currentUser.balance -= 10000;
        mUserRef.update("balance", currentUser.balance);

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
            mUserRef.update("balance", currentUser.balance);
            Toast.makeText(this, "MENANG! Dapat Rp " + String.format("%,d", winAmount), Toast.LENGTH_LONG).show();
        }
        saveTransaction(1, 10000, winAmount, matches + " matches found");
    }

    private void saveTransaction(int pulls, long cost, long win, String details) {
        DocumentReference historyRef = mFirestore.collection("history").document(mAuth.getUid()).collection("transactions").document();
        Transaction t = new Transaction(historyRef.getId(), System.currentTimeMillis(), pulls, cost, win, details);
        historyRef.set(t).addOnFailureListener(e -> Log.e(TAG, "Gagal simpan histori", e));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUserListener != null) {
            mUserListener.remove();
        }
    }
}
