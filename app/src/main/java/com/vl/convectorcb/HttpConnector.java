package com.vl.convectorcb;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpConnector {
    private HttpListener listener;
    boolean working = false;

    public HttpConnector(HttpListener listener) {
        this.listener = listener;
    }

    public void get(URL url, int requestCode) {
        working = true;
        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    String result = "";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while((line = reader.readLine()) != null) result = result.concat(line);
                    listener.onRespond(result, requestCode);
                    connection.disconnect();
                } catch (IOException exception) {
                    if (listener != null) listener.onError(exception.getMessage(), requestCode);
                } finally {
                    working = false;
                }
            }
        });
        networkThread.setName("networkThread");
        networkThread.start();
    }

    public boolean isWorking() {
        return working;
    }

    interface HttpListener {
        void onRespond(String result, int requestCode);
        void onError(String error, int requestCode);
    }
}
