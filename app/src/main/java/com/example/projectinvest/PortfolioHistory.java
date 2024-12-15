package com.example.projectinvest;

public class PortfolioHistory {
    private long timestamp;
    private double value;

    public PortfolioHistory(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }
}
