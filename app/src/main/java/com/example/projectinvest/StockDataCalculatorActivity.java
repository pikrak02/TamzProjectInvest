package com.example.projectinvest;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectinvest.api.TwelveDataApiClient;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.text.TextWatcher;
import android.text.Editable;
import android.widget.ArrayAdapter;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class StockDataCalculatorActivity extends AppCompatActivity {

    private AutoCompleteTextView stockSymbol;
    private TextView stockDataResult;
    private Button fetchDataButton;
    private EditText startDate, endDate, monthlyContribution;
    private LineChart stockChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_data_calculator);

        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        monthlyContribution = findViewById(R.id.monthlyContribution);
        stockDataResult = findViewById(R.id.stockDataResult);
        fetchDataButton = findViewById(R.id.fetchDataButton);
        stockSymbol = findViewById(R.id.stockSymbol);
        stockChart = findViewById(R.id.stockChart);

        stockSymbol.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 1) {
                    loadStockSymbols(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showDatePickerDialog(startDate, true); }
        });

        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { showDatePickerDialog(endDate, false); }
        });

        fetchDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { fetchStockData(); }
        });
    }

    private void fetchStockData() {
        String symbol = stockSymbol.getText().toString();
        String start = startDate.getText().toString();
        String end = endDate.getText().toString();
        String monthlyInvestmentString = monthlyContribution.getText().toString();

        if (symbol.isEmpty() || start.isEmpty() || end.isEmpty() || monthlyInvestmentString.isEmpty()) {
            displayErrorMessage("Please fill in all fields.");
            return;
        }

        if (isDateInFuture(end)) {
            displayErrorMessage("End date cannot be in the future.");
            return;
        }

        double monthlyInvestment;
        try {
            monthlyInvestment = Double.parseDouble(monthlyInvestmentString);
        } catch (NumberFormatException e) {
            displayErrorMessage("Invalid monthly contribution amount.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = fetchDataInChunks(symbol, start, end);
                    Log.d("API_RESPONSE", response);

                    if (isInvalidStockData(response)) {
                        runOnUiThread(() -> displayErrorMessage("This stock does not exist or no data available."));
                        return;
                    }

                    Date earliestDate = getEarliestDateFromData(response);
                    if (earliestDate == null) {
                        runOnUiThread(() -> displayErrorMessage("No valid data available."));
                        return;
                    }

                    if (isDateBeforeGivenDate(start, earliestDate)) {
                        runOnUiThread(() -> displayErrorMessage("Start date is before the stock was available on the market."));
                        return;
                    }

                    calculateInvestmentReturns(response, monthlyInvestment);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> displayErrorMessage("Error fetching data."));
                }
            }
        }).start();
    }

    private boolean isDateInFuture(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date date = sdf.parse(dateStr);
            return date != null && date.after(new Date());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isDateBeforeGivenDate(String dateStr, Date limitDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date chosenDate = sdf.parse(dateStr);
            return chosenDate.before(limitDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void displayErrorMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stockDataResult.setText(message);
                stockChart.setData(null);
                stockChart.invalidate();
            }
        });
    }

    private String fetchDataInChunks(String symbol, String start, String end) throws IOException {
        TwelveDataApiClient apiClient = new TwelveDataApiClient();
        return apiClient.getTimeSeriesData(symbol, "1day", start, end);
    }

    private boolean isInvalidStockData(String response) {
        if (response == null || response.isEmpty()) return true;
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (!jsonResponse.has("values")) return true;
            JSONArray values = jsonResponse.getJSONArray("values");
            return values.length() == 0;
        } catch (JSONException e) {
            e.printStackTrace();
            return true;
        }
    }


    private Date getEarliestDateFromData(String apiResponse) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            JSONObject jsonResponse = new JSONObject(apiResponse);
            JSONArray values = jsonResponse.getJSONArray("values");
            if (values.length() == 0) return null;

            JSONObject oldestDay = values.getJSONObject(values.length() - 1);
            String oldestDateStr = oldestDay.getString("datetime");
            return sdf.parse(oldestDateStr);
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void calculateInvestmentReturns(String apiResponse, double monthlyInvestment) {
        double totalInvestment = 0;
        double totalSharesPurchased = 0;
        double finalValue;
        List<Entry> entries = new ArrayList<>();

        try {
            JSONObject jsonResponse = new JSONObject(apiResponse);
            JSONArray values = jsonResponse.getJSONArray("values");

            if (values.length() == 0) {
                runOnUiThread(() -> displayErrorMessage("No data available for this stock in selected range."));
                return;
            }

            String lastProcessedMonth = "";
            double totalValue = 0;
            int monthIndex = 0;

            for (int i = values.length() - 1; i >= 0; i--) {
                JSONObject dayValue = values.getJSONObject(i);
                String datetime = dayValue.getString("datetime");
                double closePrice = dayValue.getDouble("close");

                String currentMonth = datetime.substring(0, 7);
                if (!currentMonth.equals(lastProcessedMonth)) {
                    totalSharesPurchased += (monthlyInvestment / closePrice);
                    totalValue = totalSharesPurchased * closePrice;
                    lastProcessedMonth = currentMonth;
                    totalInvestment += monthlyInvestment;

                    monthIndex++;
                    entries.add(new Entry(monthIndex, (float) totalValue));
                }
            }

            double lastClosePrice = values.getJSONObject(0).getDouble("close");
            finalValue = totalSharesPurchased * lastClosePrice;
            double totalInterest = finalValue - totalInvestment;

            final double fv = finalValue;
            final double ti = totalInterest;
            final double tInv = totalInvestment;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stockDataResult.setText(String.format(Locale.US,
                            "Final Investment Value: $%,.2f\nTotal Invested: $%,.2f\nTotal Interest: $%,.2f",
                            fv, tInv, ti));

                    LineDataSet dataSet = new LineDataSet(entries, "Investment Growth");
                    dataSet.setColor(getResources().getColor(R.color.primary_dark_color));
                    dataSet.setCircleColor(getResources().getColor(R.color.primary_color));
                    dataSet.setLineWidth(2f);
                    dataSet.setCircleRadius(3f);

                    LineData lineData = new LineData(dataSet);
                    stockChart.setData(lineData);
                    stockChart.invalidate();

                    com.example.projectinvest.marker.MyMarkerView marker = new com.example.projectinvest.marker.MyMarkerView(StockDataCalculatorActivity.this, R.layout.marker_view_layout);
                    marker.setChartView(stockChart);
                    stockChart.setMarker(marker);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> displayErrorMessage("Error parsing data."));
        }
    }

    private void loadStockSymbols(String query) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = callSymbolSearchApi(query);
                    List<String> symbols = parseSymbolsFromResponse(response);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (symbols.isEmpty()) {
                                displayErrorMessage("This stock does not exist.");
                            } else {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(StockDataCalculatorActivity.this, android.R.layout.simple_dropdown_item_1line, symbols);
                                stockSymbol.setAdapter(adapter);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> displayErrorMessage("Error searching for symbols."));
                }
            }
        }).start();
    }

    private String callSymbolSearchApi(String query) throws IOException {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://twelve-data1.p.rapidapi.com/symbol_search?symbol=" + query + "&outputsize=30")
                .get()
                .addHeader("X-RapidAPI-Key", "683a5fe792msh21d9cb793fbc1c2p15eaf8jsnf0bfe49f664b") // váš klíč
                .addHeader("X-RapidAPI-Host", "twelve-data1.p.rapidapi.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    private List<String> parseSymbolsFromResponse(String response) {
        List<String> symbols = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            if (!jsonResponse.has("data")) return symbols;
            JSONArray dataArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject symbolObject = dataArray.getJSONObject(i);
                String symbol = symbolObject.getString("symbol");
                symbols.add(symbol);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return symbols;
    }

    private void showDatePickerDialog(final EditText dateField, boolean isStartDate) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel(dateField, calendar);
            }
        };

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                StockDataCalculatorActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));


        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void updateLabel(EditText editText, Calendar calendar) {
        String format = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        editText.setText(sdf.format(calendar.getTime()));
    }
}
