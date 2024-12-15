package com.example.projectinvest;

public class Stock {
    private int id;
    private String symbol;
    private String purchaseDate;
    private int numberOfShares;
    private double price;

    public Stock() {
    }

    public Stock(int id, String symbol, String purchaseDate, int numberOfShares, double price) {
        this.id = id;
        this.symbol = symbol;
        this.purchaseDate = purchaseDate;
        this.numberOfShares = numberOfShares;
        this.price = price;
    }

    public int getId() {
        return id;
    }
    public double getPrice() {
        return price;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public int getNumberOfShares() {
        return numberOfShares;
    }

    public void setNumberOfShares(int numberOfShares) {
        this.numberOfShares = numberOfShares;
    }

    public void setPrice(double aDouble) {
        price = aDouble;
    }
}
