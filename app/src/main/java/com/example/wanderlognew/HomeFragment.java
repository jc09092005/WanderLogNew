package com.example.wanderlognew;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.List;

public class HomeFragment extends Fragment {

    // UI components
    private RecyclerView recyclerView;
    private TextView emptyMessage;
    private TextView headerTitle;
    private JournalEntryAdapter adapter;
    private Button addEntryButton;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Lock screen orientation to portrait
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialise views
        headerTitle = view.findViewById(R.id.header_title);
        emptyMessage = view.findViewById(R.id.empty_message);
        recyclerView = view.findViewById(R.id.recyclerView);
        addEntryButton = view.findViewById(R.id.btn_add_entry);

        // Load user language setting
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int savedIndex = prefs.getInt("language_index", 0);
        String[] langCodes = {"EN", "ES", "IT"};
        String langCode = langCodes[savedIndex];

        // Ensure translations are loaded
        String[] filesToTranslate = {
                "home_fragment_strings.json"
        };

        translateJsonsThenDisplay(requireContext(), filesToTranslate, 0, langCode);

        return view;
    }

    // Recursively loads files, then calls displayContent() once done
    private void translateJsonsThenDisplay(Context context, String[] files, int index, String lang) {
        // all files have been translated
        if (index >= files.length) {
            displayContent(); // safe to update UI
            return;
        }

        String file = files[index];

        // Use cached version if available
        if (TranslatedStore.get(file) != null) {
            translateJsonsThenDisplay(context, files, index + 1, lang);
        } else {
            // Otherwise, translate the file now
            JsonTranslator.translateJsonFile(context, file, lang, new JsonTranslator.TranslationListener() {
                @Override public void onComplete(JSONObject translatedJson) {
                    TranslatedStore.put(file, translatedJson);
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            translateJsonsThenDisplay(context, files, index + 1, lang));
                }

                @Override public void onError(Exception e) {
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Toast.makeText(context, "Translation failed", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    // Updates UI content once translations are available. Loads user journal entries from database and displays them in a grid layout
    private void displayContent() {
        // Load translated strings or fallback from assets
        JSONObject updated = TranslatedStore.get("home_fragment_strings.json");
        JsonStringLoader fallback = new JsonStringLoader(requireContext(), "home_fragment_strings.json");

        // Get translated or fallback UI strings
        String title = updated != null ? updated.optString("home_title", "Your Journal Entries") : fallback.getString("home_title");
        String empty = updated != null ? updated.optString("home_default", "No Journal Entries available") : fallback.getString("home_default");

        headerTitle.setText(title);
        emptyMessage.setText(empty);

        // Load journal entries from database for the logged-in user
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = prefs.getString("logged_in_user", null);
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        List<JournalEntry> dbEntries = dbHelper.getEntriesForUser(username);

        // Show or hide views depending on whether entries exist
        if (dbEntries.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyMessage.setVisibility(View.VISIBLE);
            addEntryButton.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyMessage.setVisibility(View.GONE);
            addEntryButton.setVisibility(View.GONE);
        }

        // Set up RecyclerView with a grid layout of 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new JournalEntryAdapter(getContext(), dbEntries);
        recyclerView.setAdapter(adapter);

        // When "Add Entry" button is clicked, open AddFragments
        addEntryButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, new AddFragment())
                        .addToBackStack(null)
                        .commit()
        );
    }

}
