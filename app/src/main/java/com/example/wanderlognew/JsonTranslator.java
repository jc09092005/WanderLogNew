package com.example.wanderlognew;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Translates a single JSON file from English to the target language using DeepL's API.
 * Translations are cached locally after being fetched.
 */
public class JsonTranslator {

    // DeepL API constants
    private static final String DEEPL_URL = "https://api-free.deepl.com/v2/translate";
    private static final String DEEPL_API_KEY = BuildConfig.DEEPL_API_KEY; // Secure API key from BuildConfig
    private static final OkHttpClient client = new OkHttpClient(); // Reusable HTTP client

    // Listener interface to handle success or failure callbacks
    public interface TranslationListener {
        void onComplete(JSONObject translatedJson);
        void onError(Exception e);
    }

    // Loads the local JSON (English) from assets folder
    private static JSONObject loadEnglishJson(Context context, String filename) {
        try {
            InputStream is = context.getAssets().open(filename); // Open asset file
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer); // Read file content
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            return new JSONObject(jsonStr); // Convert to JSONobject
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Translates a given JSON file from English to a target language.
     * Uses a cache to avoid re-translating files.
     */
    public static void translateJsonFile(Context context,
                                         String filename,
                                         String targetLang,
                                         TranslationListener listener) {
        String cacheKey = targetLang.toLowerCase() + "_" + filename;

        // Check if translation already exists in cache
        JSONObject cached = TranslationCache.load(context, cacheKey);
        if(cached != null){
            listener.onComplete(cached);
            return;
        }

        // Load default English from assets
        JSONObject originalJson = loadEnglishJson(context, filename);
        if (originalJson == null) {
            listener.onError(new Exception("Failed to load file: " + filename));
            return;
        }

        // Prepare keys to translate
        List<String> keys = new ArrayList<>();
        Iterator<String> iter = originalJson.keys();
        while (iter.hasNext()) {
            keys.add(iter.next());
        }
        // Start chain, and pass cacheKey to save later
        translateNextKey(context, originalJson, keys, 0, targetLang, cacheKey, listener);
    }

    // Recursively translates each key's value using the DeepL API and updates the JSON object.
    private static void translateNextKey(Context context, JSONObject jsonObj, List<String> keys, int index, String targetLang, String cacheKey, TranslationListener listener) {
        // Base case: all keys translated
        if (index >= keys.size()) {
            // save to cache
            TranslationCache.save(context, cacheKey, jsonObj);
            listener.onComplete(jsonObj);
            return;
        }

        String key = keys.get(index);
        String enText = jsonObj.optString(key, "");

        // Build form data for DeepL
        RequestBody formBody = new FormBody.Builder()
                .add("text", enText)
                .add("source_lang", "EN")
                .add("target_lang", targetLang)
                .build();

        // Build HTTP request with API key
        Request request = new Request.Builder()
                .url(DEEPL_URL)
                .addHeader("Authorization", "DeepL-Auth-Key " + DEEPL_API_KEY)
                .post(formBody)
                .build();

        // Execute API request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                listener.onError(e); // Network or server error
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("DeepL", "Response code: " + response.code()
                            + ", message: " + response.message()
                            + ", body: " + response.body().string());
                    listener.onError(new IOException("Unexpected code " + response));
                    return;

                }

                // Parse translation result
                String respBody = response.body().string();
                try {
                    JSONObject respJson = new JSONObject(respBody);
                    JSONArray translations = respJson.getJSONArray("translations");
                    String newText = translations.getJSONObject(0).getString("text");
                    // Insert translated text into JSON
                    jsonObj.put(key, newText);

                    // Next key
                    translateNextKey(context, jsonObj, keys, index + 1, targetLang, cacheKey, listener);
                } catch (JSONException e) {
                    listener.onError(e);
                }
            }
        });
    }
}
