package com.example.incarica_si_o_no;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView tvChargingStatus;
    private long lastRequestTime = 0;  // Aggiungi questa variabile per tenere traccia dell'ultimo momento di invio della richiesta

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Creare un TextView programmabilmente
        tvChargingStatus = new TextView(this);
        tvChargingStatus.setTextSize(16);
        tvChargingStatus.setTextColor(Color.BLACK);

        // Creare un RelativeLayout programmabilmente
        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.WHITE);

        // Creare LayoutParams per posizionare la TextView al centro
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Aggiungere la TextView al RelativeLayout
        layout.addView(tvChargingStatus, params);

        // Impostare il RelativeLayout come contenuto della vista dell'activity
        setContentView(layout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Registrare il BroadcastReceiver per ricevere gli aggiornamenti dello stato di carica
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryStatusReceiver, ifilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Annullare la registrazione del BroadcastReceiver quando l'activity non è più visibile
        unregisterReceiver(batteryStatusReceiver);
    }

    // Creare un BroadcastReceiver per monitorare lo stato di carica
    private final BroadcastReceiver batteryStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime >= 60000) {  // controlla se è passato almeno un minuto dall'ultima richiesta
                if (isCharging) {
                    tvChargingStatus.setText("Il telefono è in carica");
                    sendPostRequest("hey");
                } else {
                    tvChargingStatus.setText("Il telefono non è in carica");
                    sendPostRequest("hey");
                }
                lastRequestTime = currentTime;  // aggiorna l'ultimo momento di invio della richiesta
            }
        }
    };

    private void sendPostRequest(final String message) {
        // URL del webhook Zapier
        final String zapierWebhookUrl = "https://hooks.zapier.com/hooks/catch/16772660/3sq6dqh/";

        // Utilizzare un thread separato per eseguire la richiesta HTTP perché Android non consente operazioni di rete nel thread principale
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Creare un oggetto URL
                    URL url = new URL(zapierWebhookUrl);

                    // Creare un oggetto HttpURLConnection
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                    // Impostare il metodo di richiesta a POST
                    httpURLConnection.setRequestMethod("POST");

                    // Abilitare l'input/output
                    httpURLConnection.setDoOutput(true);

                    // Creare un OutputStream per inviare dati
                    OutputStream os = httpURLConnection.getOutputStream();

                    // Creare un BufferedWriter per scrivere i dati nel flusso di output
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                    // Creare un payload JSON con il messaggio da inviare a Zapier
                    String jsonPayload = "{\"message\":\"" + message + "\"}";

                    // Scrivere il payload nel flusso di output
                    writer.write(jsonPayload);

                    // Pulire e chiudere i flussi
                    writer.flush();
                    writer.close();
                    os.close();

                    // Ottenere il codice di risposta
                    final int responseCode = httpURLConnection.getResponseCode();

                    // Loggare il codice di risposta (opzionale)
                    Log.i("Zapier", "POST Response Code :: " + responseCode);

                    // Se la richiesta è stata elaborata correttamente, aggiornare la TextView con il codice di risposta
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvChargingStatus.append("\nRisposta HTTP: " + responseCode);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvChargingStatus.append("\nErrore: " + responseCode);
                            }
                        });
                    }

                    // Chiudere la connessione
                    httpURLConnection.disconnect();

                } catch (Exception e) {
                    // Gestire eccezioni (ad esempio, loggare l'errore)
                    e.printStackTrace();
                }
            }
        });

        // Avviare il thread
        thread.start();
    }
}
