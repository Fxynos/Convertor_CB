package com.vl.convectorcb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements HttpConnector.HttpListener, RecyclerAdapter.OnItemClickListener, View.OnClickListener {
    public static final long REFRESH_COOLDOWN = 8 * 3600 * 1000;
    RecyclerView recyclerView;
    RecyclerAdapter adapter;
    TextView lastRefreshTime;
    ImageButton refreshButton;
    ArrayList<Valute> valutes = new ArrayList<>();
    SharedPreferences preferences;
    HttpConnector connector;
    ObjectAnimator refreshAnimator;

    static private String parseTime(long millis) {
        if (millis > 0) {
            Calendar data = Calendar.getInstance();
            data.setTimeInMillis(millis);
            return String.format("%02d:%02d", data.get(Calendar.HOUR), data.get(Calendar.MINUTE));
        } else return "";
    }

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
        preferences = getSharedPreferences(getString(R.string.preferencesFile), Context.MODE_PRIVATE);
        long lastRespondTime = preferences.getLong(getString(R.string.preferencesLastRefreshTime), -REFRESH_COOLDOWN);
        lastRefreshTime.setText(parseTime(lastRespondTime));
        adapter = new RecyclerAdapter(this, valutes, this);
        recyclerView.setAdapter(adapter);
        connector = new HttpConnector(this);
        if (Calendar.getInstance().getTimeInMillis() - lastRespondTime >= REFRESH_COOLDOWN) {
            refresh();
        } else {
            String lastRespond = preferences.getString(getString(R.string.preferencesLastJSONRespond), "");
            if (lastRespond.length() > 0) {
                valutes.clear();
                valutes.addAll(parseResponce(lastRespond));
                adapter.notifyDataSetChanged();
            }
        }
        refreshButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.refreshImageButton && !connector.isWorking()) refresh();
    }

    @Override
    public void onItemClick(int pos) {
        Toast.makeText(this, valutes.get(pos).getName().concat(" clicked"), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRespond(String result, int requestCode) {
        long lastRespondTime = Calendar.getInstance().getTimeInMillis();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(getString(R.string.preferencesLastJSONRespond), result);
        editor.putLong(getString(R.string.preferencesLastRefreshTime), lastRespondTime);
        editor.apply();
        valutes.clear();
        valutes.addAll(parseResponce(result));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastRefreshTime.setText(parseTime(lastRespondTime));
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

    private void startRefreshAnimation() {
        final int duration = 500;
        refreshAnimator = ObjectAnimator.ofFloat(refreshButton, "rotation", 0, 360).setDuration(duration);
        refreshAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        refreshAnimator.start();
    }

    private void refresh() {
        startRefreshAnimation();
        try {
            connector.get(new URL("https://www.cbr-xml-daily.ru/daily_json.js"), 1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void initializeView() {
        recyclerView = findViewById(R.id.valuteListRecycler);
        lastRefreshTime = findViewById(R.id.lastRefreshTimeTextView);
        refreshButton = findViewById(R.id.refreshImageButton);
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