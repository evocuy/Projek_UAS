package com.example.projek_uas.models;

public class Transaction {
    public String id;
    public long timestamp;
    public int pullCount;
    public long cost;
    public long result;
    public String details; // e.g., "0 matches" or "3 horizontal lines"

    public Transaction() {
    }

    public Transaction(String id, long timestamp, int pullCount, long cost, long result, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.pullCount = pullCount;
        this.cost = cost;
        this.result = result;
        this.details = details;
    }
}
