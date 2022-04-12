package com.vl.convectorcb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements HttpConnector.HttpListener {
    final static String CURRENT_VALUTE = "USD";
    TextView title;
    long time;

    static private String parseResponce(String jsonString) {
        String result;
        Gson gson = new Gson();
        Type T = new TypeToken<Map<String, Object>>(){}.getType();
        Map map = gson.fromJson(jsonString, T);
        String slaveString = gson.toJson(map.get("Valute"));
        map = gson.fromJson(slaveString, T);
        slaveString = gson.toJson(map.get(CURRENT_VALUTE));
        Valute USD = gson.fromJson(slaveString, Valute.class);
        result = USD.getName()
                .concat(" или ")
                .concat(USD.getCharCode())
                .concat(": ")
                .concat(Integer.toString(USD.getNominal()))
                .concat(" за ")
                .concat(Double.toString(USD.getValue()));
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeView();
        HttpConnector connector = new HttpConnector(this);
        try {
            time = System.currentTimeMillis();
            connector.get(new URL("https://www.cbr-xml-daily.ru/daily_json.js"), 1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRespond(String result, int requestCode) {
        refreshTitleOnUI(result);
    }

    @Override
    public void onError(String error, int requestCode) {

    }

    private void refreshTitleOnUI(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, Long.toString(System.currentTimeMillis() - time), Toast.LENGTH_SHORT).show();
                title.setText(parseResponce(text));
            }
        });
    }

    private void initializeView() {
        title = findViewById(R.id.textView);
    }
}