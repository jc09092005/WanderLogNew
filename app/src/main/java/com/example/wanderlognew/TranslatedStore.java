package com.example.wanderlognew;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple store for translated JSON objects in memory.
 */
public class TranslatedStore {

    // Static map to hold filename to translation mappings
    private static final Map<String, JSONObject> store = new HashMap<>();

    // Saves the translated JSON data to a specific file name
    public static void put(String filename, JSONObject data) {
        store.put(filename, data);
    }

    // Retrieves the cached translated JSON data for a specific file
    public static JSONObject get(String filename) {
        return store.get(filename);
    }
}
