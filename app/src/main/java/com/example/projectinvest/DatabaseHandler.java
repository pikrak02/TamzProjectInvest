package com.example.projectinvest;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "InvestmentDatabase";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_STOCKS = "stocks";
    private static final String KEY_ID = "id";
    private static final String KEY_SYMBOL = "symbol";
    private static final String KEY_PURCHASE_DATE = "purchase_date";
    private static final String KEY_SHARES = "shares";
    private static final String KEY_PRICE = "price";

    private static final String TABLE_USER = "user_account";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_BALANCE = "balance";

    private static final String TABLE_HISTORY = "portfolio_history";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_VALUE = "value";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_STOCKS_TABLE = "CREATE TABLE " + TABLE_STOCKS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_SYMBOL + " TEXT,"
                + KEY_PURCHASE_DATE + " TEXT," + KEY_SHARES + " INTEGER,"
                + KEY_PRICE + " REAL" + ")";
        db.execSQL(CREATE_STOCKS_TABLE);

        String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER + "("
                + KEY_USER_ID + " INTEGER PRIMARY KEY,"
                + KEY_BALANCE + " REAL" + ")";
        db.execSQL(CREATE_USER_TABLE);

        ContentValues values = new ContentValues();
        values.put(KEY_USER_ID, 1);
        values.put(KEY_BALANCE, 0.0);
        db.insert(TABLE_USER, null, values);

        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + KEY_TIMESTAMP + " INTEGER,"
                + KEY_VALUE + " REAL)";
        db.execSQL(CREATE_HISTORY_TABLE);

        ContentValues histValues = new ContentValues();
        histValues.put(KEY_TIMESTAMP, System.currentTimeMillis());
        histValues.put(KEY_VALUE, 0.0);
        db.insert(TABLE_HISTORY, null, histValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STOCKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public double getBalance() {
        double balance = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_BALANCE + " FROM " + TABLE_USER + " WHERE " + KEY_USER_ID + "=1", null);
        if (cursor.moveToFirst()) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }

    public void updateBalance(double newBalance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_BALANCE, newBalance);
        db.update(TABLE_USER, values, KEY_USER_ID + "=?", new String[]{"1"});
        recordPortfolioValue();
    }

    public void deposit(double amount) {
        double current = getBalance();
        double newBalance = current + amount;
        updateBalance(newBalance);
    }

    public boolean withdraw(double amount) {
        double current = getBalance();
        if (current >= amount) {
            double newBalance = current - amount;
            updateBalance(newBalance);
            return true;
        } else {
            return false;
        }
    }

    public void addStock(String symbol, String purchaseDate, int shares, double price) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_SYMBOL, symbol);
        values.put(KEY_PURCHASE_DATE, purchaseDate);
        values.put(KEY_SHARES, shares);
        values.put(KEY_PRICE, price);

        db.insert(TABLE_STOCKS, null, values);
        recordPortfolioValue();
    }

    public void sellStock(String symbol, int sharesToSell, Context context) {
        SQLiteDatabase db = this.getWritableDatabase();

        String query = "SELECT " + KEY_ID + ", " + KEY_SHARES + " FROM " + TABLE_STOCKS
                + " WHERE " + KEY_SYMBOL + " = ? ORDER BY " + KEY_PURCHASE_DATE;
        Cursor cursor = db.rawQuery(query, new String[]{symbol});

        int idIndex = cursor.getColumnIndex(KEY_ID);
        int sharesIndex = cursor.getColumnIndex(KEY_SHARES);

        while (cursor.moveToNext()) {
            if (idIndex != -1 && sharesIndex != -1) {
                int id = cursor.getInt(idIndex);
                int currentShares = cursor.getInt(sharesIndex);

                if (currentShares <= sharesToSell) {
                    db.delete(TABLE_STOCKS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
                    sharesToSell -= currentShares;
                } else {
                    ContentValues values = new ContentValues();
                    values.put(KEY_SHARES, currentShares - sharesToSell);
                    db.update(TABLE_STOCKS, values, KEY_ID + " = ?", new String[]{String.valueOf(id)});
                    sharesToSell = 0;
                }

                if (sharesToSell == 0) break;
            }
        }

        cursor.close();

        if (sharesToSell > 0) {
        } else {
            recordPortfolioValue();
        }
    }

    public void deleteAllStocks() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_STOCKS, null, null);
        recordPortfolioValue();
    }

    public List<Stock> getAllStocks() {
        List<Stock> stockList = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_STOCKS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int idIndex = cursor.getColumnIndex(KEY_ID);
        int symbolIndex = cursor.getColumnIndex(KEY_SYMBOL);
        int purchaseDateIndex = cursor.getColumnIndex(KEY_PURCHASE_DATE);
        int sharesIndex = cursor.getColumnIndex(KEY_SHARES);
        int priceIndex = cursor.getColumnIndex(KEY_PRICE);

        if (cursor.moveToFirst()) {
            do {
                if (idIndex != -1 && symbolIndex != -1 && purchaseDateIndex != -1 && sharesIndex != -1 && priceIndex != -1) {
                    Stock stock = new Stock();
                    stock.setId(cursor.getInt(idIndex));
                    stock.setSymbol(cursor.getString(symbolIndex));
                    stock.setPurchaseDate(cursor.getString(purchaseDateIndex));
                    stock.setNumberOfShares(cursor.getInt(sharesIndex));
                    stock.setPrice(cursor.getDouble(priceIndex));

                    stockList.add(stock);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();
        return stockList;
    }

    public double getPortfolioValue() {
        List<Stock> portfolio = getAllStocks();
        double total = 0;
        for (Stock s : portfolio) {
            total += s.getNumberOfShares() * s.getPrice();
        }
        total += getBalance();
        return total;
    }

    public void recordPortfolioValue() {
        double val = getPortfolioValue();
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TIMESTAMP, System.currentTimeMillis());
        values.put(KEY_VALUE, val);
        db.insert(TABLE_HISTORY, null, values);
    }

    public List<PortfolioHistory> getPortfolioHistory() {
        List<PortfolioHistory> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_TIMESTAMP + ", " + KEY_VALUE + " FROM " + TABLE_HISTORY + " ORDER BY " + KEY_TIMESTAMP, null);

        while (cursor.moveToNext()) {
            long ts = cursor.getLong(0);
            double val = cursor.getDouble(1);
            history.add(new PortfolioHistory(ts, val));
        }

        cursor.close();
        return history;
    }
}
