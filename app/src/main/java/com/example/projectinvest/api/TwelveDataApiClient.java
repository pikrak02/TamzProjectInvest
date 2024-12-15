package com.example.projectinvest.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TwelveDataApiClient {

    private OkHttpClient client = new OkHttpClient();

    public String getTimeSeriesData(String symbol, String interval, String startDate, String endDate) throws IOException {
        String url = "https://twelve-data1.p.rapidapi.com/time_series?symbol=" + symbol +
                "&interval=" + interval + "&start_date=" + startDate + "&end_date=" + endDate +
                "&outputsize=5000&format=json";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", "683a5fe792msh21d9cb793fbc1c2p15eaf8jsnf0bfe49f664b")
                .addHeader("X-RapidAPI-Host", "twelve-data1.p.rapidapi.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        }
    }

    public double getCurrentStockPrice(String symbol) throws IOException {
        String url = "https://twelve-data1.p.rapidapi.com/price?symbol=" + symbol + "&format=json";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-RapidAPI-Key", "683a5fe792msh21d9cb793fbc1c2p15eaf8jsnf0bfe49f664b")
                .addHeader("X-RapidAPI-Host", "twelve-data1.p.rapidapi.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String jsonStr = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("Unsuccessful response: " + response.code() + ", body: " + jsonStr);
            }

            JSONObject jsonResponse = new JSONObject(jsonStr);

            if (!jsonResponse.has("price")) {
                String message = jsonResponse.has("message") ? jsonResponse.getString("message") : "No 'price' field in JSON";
                throw new IOException("API did not return a 'price' field. Full response: " + jsonStr + " Message: " + message);
            }

            return jsonResponse.getDouble("price");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new IOException("Error parsing JSON response: " + e.getMessage());
        }
    }
}
