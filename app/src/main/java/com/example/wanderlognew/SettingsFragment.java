package com.example.wanderlognew;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

public class SettingsFragment extends Fragment {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_LANG   = "selected_language";
    private Spinner languageSpinner;
    private Button  changePasswordBtn, deleteAccountBtn, logoutBtn;
    private boolean hasShownTranslatedToast = false; //prevents repeated popup after translation


    public SettingsFragment() {}

    // retrieve username of the currently logged-in user from SharedPreferences
    private String getCurrentLoggedInUsername() {
        return requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("logged_in_user", null);
    }

    // Load view and restrict screen to portrait mode
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        ensureTranslationThenDisplay(v); // defer UI setup until translations are ready
        return v;
    }

    // Checks if translation already exists or triggers new translation
    private void ensureTranslationThenDisplay(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int savedIndex = prefs.getInt("language_index", 0);
        String[] langCodes = {"EN", "ES", "IT"};
        String langCode = langCodes[savedIndex];
        String file = "settings_fragment_strings.json";

        if (TranslatedStore.get(file) != null) {
            displayUI(view);
        } else {
            JsonTranslator.translateJsonFile(requireContext(), file, langCode, new JsonTranslator.TranslationListener() {
                @Override public void onComplete(JSONObject t) {
                    TranslatedStore.put(file, t);
                    if (isAdded()) requireActivity().runOnUiThread(() -> displayUI(view));
                }

                @Override public void onError(Exception e) {
                    if (isAdded()) requireActivity().runOnUiThread(() -> displayUI(view));
                }
            });
        }
    }

    // Once translation is ready, build and populate UI
    private void displayUI(View v) {
        JSONObject tr = TranslatedStore.get("settings_fragment_strings.json");
        JsonStringLoader fallback = (tr == null)
                ? new JsonStringLoader(requireContext(), "settings_fragment_strings.json") : null;

        // Get translated strings or fallback
        java.util.function.Function<String,String> get =
                key -> tr != null ? tr.optString(key, key) : fallback.getString(key);

        // UI references
        TextView title               = v.findViewById(R.id.settings_title);
        TextView appSettingsLabel    = v.findViewById(R.id.settings_app_label);
        TextView languageLabel       = v.findViewById(R.id.settings_language_label);
        TextView accountSettingsLabel= v.findViewById(R.id.settings_account_label);
        Spinner languageSpinner      = v.findViewById(R.id.spinner_language);
        Button changePasswordBtn     = v.findViewById(R.id.btn_change_password);
        Button deleteAccountBtn      = v.findViewById(R.id.btn_delete_account);
        Button logoutBtn             = v.findViewById(R.id.btn_logout);

        // Text
        title.setText               (get.apply("settings_title"));
        appSettingsLabel.setText    (get.apply("app_settings"));
        languageLabel.setText       (get.apply("language"));
        accountSettingsLabel.setText(get.apply("account_settings"));
        changePasswordBtn.setText   (get.apply("change_pass_btn"));
        deleteAccountBtn.setText    (get.apply("delete_acc_btn"));
        logoutBtn.setText           (get.apply("log_out_btn"));

        // Poplate Language Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.language_options,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int savedIndex = prefs.getInt("language_index", 0);
        languageSpinner.setSelection(savedIndex);

        String[] langCodes = {"EN", "ES", "IT"};

        // Handle language selection
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (first) { first = false; return; } // skip initial auto-selection

                prefs.edit().putInt("language_index", pos).apply();
                String code = langCodes[pos];
                translateAllJsons(code); // begin translation of all relevant JSONs
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // Log out confirmation
        logoutBtn.setOnClickListener(vv -> new AlertDialog.Builder(requireContext())
                .setTitle(get.apply("log_out_btn"))
                .setMessage(get.apply("confirm_logout"))
                .setPositiveButton("Yes", (d, w) -> {
                    startActivity(new Intent(requireContext(), LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                })
                .setNegativeButton("Cancel", null)
                .show());

        // Delete account confirmation
        deleteAccountBtn.setOnClickListener(vv -> new AlertDialog.Builder(requireContext())
                .setTitle(get.apply("delete_acc_btn"))
                .setMessage(get.apply("confirm_delete"))
                .setPositiveButton("Yes", (d, w) -> {
                    new DatabaseHelper(requireContext()).deleteUser(getCurrentLoggedInUsername());
                    startActivity(new Intent(requireContext(), LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                })
                .setNegativeButton("Cancel", null)
                .show());

        // Open password change dialog
        changePasswordBtn.setOnClickListener(vv -> showChangePasswordDialog());
    }

    // Builds and validates the password change dialog
    private void showChangePasswordDialog() {
        View dView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_change_password, null);

        EditText cur = dView.findViewById(R.id.current_password);
        EditText neu = dView.findViewById(R.id.new_password);
        EditText con = dView.findViewById(R.id.confirm_password);

        AlertDialog dlg = new AlertDialog.Builder(requireContext())
                .setTitle("Change Password")
                .setView(dView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dlg.setOnShowListener(di -> {
            Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
            ok.setOnClickListener(v -> {
                String c = cur.getText().toString().trim();
                String n = neu.getText().toString().trim();
                String f = con.getText().toString().trim();
                String pat = "^(?=.*[A-Z])(?=.*\\d).{5,}$"; // password pattern

                if (c.isEmpty()) { cur.setError("Required"); return; }
                if (n.isEmpty() || !n.matches(pat)) {
                    neu.setError("Min5,1 upper,1 num"); return; }
                if (n.equals(c)) { neu.setError("Same as old"); return; }
                if (!n.equals(f)) { con.setError("Mismatch"); return; }

                Toast.makeText(requireContext(),"Password updated",Toast.LENGTH_SHORT).show();
                dlg.dismiss();
            });
        });
        dlg.show();
    }

    // Starts translating all required JSON files, avoiding re-translation if not needed
    private void translateAllJsons(String lang) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String lastLang = prefs.getString("last_translated_lang", "EN");

        // Only translate if language has changed
        if (lang.equalsIgnoreCase(lastLang)) return; // Avoid re-translating

        prefs.edit().putString("last_translated_lang", lang).apply(); // update saved lang

        String[] files = {
                "settings_fragment_strings.json",
                "bottom_nav_strings.json",
                "home_fragment_strings.json",
                "login_strings.json",
                "signup_strings.json",
                "add_fragment_strings.json",
                "journal_details_strings.json"
        };
        translateChain(files, 0, lang);
    }


    // Recursively translated each file one-by-one
    private void translateChain(String[] files, int idx, String lang) {
        if (idx == files.length) {
            if (!hasShownTranslatedToast) {
                hasShownTranslatedToast = true;
                Toast.makeText(requireContext(), "Translated!", Toast.LENGTH_SHORT).show();
            }
            requireActivity().recreate(); // Refresh UI with new strings
            return;
        }

        JsonTranslator.translateJsonFile(requireContext(), files[idx], lang,
                new JsonTranslator.TranslationListener() {
                    @Override public void onComplete(JSONObject t) {
                        TranslatedStore.put(files[idx], t);
                        if (isAdded()) requireActivity().runOnUiThread(() ->
                                translateChain(files, idx + 1, lang));
                    }

                    @Override public void onError(Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(),
                                        "Fail: " + files[idx], Toast.LENGTH_SHORT).show());
                    }
                });
    }

}
