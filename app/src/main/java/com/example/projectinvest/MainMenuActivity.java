package com.example.projectinvest;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        Button btnToCalculator = findViewById(R.id.btnToCalculator);
        btnToCalculator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Button btnToOther = findViewById(R.id.btnToOther);
        btnToOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, StockDataCalculatorActivity.class);
                startActivity(intent);
            }
        });

        Button btnToDemoInvestment = findViewById(R.id.btnToDemoInvestment);
        btnToDemoInvestment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenuActivity.this, DemoInvestmentActivity.class);
                startActivity(intent);
            }
        });
    }
}
