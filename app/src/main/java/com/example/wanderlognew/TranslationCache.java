package com.example.wanderlognew;
import android.content.Context;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Handles saving and loading translated JSON objects to internal storage.
 * This allows offline access to translations after the first use.
 */
public class TranslationCache {
    // Saves a translated JSONObject to internal storage with the given filename
    public static void save(Context context, String filename, JSONObject json) {
        try {
            File file = new File(context.getFilesDir(), filename); // Target file path
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.toString().getBytes()); // Write JSON as string
            fos.close();
        } catch (Exception e) {
            e.printStackTrace(); // Log error is saving fails
        }
    }

    // Loads a previously cached translated JSON file from internal storage
    public static JSONObject load(Context context, String filename){
        try{
            File file = new File(context.getFilesDir(), filename); // Check for file
            if(!file.exists()) return null;

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer); // Read file content into byte array
            fis.close();

            String json = new String(buffer); // Convert byte to string
            return new JSONObject(json); // Parse json string
        }catch (Exception e){
            e.printStackTrace(); // Log error if loading fails
            return null;
        }
    }

}
