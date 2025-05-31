package com.ymp.reold.ai;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    //Наше основное активити.
    private static final String TAG = "MainActivity";
    private static final int SPEECH_INPUT_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "AIPrefs";
    private static final String API_KEY_PREF = "ai_api_key";
    private static final String AI_MODEL_PREF = "ai_model";
    private static final String TEMPERATURE_PREF = "temperature";
    private static final String TOP_K_PREF = "top_k";
    private static final String TOP_P_PREF = "top_p";

    private EditText promptEditText;
    private ImageButton sendButton;
    private ImageButton micButton;
    private ListView chatListView;
    private ChatMessageAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;

    private String aiApiKey;
    private String selectedAiModel;
    private float temperature;
    private int topK;
    private float topP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        promptEditText = (EditText) findViewById(R.id.prompt_edit_text);
        sendButton = (ImageButton) findViewById(R.id.send_button);
        micButton = (ImageButton) findViewById(R.id.mic_button);
        chatListView = (ListView) findViewById(R.id.chat_list_view);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatMessageAdapter(this, chatMessages);
        chatListView.setAdapter(chatAdapter);

        loadSettings();

        if (TextUtils.isEmpty(aiApiKey)) {
            showApiKeyDialog(true);
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceInput();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        aiApiKey = prefs.getString(API_KEY_PREF, null);
        selectedAiModel = prefs.getString(AI_MODEL_PREF, "Gemini 1.5 Flash");
        temperature = prefs.getFloat(TEMPERATURE_PREF, 0.9f);
        topK = prefs.getInt(TOP_K_PREF, 40);
        topP = prefs.getFloat(TOP_P_PREF, 0.9f);
    }

    private void showApiKeyDialog(final boolean isFirstLaunch) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isFirstLaunch ? "Enter API key" : "Enter API key");

        final EditText input = new EditText(this);
        input.setHint("API key");
        if (!TextUtils.isEmpty(aiApiKey) && !isFirstLaunch) {
            input.setText(aiApiKey);
        }
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = input.getText().toString().trim();
                if (!TextUtils.isEmpty(key)) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(API_KEY_PREF, key);
                    editor.commit();
                    aiApiKey = key;
                    Toast.makeText(MainActivity.this, "API key saved.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "The API key cannot be empty.", Toast.LENGTH_LONG).show();
                    if (isFirstLaunch) {
                        showApiKeyDialog(true);
                    }
                }
            }
        });

        if (isFirstLaunch) {
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    Toast.makeText(MainActivity.this, "Without the API key, this application will not function properly.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            builder.setNegativeButton("Cancel", null);
        }

        builder.show();
    }

    private void sendMessage() {
        String prompt = promptEditText.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(aiApiKey)) {
            Toast.makeText(this, "Please enter your AI API key in settings.", Toast.LENGTH_LONG).show();
            showApiKeyDialog(false);
            return;
        }

        chatMessages.add(new ChatMessage(prompt, ChatMessage.SenderRole.USER));
        chatAdapter.notifyDataSetChanged();
        promptEditText.setText("");

        new AICallTask().execute(chatMessages);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something...");

        try {
            startActivityForResult(intent, SPEECH_INPUT_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Your device does not support voice input.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_INPUT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    promptEditText.setText(result.get(0));
                    sendMessage();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class ChatMessage {
        public enum SenderRole { USER, MODEL }
        String text;
        SenderRole role;

        ChatMessage(String text, SenderRole role) {
            this.text = text;
            this.role = role;
        }
    }

    private class ChatMessageAdapter extends ArrayAdapter<ChatMessage> {
        public ChatMessageAdapter(Context context, ArrayList<ChatMessage> messages) {
            super(context, 0, messages);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ChatMessage message = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.chat_message_item, parent, false);
            }

            RelativeLayout userMessageLayout = (RelativeLayout) convertView.findViewById(R.id.user_message_layout);
            TextView userMessageTextView = (TextView) convertView.findViewById(R.id.user_message_text_view);
            ImageButton btnCopyUser = (ImageButton) convertView.findViewById(R.id.btn_copy_user);
            ImageButton btnDeleteUser = (ImageButton) convertView.findViewById(R.id.btn_delete_user);

            RelativeLayout aiMessageLayout = (RelativeLayout) convertView.findViewById(R.id.ai_message_layout);
            TextView aiMessageTextView = (TextView) convertView.findViewById(R.id.ai_message_text_view);
            ImageButton btnCopyAi = (ImageButton) convertView.findViewById(R.id.btn_copy_ai);
            ImageButton btnDeleteAi = (ImageButton) convertView.findViewById(R.id.btn_delete_ai);


            if (message.role == ChatMessage.SenderRole.USER) {
                userMessageLayout.setVisibility(View.VISIBLE);
                aiMessageLayout.setVisibility(View.GONE);
                userMessageTextView.setText(message.text);

                btnCopyUser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        copyTextToClipboard(message.text);
                    }
                });
                btnDeleteUser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteMessage(position);
                    }
                });

            } else { // message.role == ChatMessage.SenderRole.MODEL
                userMessageLayout.setVisibility(View.GONE);
                aiMessageLayout.setVisibility(View.VISIBLE);
                aiMessageTextView.setText(message.text);

                btnCopyAi.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        copyTextToClipboard(message.text);
                    }
                });
                btnDeleteAi.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteMessage(position);
                    }
                });
            }

            return convertView;
        }

        private void copyTextToClipboard(String text) {
            // Для Android API 11 (Honeycomb) и выше
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("AI Chat Message", text);
                clipboard.setPrimaryClip(clip);
            }
            // Для Android API 8, 9, 10 (Froyo, Gingerbread ->)
            else {
                //android.text.ClipboardManager
                android.text.ClipboardManager clipboard =
                        (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            }
            Toast.makeText(getContext(), "Copied text.", Toast.LENGTH_SHORT).show();
        }

        private void deleteMessage(int position) {
            chatMessages.remove(position);
            notifyDataSetChanged();
            Toast.makeText(getContext(), "Message deleted.", Toast.LENGTH_SHORT).show();
        }
    }

    private class AICallTask extends AsyncTask<List<ChatMessage>, Void, String> {

        private String currentAiModel;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (selectedAiModel.equals("Gemini 2.0 Flash")) {
                currentAiModel = "gemini-2.0-flash";
            } else {
                currentAiModel = "gemini-1.5-flash";
            }
        }

        @Override
        protected String doInBackground(List<ChatMessage>... chatHistory) {
            if (chatHistory.length == 0 || chatHistory[0].isEmpty() || TextUtils.isEmpty(aiApiKey)) {
                return "Error: Prompt or API key is not specified.";
            }

            List<ChatMessage> history = chatHistory[0];
            String apiUrl = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", currentAiModel, aiApiKey);
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String aiResponse = null;
            Gson gson = new Gson();

            try {
                URL url = new URL(apiUrl);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(15000);

                JsonObject requestBody = new JsonObject();
                JsonArray contentsArray = new JsonArray();

                for (ChatMessage msg : history) {
                    JsonObject contentObj = new JsonObject();
                    contentObj.addProperty("role", msg.role == ChatMessage.SenderRole.USER ? "user" : "model");
                    JsonArray partsArray = new JsonArray();
                    JsonObject partObj = new JsonObject();
                    partObj.addProperty("text", msg.text);
                    partsArray.add(partObj);
                    contentObj.add("parts", partsArray);
                    contentsArray.add(contentObj);
                }
                requestBody.add("contents", contentsArray);

                JsonObject generationConfig = new JsonObject();
                generationConfig.addProperty("temperature", temperature);
                generationConfig.addProperty("topK", topK);
                generationConfig.addProperty("topP", topP);
                requestBody.add("generationConfig", generationConfig);


                String jsonInputString = gson.toJson(requestBody);
                Log.d(TAG, "Request JSON: " + jsonInputString);

                OutputStream os = urlConnection.getOutputStream();
                os.write(jsonInputString.getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "HTTP Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    aiResponse = responseBuilder.toString();
                    Log.d(TAG, "AI Raw Response: " + aiResponse);

                    try {
                        JsonObject jsonResponse = gson.fromJson(aiResponse, JsonObject.class);
                        if (jsonResponse != null && jsonResponse.has("candidates")) {
                            if (jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject().getAsJsonObject("content").has("parts")) {
                                String candidateText = jsonResponse.getAsJsonArray("candidates")
                                        .get(0).getAsJsonObject()
                                        .getAsJsonObject("content")
                                        .getAsJsonArray("parts")
                                        .get(0).getAsJsonObject()
                                        .get("text").getAsString();
                                return candidateText;
                            } else {
                                return "Gemini: The model rejected the response due to unsafe content.";
                            }
                        } else if (jsonResponse != null && jsonResponse.has("error")) {
                            JsonObject error = jsonResponse.getAsJsonObject("error");
                            String errorMessage = error.has("message") ? error.get("message").getAsString() : "Unknown API error";
                            int errorCode = error.has("code") ? error.get("code").getAsInt() : -1;
                            return "Error API (" + errorCode + "): " + errorMessage;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "AI response parsing error: " + e.getMessage());
                        return "AI response parsing error: " + e.getMessage();
                    }

                } else {
                    InputStreamReader errorStreamReader = new InputStreamReader(urlConnection.getErrorStream(), "UTF-8");
                    BufferedReader errorReader = new BufferedReader(errorStreamReader);
                    StringBuilder errorResponseBuilder = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponseBuilder.append(errorLine);
                    }
                    Log.e(TAG, "Gemini API Error Response: " + errorResponseBuilder.toString());
                    return "Error API: " + responseCode + " - " + errorResponseBuilder.toString();
                }

            } catch (Exception e) {
                Log.e(TAG, "Err with Gemini API: " + e.getMessage());
                e.printStackTrace();
                return "Err with Gemini API: " + e.getMessage();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Err close reader: " + e.getMessage());
                    }
                }
            }
            return "Unknown error while receiving a response.";
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            chatMessages.add(new ChatMessage(result, ChatMessage.SenderRole.MODEL));
            chatAdapter.notifyDataSetChanged();
            chatListView.smoothScrollToPosition(chatMessages.size() - 1);
        }
    }
}