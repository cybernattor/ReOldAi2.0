package com.ymp.reold.ai;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AIPrefs";
    private static final String API_KEY_PREF = "ai_api_key";
    private static final String AI_MODEL_PREF = "ai_model";
    private static final String TEMPERATURE_PREF = "temperature";
    private static final String TOP_K_PREF = "top_k";
    private static final String TOP_P_PREF = "top_p";

    private EditText apiKeyEditText;
    private Spinner aiModelSpinner;
    private EditText temperatureEditText;
    private EditText topKEditText;
    private EditText topPEditText;
    private Button saveSettingsButton;

    private TextView appNameTextView;
    private TextView appVersionTextView;
    private TextView librariesTextView;
    private TextView geminiInfoTextView;

    private ArrayList<String> modelDisplayNames = new ArrayList<>();
    private ArrayList<String> modelNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Активити настроек. Здесь хранятся настройки нейросети, приложения и др.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        apiKeyEditText = (EditText) findViewById(R.id.edit_text_api_key);
        aiModelSpinner = (Spinner) findViewById(R.id.spinner_ai_model);
        temperatureEditText = (EditText) findViewById(R.id.edit_text_temperature);
        topKEditText = (EditText) findViewById(R.id.edit_text_top_k);
        topPEditText = (EditText) findViewById(R.id.edit_text_top_p);
        saveSettingsButton = (Button) findViewById(R.id.button_save_settings);

        appNameTextView = (TextView) findViewById(R.id.text_view_app_name);
        appVersionTextView = (TextView) findViewById(R.id.text_view_app_version);
        librariesTextView = (TextView) findViewById(R.id.text_view_libraries);
        geminiInfoTextView = (TextView) findViewById(R.id.text_view_gemini_info);


        loadSettings();
        populateAboutSection();
        fetchAiModels();

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void fetchAiModels() {
        final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        final String apiKey = prefs.getString(API_KEY_PREF, "");

        if (apiKey.isEmpty()) {
            Toast.makeText(this, "API key is not set", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        StringBuilder stringBuilder = new StringBuilder();
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            stringBuilder.append(line).append("\n");
                        }
                        bufferedReader.close();
                        String jsonResponse = stringBuilder.toString();

                        modelDisplayNames.clear();
                        modelNames.clear();

                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray modelsArray = jsonObject.getJSONArray("models");
                        for (int i = 0; i < modelsArray.length(); i++) {
                            JSONObject modelObject = modelsArray.getJSONObject(i);
                            String displayName = modelObject.getString("displayName");
                            String modelName = modelObject.getString("name");
                            if (modelName.startsWith("models/")) {
                                modelName = modelName.substring("models/".length());
                            }
                            modelDisplayNames.add(displayName);
                            modelNames.add(modelName);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(SettingsActivity.this, android.R.layout.simple_spinner_item, modelDisplayNames);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                aiModelSpinner.setAdapter(adapter);
                                loadSettings(); // Reload settings to select the correct model
                            }
                        });
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SettingsActivity.this, "Failed to fetch AI models", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        apiKeyEditText.setText(prefs.getString(API_KEY_PREF, ""));

        String savedModel = prefs.getString(AI_MODEL_PREF, "");
        if (!savedModel.isEmpty() && !modelNames.isEmpty()) {
            int modelIndex = modelNames.indexOf(savedModel);
            if (modelIndex != -1) {
                aiModelSpinner.setSelection(modelIndex);
            }
        }

        temperatureEditText.setText(String.valueOf(prefs.getFloat(TEMPERATURE_PREF, 0.9f)));
        topKEditText.setText(String.valueOf(prefs.getInt(TOP_K_PREF, 40)));
        topPEditText.setText(String.valueOf(prefs.getFloat(TOP_P_PREF, 0.9f)));
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(API_KEY_PREF, apiKeyEditText.getText().toString().trim());

        int selectedModelIndex = aiModelSpinner.getSelectedItemPosition();
        if (selectedModelIndex != -1 && selectedModelIndex < modelNames.size()) {
            String selectedModelName = modelNames.get(selectedModelIndex);
            editor.putString(AI_MODEL_PREF, selectedModelName);
        }

        try {
            float temp = Float.parseFloat(temperatureEditText.getText().toString());
            editor.putFloat(TEMPERATURE_PREF, temp);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_temperature_format_toast, Toast.LENGTH_SHORT).show();
            editor.putFloat(TEMPERATURE_PREF, 0.9f);
        }

        try {
            int topK = Integer.parseInt(topKEditText.getText().toString());
            editor.putInt(TOP_K_PREF, topK);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_top_k_format_toast, Toast.LENGTH_SHORT).show();
            editor.putInt(TOP_K_PREF, 40);
        }

        try {
            float topP = Float.parseFloat(topPEditText.getText().toString());
            editor.putFloat(TOP_P_PREF, topP);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_top_p_format_toast, Toast.LENGTH_SHORT).show();
            editor.putFloat(TOP_P_PREF, 0.9f);
        }

        editor.commit();
        Toast.makeText(this, R.string.settings_saved_toast, Toast.LENGTH_SHORT).show();
    }

    private void populateAboutSection() {
        appNameTextView.setText(getString(R.string.app_name));
        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                PackageInfo pInfo = packageManager.getPackageInfo(getPackageName(), 0);
                appVersionTextView.setText(getString(R.string.app_version_label) + " " + pInfo.versionName);
            } else {
                appVersionTextView.setText(getString(R.string.app_version_label) + " N/A");
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appVersionTextView.setText(getString(R.string.app_version_label) + " N/A");
        }
        librariesTextView.setText(getString(R.string.libraries_list));
        geminiInfoTextView.setText(getString(R.string.gemini_api_info));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}