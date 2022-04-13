package com.vl.convectorcb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements HttpConnector.HttpListener, RecyclerAdapter.OnItemClickListener {
    RecyclerView recyclerView;
    RecyclerAdapter adapter;
    ArrayList<Valute> valutes = new ArrayList<>();
    long time;

    static private ArrayList<Valute> parseResponce(String jsonString) {
        ArrayList<Valute> result = new ArrayList<>();
        Gson gson = new Gson();
        Type T = new TypeToken<Map<String, Object>>(){}.getType();
        Map map = gson.fromJson(jsonString, T);
        String slaveString = gson.toJson(map.get("Valute"));
        map = gson.fromJson(slaveString, T);
        Set<String> keys = map.keySet();
        for (String key : keys) {
            slaveString = gson.toJson(map.get(key));
            result.add(gson.fromJson(slaveString, Valute.class));
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeView();
        adapter = new RecyclerAdapter(this, valutes, this);
        recyclerView.setAdapter(adapter);
        HttpConnector connector = new HttpConnector(this);
        try {
            time = System.currentTimeMillis();
            connector.get(new URL("https://www.cbr-xml-daily.ru/daily_json.js"), 1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onItemClick(int pos) {
        Toast.makeText(this, valutes.get(pos).getName().concat(" clicked"), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRespond(String result, int requestCode) {
        valutes.clear();
        valutes.addAll(parseResponce(result));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onError(String error, int requestCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initializeView() {
        recyclerView = findViewById(R.id.valuteListRecycler);
    }
}

class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    ArrayList<Valute> list;
    LayoutInflater inflater;
    OnItemClickListener listener;

    public RecyclerAdapter(Context context, ArrayList<Valute> list, OnItemClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.list = list;
        this.listener = listener;
    }

    public int getItemCount() {
        return list.size();
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int c) {
        return new ViewHolder(inflater.inflate(R.layout.item_valute_layout, parent, false));
    }

    public void onBindViewHolder(ViewHolder holder, int pos) {
        holder.name.setText(list.get(pos).getName());
        holder.charCode.setText(list.get(pos).getCharCode());
        holder.previous.setText(String.format("%.2f", list.get(pos).getPrevious() / list.get(pos).getNominal()));
        holder.value.setText(String.format("%.2f", list.get(pos).getValue() / list.get(pos).getNominal()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name, value, previous, charCode;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            name = view.findViewById(R.id.valuteNameTextView);
            value = view.findViewById(R.id.valueTextView);
            previous = view.findViewById(R.id.previousValueTextView);
            charCode = view.findViewById(R.id.charCodeTextView);
        }

        @Override
        public void onClick(View view) {
            if (listener != null) listener.onItemClick(getAdapterPosition());
        }
    }

    interface OnItemClickListener {
        void onItemClick(int pos);
    }
}