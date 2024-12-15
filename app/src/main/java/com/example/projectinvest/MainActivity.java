package com.example.projectinvest;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

public class MainActivity extends AppCompatActivity {

    private EditText initialDeposit, interestRate, monthlyAddition, investmentDuration;
    private TextView resultView;
    private Button calculateButton;
    private LineChart monthlyChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialDeposit = findViewById(R.id.initialDeposit);
        interestRate = findViewById(R.id.interestRate);
        monthlyAddition = findViewById(R.id.monthlyAddition);
        investmentDuration = findViewById(R.id.investmentDuration);
        calculateButton = findViewById(R.id.calculateButton);
        resultView = findViewById(R.id.resultView);
        monthlyChart = findViewById(R.id.monthlyChart);

        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { calculateCompoundInterest(); }
        });
    }

    private void calculateCompoundInterest() {
        try {
            double firstDeposit = Double.parseDouble(initialDeposit.getText().toString());
            double annualRate = Double.parseDouble(interestRate.getText().toString());
            double monthlyRate = annualRate / 12 / 100;
            double monthlyContribution = Double.parseDouble(monthlyAddition.getText().toString());
            int years = Integer.parseInt(investmentDuration.getText().toString());

            double futureValue = firstDeposit;
            List<Entry> entries = new ArrayList<>();

            int monthIndex = 0;
            for (int i = 1; i <= years; i++) {
                for (int j = 0; j < 12; j++) {
                    futureValue += monthlyContribution;
                    futureValue += futureValue * monthlyRate;
                    monthIndex++;
                    entries.add(new Entry(monthIndex, (float) futureValue));
                }
            }

            resultView.setText(String.format("Final Amount: $%,.2f", futureValue));

            LineDataSet dataSet = new LineDataSet(entries, "Investment Growth");
            dataSet.setColor(getResources().getColor(R.color.primary_dark_color));
            dataSet.setCircleColor(getResources().getColor(R.color.primary_color));
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(3f);

            LineData lineData = new LineData(dataSet);
            monthlyChart.setData(lineData);
            monthlyChart.invalidate();

            com.example.projectinvest.marker.MyMarkerView marker = new com.example.projectinvest.marker.MyMarkerView(this, R.layout.marker_view_layout);
            marker.setChartView(monthlyChart);
            monthlyChart.setMarker(marker);

        } catch (NumberFormatException e) {
            resultView.setText("Please enter valid numbers");
            monthlyChart.setData(null);
            monthlyChart.invalidate();
        }
    }
}
