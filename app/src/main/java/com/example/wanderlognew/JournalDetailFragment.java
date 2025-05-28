package com.example.wanderlognew;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

public class JournalDetailFragment extends Fragment {

    // Default constructor
    public JournalDetailFragment() {}

    // factory method to create a new instance with a specific journal entry ID
    public static JournalDetailFragment newInstance(int entryId) {
        JournalDetailFragment fragment = new JournalDetailFragment();
        Bundle args = new Bundle();
        args.putInt("entryId", entryId);
        fragment.setArguments(args);
        return fragment;
    }

    //Enable options menu for handling toolbar actions
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);  // Enable options menu for this fragment
    }

    // Inflate the layout and initialise the view
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Lock orientation to portrait
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        View view = inflater.inflate(R.layout.fragment_journal_detail, container, false);

        // Show back arrow in toolbar
        ((AppCompatActivity) requireActivity())
                .getSupportActionBar()
                .setDisplayHomeAsUpEnabled(true);

        // Load translation then display UI content
        ensureTranslationThenDisplay(view);

        return view;
    }

    // Handle toolbar back arrow action
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            requireActivity().getSupportFragmentManager().popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Loads translation strings for the UI, then displays content
    private void ensureTranslationThenDisplay(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int savedIndex = prefs.getInt("language_index", 0);
        String[] langCodes = {"EN", "ES", "IT"};
        String langCode = langCodes[savedIndex];
        String file = "journal_details_strings.json";

        // If already translated, display UI immediately
        if (TranslatedStore.get(file) != null) {
            displayUI(view);
        } else {
            // Otherwise, translate JSON and then display
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

    // Populate the UI with journal entry data and translated labels
    private void displayUI(View view) {
        // Load translated and fallback strings
        String file = "journal_details_strings.json";
        JSONObject tr = TranslatedStore.get(file);
        JsonStringLoader assetLoader = new JsonStringLoader(requireContext(), file);

        // Function to get translated string or fallback
        java.util.function.Function<String, String> get = key -> tr != null && tr.has(key)
                ? tr.optString(key)
                : assetLoader.getString(key);

        // Find views
        TextView titleView        = view.findViewById(R.id.journal_title);
        ImageView imageView       = view.findViewById(R.id.journal_image);
        TextView countryView      = view.findViewById(R.id.journal_country);
        TextView dateView         = view.findViewById(R.id.journal_dates);
        TextView descriptionView  = view.findViewById(R.id.journal_description);
        TextView descriptionLabel = view.findViewById(R.id.journal_description_label);
        Button backButton         = view.findViewById(R.id.btn_back_home);
        Button deleteButton       = view.findViewById(R.id.btn_delete);

        // Apply translated static text
        descriptionLabel.setText(get.apply("description_title"));
        backButton.setText(get.apply("home_btn"));
        deleteButton.setText(get.apply("delete_btn"));

        // Get entry ID passed to the fragment
        int entryId = getArguments() != null
                ? getArguments().getInt("entryId", -1)
                : -1;

        // Load and display the journal entry
        if (entryId != -1) {
            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            JournalEntry entry = dbHelper.getEntryById(entryId);

            if (entry != null) {
                titleView.setText(entry.title);
                countryView.setText(entry.country);
                dateView.setText(entry.dateStart + " - " + entry.dateEnd);
                descriptionView.setText(entry.description);

                // Load image using Glide from either URI or drawable
                if (entry.imageName != null && entry.imageName.startsWith("content://")) {
                    Glide.with(requireContext())
                            .load(Uri.parse(entry.imageName))
                            .placeholder(R.drawable.image_icon)
                            .into(imageView);
                } else {
                    int resId = getResources()
                            .getIdentifier(entry.imageName, "drawable", requireContext().getPackageName());
                    if (resId != 0) {
                        Glide.with(requireContext()).load(resId).into(imageView);
                    } else {
                        Glide.with(requireContext()).load(R.drawable.image_icon).into(imageView);
                    }
                }
            }
        }

        // Handle "Back to Home" button
        backButton.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // Handle "Delete" button with confirmation dialog
        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(get.apply("delete_dialog_title"))
                    .setMessage(get.apply("delete_dialog_msg"))
                    .setPositiveButton(get.apply("yes_btn"), (dialog, which) -> {
                        SharedPreferences prefs = requireActivity()
                                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                        String username = prefs.getString("logged_in_user", null);

                        if (username != null) {
                            // delete the journal entry
                            new DatabaseHelper(requireContext())
                                    .deleteEntryById(username, entryId);

                            Toast.makeText(requireContext(),
                                    "Journal entry deleted",
                                    Toast.LENGTH_SHORT).show();

                            // Navigate back to HomeFragment
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.nav_host_fragment, new HomeFragment())
                                    .commit();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Error: No user found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(get.apply("cancel_btn"), null)
                    .show();
        });
    }
}
