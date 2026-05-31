package com.example.projek_uas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.projek_uas.databinding.ActivityHistoryBinding;
import com.example.projek_uas.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;
    private List<Transaction> transactionList = new ArrayList<>();
    private HistoryAdapter adapter;
    // FIX: Gunakan URL Region Singapore agar sinkron
    private static final String DB_URL = "https://uas-bobile-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private DatabaseReference mHistoryRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mHistoryRef = FirebaseDatabase.getInstance(DB_URL).getReference("history").child(auth.getUid());

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(transactionList);
        binding.rvHistory.setAdapter(adapter);

        binding.bottomNavigation.setSelectedItemId(R.id.nav_history);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return id == R.id.nav_history;
        });

        binding.btnClearHistory.setOnClickListener(v -> confirmClearHistory());

        loadHistoryData();
    }

    private void loadHistoryData() {
        mHistoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Transaction t = ds.getValue(Transaction.class);
                    if (t != null) transactionList.add(t);
                }
                Collections.reverse(transactionList);
                adapter.notifyDataSetChanged();
                
                // Hapus message "Belum ada riwayat" agar tidak bentrok
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Gagal memuat data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmClearHistory() {
        if (transactionList.isEmpty()) {
            Toast.makeText(this, "Riwayat sudah kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Hapus Riwayat")
                .setMessage("Apakah Anda yakin ingin menghapus semua riwayat spin?")
                .setPositiveButton("Ya, Hapus", (dialog, which) -> {
                    mHistoryRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(HistoryActivity.this, "Riwayat berhasil dihapus", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HistoryActivity.this, "Gagal menghapus riwayat", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Transaction> list;
        private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

        HistoryAdapter(List<Transaction> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction t = list.get(position);
            holder.tvDate.setText(sdf.format(new Date(t.timestamp)));
            holder.tvPulls.setText("Spin: -Rp " + String.format("%,d", t.cost));
            
            if (t.result > 0) {
                holder.tvResult.setText("+Rp " + String.format("%,d", t.result));
                holder.tvResult.setTextColor(0xFF4CAF50);
            } else {
                holder.tvResult.setText("+Rp 0");
                holder.tvResult.setTextColor(0xFFF44336);
            }
            
            holder.tvDetails.setText(t.details);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvPulls, tvResult, tvDetails;
            ViewHolder(View v) {
                super(v);
                tvDate = v.findViewById(R.id.tvHistoryDate);
                tvPulls = v.findViewById(R.id.tvHistoryPulls);
                tvResult = v.findViewById(R.id.tvHistoryResult);
                tvDetails = v.findViewById(R.id.tvHistoryDetails);
            }
        }
    }
}
