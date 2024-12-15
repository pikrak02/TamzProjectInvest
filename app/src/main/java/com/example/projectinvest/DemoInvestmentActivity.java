package com.example.projectinvest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.projectinvest.api.TwelveDataApiClient;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DemoInvestmentActivity extends AppCompatActivity {

    private EditText etStockSymbol, etNumberOfShares, etAmount;
    private Button btnBuyStock, btnSellStock, btnDeposit, btnWithdraw;
    private TextView tvPortfolio, tvBalance;
    private DatabaseHandler dbHandler;
    private Button btnOrder, btnBuyMode, btnSellMode;

    private TwelveDataApiClient apiClient;
    private PieChart portfolioPieChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_investment);

        etStockSymbol = findViewById(R.id.etStockSymbol);
        etNumberOfShares = findViewById(R.id.etNumberOfShares);
        etAmount = findViewById(R.id.etAmount);
        btnBuyStock = findViewById(R.id.btnBuyMode);
        btnSellStock = findViewById(R.id.btnSellMode);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        tvPortfolio = findViewById(R.id.tvPortfolio);
        tvBalance = findViewById(R.id.tvBalance);
        dbHandler = new DatabaseHandler(this);
        apiClient = new TwelveDataApiClient();
        portfolioPieChart = findViewById(R.id.portfolioPieChart);

        btnBuyStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { buyStock(); }
        });

        btnSellStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sellStock(); }
        });

        btnBuyMode = findViewById(R.id.btnBuyMode);
        btnSellMode = findViewById(R.id.btnSellMode);
        btnOrder = findViewById(R.id.btnOrder);

        btnBuyMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setBuyMode(); }
        });

        btnSellMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { setSellMode(); }
        });

        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnOrder.getText().toString().equalsIgnoreCase("Buy")) {
                    buyStock();
                } else {
                    sellStock();
                }
            }
        });

        btnDeposit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etAmount.getText().toString();
                if (amountStr.isEmpty()) return;
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(DemoInvestmentActivity.this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                dbHandler.deposit(amount);
                updateBalanceView();
                Toast.makeText(DemoInvestmentActivity.this, "Deposited $" + amount, Toast.LENGTH_SHORT).show();
            }
        });

        btnWithdraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = etAmount.getText().toString();
                if (amountStr.isEmpty()) return;
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(DemoInvestmentActivity.this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dbHandler.withdraw(amount)) {
                    updateBalanceView();
                    Toast.makeText(DemoInvestmentActivity.this, "Withdrew $" + amount, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DemoInvestmentActivity.this, "Not enough funds", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setBuyMode();
        updatePortfolioView();
        updateBalanceView();
    }

    private void setBuyMode() {
        btnOrder.setText("Buy");
    }

    private void setSellMode() {
        btnOrder.setText("Sell");
    }

    private void buyStock() {
        String stockSymbol = etStockSymbol.getText().toString();
        String numberOfSharesStr = etNumberOfShares.getText().toString();

        if (stockSymbol.isEmpty() || numberOfSharesStr.isEmpty()) {
            Toast.makeText(this, "Please fill stock symbol and number of shares", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfShares = Integer.parseInt(numberOfSharesStr);

        String purchaseDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double currentPrice = apiClient.getCurrentStockPrice(stockSymbol);
                    double totalCost = currentPrice * numberOfShares;

                    double balance = dbHandler.getBalance();
                    if (balance >= totalCost) {
                        dbHandler.updateBalance(balance - totalCost);
                        dbHandler.addStock(stockSymbol, purchaseDate, numberOfShares, currentPrice);
                        runOnUiThread(() -> {
                            updatePortfolioView();
                            updateBalanceView();
                            Toast.makeText(DemoInvestmentActivity.this, "Bought " + numberOfShares + " shares of " + stockSymbol, Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(DemoInvestmentActivity.this, "Not enough funds to buy", Toast.LENGTH_SHORT).show());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(DemoInvestmentActivity.this, "Error buying stock: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void sellStock() {
        String stockSymbol = etStockSymbol.getText().toString();
        String numberOfSharesStr = etNumberOfShares.getText().toString();

        if (stockSymbol.isEmpty() || numberOfSharesStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields for selling", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberOfShares = Integer.parseInt(numberOfSharesStr);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    double currentPrice = apiClient.getCurrentStockPrice(stockSymbol);
                    double gain = currentPrice * numberOfShares;

                    List<Stock> portfolio = dbHandler.getAllStocks();
                    int totalSharesOwned = 0;
                    for (Stock s : portfolio) {
                        if (s.getSymbol().equalsIgnoreCase(stockSymbol)) {
                            totalSharesOwned += s.getNumberOfShares();
                        }
                    }

                    if (totalSharesOwned < numberOfShares) {
                        runOnUiThread(() -> Toast.makeText(DemoInvestmentActivity.this, "Not enough shares to sell", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    dbHandler.sellStock(stockSymbol, numberOfShares, DemoInvestmentActivity.this);
                    double balance = dbHandler.getBalance();
                    dbHandler.updateBalance(balance + gain);

                    runOnUiThread(() -> {
                        updatePortfolioView();
                        updateBalanceView();
                        Toast.makeText(DemoInvestmentActivity.this, "Sold " + numberOfShares + " shares of " + stockSymbol + " for $" + gain, Toast.LENGTH_SHORT).show();
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(DemoInvestmentActivity.this, "Error selling stock: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void updatePortfolioView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Stock> portfolio = dbHandler.getAllStocks();
                if (portfolio.isEmpty()) {
                    runOnUiThread(() -> {
                        tvPortfolio.setText("No stocks in the portfolio.");
                        portfolioPieChart.setData(null);
                        portfolioPieChart.invalidate();
                    });
                    return;
                }

                StringBuilder portfolioDisplay = new StringBuilder();
                portfolioDisplay.append("----- Portfolio Overview -----\n\n");

                Map<String, Double> portfolioValueMap = new HashMap<>();
                double totalPortfolioValue = 0.0;

                for (Stock stock : portfolio) {
                    double purchasePrice = stock.getPrice();
                    int shares = stock.getNumberOfShares();
                    double currentPrice;
                    double value;
                    double profitLoss;
                    double profitLossPercentage = 0.0;

                    try {
                        currentPrice = apiClient.getCurrentStockPrice(stock.getSymbol());
                        value = currentPrice * shares;
                        totalPortfolioValue += value;
                        profitLoss = (currentPrice - purchasePrice) * shares;
                        if (purchasePrice > 0) {
                            profitLossPercentage = (profitLoss / (purchasePrice * shares)) * 100;
                        }

                        portfolioValueMap.put(stock.getSymbol(), portfolioValueMap.getOrDefault(stock.getSymbol(), 0.0) + value);

                        portfolioDisplay.append(String.format(Locale.US,
                                "Symbol: %s\n" +
                                        " Shares: %d\n" +
                                        " Purchase Price: $%.2f\n" +
                                        " Current Price: $%.2f\n" +
                                        " Value: $%.2f\n" +
                                        " Profit/Loss: $%.2f (%.2f%%)\n\n",
                                stock.getSymbol(), shares, purchasePrice, currentPrice, value, profitLoss, profitLossPercentage));

                    } catch (Exception e) {
                        e.printStackTrace();
                        portfolioDisplay.append(String.format("Symbol: %s - Error fetching current price: %s\n\n",
                                stock.getSymbol(), e.getMessage()));
                    }
                }

                portfolioDisplay.append("-----------------------------------\n");
                portfolioDisplay.append(String.format(Locale.US, "Total Portfolio Value: $%.2f\n", totalPortfolioValue));

                List<PieEntry> entries = new ArrayList<>();
                for (Map.Entry<String, Double> entry : portfolioValueMap.entrySet()) {
                    double percentage = (totalPortfolioValue > 0) ? (entry.getValue() / totalPortfolioValue) * 100 : 0;
                    entries.add(new PieEntry((float) entry.getValue().doubleValue(), entry.getKey() + " (" + String.format("%.1f%%", percentage) + ")"));
                }

                PieDataSet dataSet = new PieDataSet(entries, "Portfolio Composition");
                dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
                dataSet.setSliceSpace(3f);
                dataSet.setSelectionShift(5f);

                PieData pieData = new PieData(dataSet);
                pieData.setValueTextSize(12f);
                pieData.setValueTextColor(android.graphics.Color.WHITE);

                runOnUiThread(() -> {
                    tvPortfolio.setText(portfolioDisplay.toString());
                    portfolioPieChart.setData(pieData);
                    portfolioPieChart.setUsePercentValues(false);
                    portfolioPieChart.setCenterText("Portfolio\nDistribution");
                    portfolioPieChart.setCenterTextSize(14f);
                    portfolioPieChart.invalidate();
                });

            }
        }).start();
    }

    private void updateBalanceView() {
        double balance = dbHandler.getBalance();
        tvBalance.setText(String.format(Locale.US, "$%.2f", balance));
        updatePortfolioChart();
    }

    private void updatePortfolioChart() {
        LineChart portfolioChart = findViewById(R.id.portfolioChart);
        List<PortfolioHistory> history = dbHandler.getPortfolioHistory();

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            PortfolioHistory ph = history.get(i);
            entries.add(new Entry(i, (float) ph.getValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Portfolio Value Over Time");
        dataSet.setColor(getResources().getColor(R.color.primary_dark_color));
        dataSet.setCircleColor(getResources().getColor(R.color.primary_color));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);

        LineData lineData = new LineData(dataSet);
        portfolioChart.setData(lineData);
        portfolioChart.invalidate();
    }

}
