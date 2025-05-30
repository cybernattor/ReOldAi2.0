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
import android.widget.Toast;

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


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ai_models_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        aiModelSpinner.setAdapter(adapter);

        loadSettings();
        populateAboutSection();

        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        apiKeyEditText.setText(prefs.getString(API_KEY_PREF, ""));

        String savedModel = prefs.getString(AI_MODEL_PREF, "Gemini 1.5 Flash");
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) aiModelSpinner.getAdapter();
        int spinnerPosition = adapter.getPosition(savedModel);
        aiModelSpinner.setSelection(spinnerPosition);

        temperatureEditText.setText(String.valueOf(prefs.getFloat(TEMPERATURE_PREF, 0.9f)));
        topKEditText.setText(String.valueOf(prefs.getInt(TOP_K_PREF, 40)));
        topPEditText.setText(String.valueOf(prefs.getFloat(TOP_P_PREF, 0.9f)));
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(API_KEY_PREF, apiKeyEditText.getText().toString().trim());
        editor.putString(AI_MODEL_PREF, aiModelSpinner.getSelectedItem().toString());

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
}