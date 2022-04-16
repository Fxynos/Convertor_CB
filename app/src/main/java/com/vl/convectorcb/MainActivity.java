package com.vl.convectorcb;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements HttpConnector.HttpListener, RecyclerAdapter.OnItemClickListener, View.OnClickListener, TextWatcher, View.OnKeyListener {
    public static final long REFRESH_COOLDOWN = 8 * 3600 * 1000;
    RecyclerView recyclerView;
    RecyclerAdapter adapter;
    TextView lastRefreshTime, firstValuteTitle, secondValuteTitle, secondValuteField;
    ImageButton refreshButton, swapButton;
    ArrayList<Valute> valutes = new ArrayList<>();
    SharedPreferences preferences;
    HttpConnector connector;
    ObjectAnimator refreshAnimator;
    EditText firstValuteField;

    int[] convertingValutesPos = new int[]{0, 11};
    boolean convertingValutesChoosed = false;

    static private String parseTime(long millis) {
        if (millis > 0) {
            Calendar data = Calendar.getInstance();
            data.setTimeInMillis(millis);
            return String.format("%02d:%02d", data.get(Calendar.HOUR_OF_DAY), data.get(Calendar.MINUTE));
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
                valutes.add(0, Valute.getRuble());
                adapter.notifyDataSetChanged();
                if (!convertingValutesChoosed) {
                    refreshConvertingValutes();
                    convertingValutesChoosed = true;
                }
            }
        }
        refreshButton.setOnClickListener(this);
        swapButton.setOnClickListener(this);
        firstValuteField.addTextChangedListener(this);
        firstValuteField.setOnKeyListener(this);
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refreshImageButton:
                if (!connector.isWorking()) refresh();
            break;
            case R.id.swapImageButton:
                swapConvertorValutes();
                break;
        }
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        refreshConvertingValue();
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }

    @Override
    public void onItemClick(int pos) {
        refreshConvertingValutes(pos, convertingValutesPos[1]);
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
        valutes.add(0, Valute.getRuble());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lastRefreshTime.setText(parseTime(lastRespondTime));
                adapter.notifyDataSetChanged();
                if (!convertingValutesChoosed) {
                    refreshConvertingValutes();
                    convertingValutesChoosed = true;
                }
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

    private void refreshConvertingValue() {
        final String inputValue = firstValuteField.getText().toString().trim().replace(",", ".");
        String result = "";
        try {
            Double input;
            if (inputValue.length() > 0 && (input = Double.parseDouble(inputValue)) >= 0)
                result = String.format("%.3f", convert(valutes.get(convertingValutesPos[0]), valutes.get(convertingValutesPos[1]), input));
        } catch (NumberFormatException exception) {
            secondValuteField.setText("");
        }
        secondValuteField.setText(result);
    }

    private double convert(Valute from, Valute to, double count) {
        return count * ((from.getValue() / from.getNominal()) / (to.getValue() / to.getNominal()));
    }

    private void refreshConvertingValutes(int firstValutePos, int secondValutePos) {
        convertingValutesPos[0] = firstValutePos;
        convertingValutesPos[1] = secondValutePos;
        refreshConvertingValutes();
    }

    private void refreshConvertingValutes() {
        final int
                fPos = convertingValutesPos[0],
                sPos = convertingValutesPos[1];
        firstValuteTitle.setText(valutes.get(fPos).getCharCode());
        secondValuteTitle.setText(valutes.get(sPos).getCharCode());
        refreshConvertingValue();
    }

    private void swapConvertorValutes() {
        startRotationAnimation(swapButton, 0.5f);
        refreshConvertingValutes(convertingValutesPos[1], convertingValutesPos[0]);
    }

    private void startRotationAnimation(View view, float rounds) {
        final int duration = 500;
        refreshAnimator = ObjectAnimator.ofFloat(view, "rotation", 0, rounds * 360).setDuration(duration);
        refreshAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        refreshAnimator.start();
    }

    private void refresh() {
        startRotationAnimation(refreshButton, 1);
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
        swapButton = findViewById(R.id.swapImageButton);
        firstValuteField = findViewById(R.id.firstValuteEditText);
        secondValuteField = findViewById(R.id.secondValuteText);
        firstValuteTitle = findViewById(R.id.firstValuteTitle);
        secondValuteTitle = findViewById(R.id.secondValuteTitle);
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
        if (list.get(pos).getID().equals(Valute.RUB_ID)) {
            holder.arrow.setVisibility(View.GONE);
            holder.previous.setVisibility(View.GONE);
        } else {
            holder.arrow.setVisibility(View.VISIBLE);
            holder.previous.setVisibility(View.VISIBLE);
            holder.previous.setText(String.format("%.2f", list.get(pos).getPrevious() / list.get(pos).getNominal()));
        }
        holder.name.setText(list.get(pos).getName());
        holder.charCode.setText(list.get(pos).getCharCode());
        holder.value.setText(String.format("%.2f", list.get(pos).getValue() / list.get(pos).getNominal()));
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name, value, previous, charCode;
        ImageView arrow;

        public ViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            name = view.findViewById(R.id.valuteNameTextView);
            value = view.findViewById(R.id.valueTextView);
            previous = view.findViewById(R.id.previousValueTextView);
            charCode = view.findViewById(R.id.charCodeTextView);
            arrow = view.findViewById(R.id.changeArrow);
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