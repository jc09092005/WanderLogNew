package com.example.wanderlognew;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddFragment extends Fragment {

    // Constants for identifying image request results
    private static final int REQUEST_IMAGE_PICK = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    // Holds the URI of the selected or captured image
    private Uri imageUri;   // null until the user chooses / captures an image

    // Shared reference to the image view in the layout
    private ImageView imageView;

    public AddFragment() {

    }

    // Called to initialise the fragment UI
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // Screen orientation is locked to portrait
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        View view = inflater.inflate(R.layout.fragment_add_journal, container, false);

        // Initialise image view and show default placeholder
        imageView = view.findViewById(R.id.add_image);
        showPlaceholder();

        // Translation and UI setup begins
        ensureTranslationThenDisplay(view);
        return view;
    }

    // Handles translation loading before displaying the UI
    private void ensureTranslationThenDisplay(View view) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        int savedIndex = prefs.getInt("language_index", 0);
        String[] langCodes = {"EN", "ES", "IT"};
        String langCode = langCodes[savedIndex];

        String file = "add_fragment_strings.json";

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

    // Main method to wire up and populate the UI
    private void displayUI(View view) {
        // Load translated strings or fallback JSON
        JSONObject tr = TranslatedStore.get("add_fragment_strings.json");
        JsonStringLoader json = (tr == null)
                ? new JsonStringLoader(requireContext(), "add_fragment_strings.json") : null;

        //Fallback logic for getting a string by key
        java.util.function.Function<String, String> get = key -> tr != null ? tr.optString(key, key) : json.getString(key);

        // Find views from the layout
        TextView addTitle = view.findViewById(R.id.add_title);
        TextView journalTitleLabel = view.findViewById(R.id.add_journal_title_label);
        EditText titleField = view.findViewById(R.id.input_title);
        TextView countryLabel = view.findViewById(R.id.add_country_label);
        AutoCompleteTextView countryIn = view.findViewById(R.id.input_country);
        TextView dateRangeTitle = view.findViewById(R.id.add_date_range_title);
        EditText startDateField = view.findViewById(R.id.input_start_date);
        EditText endDateField = view.findViewById(R.id.input_end_date);
        TextView errorDateRange = view.findViewById(R.id.error_date_range);
        TextView descLabel = view.findViewById(R.id.add_description_label);
        EditText descField = view.findViewById(R.id.input_description);
        Button uploadBtn = view.findViewById(R.id.btn_upload_image);
        Button saveBtn = view.findViewById(R.id.btn_save);

        // Save button is styled
        AppCompatButton btn = view.findViewById(R.id.btn_save);
        Drawable d = AppCompatResources.getDrawable(requireContext(), R.drawable.button_styling_light).mutate();
        DrawableCompat.setTint(d, ContextCompat.getColor(requireContext(), R.color.light_grey));
        btn.setBackground(d);

        // Translated text is set in all fields
        addTitle.setText(get.apply("add_title"));
        journalTitleLabel.setText(get.apply("title_title"));
        titleField.setHint(get.apply("title_hint"));
        countryLabel.setText(get.apply("country_title"));
        countryIn.setHint(get.apply("country_hint"));
        dateRangeTitle.setText(get.apply("date_range_title"));
        startDateField.setHint(get.apply("start_hint"));
        endDateField.setHint(get.apply("end_hint"));
        descLabel.setText(get.apply("description_title"));
        descField.setHint(get.apply("description_hint"));
        uploadBtn.setText(get.apply("upload_img_btn"));
        saveBtn.setText(get.apply("save_btn"));

        // Country dropdown is populated with ISO country list
        String[] isos = Locale.getISOCountries();
        List<String> countries = new ArrayList<>();
        for (String code : isos) {
            countries.add(new Locale("", code).getDisplayCountry());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, countries);
        countryIn.setAdapter(adapter);
        countryIn.setThreshold(1);
        countryIn.setOnClickListener(v -> countryIn.showDropDown());

        // Date field click handlers to show date pickers
        startDateField.setOnClickListener(v -> showDatePicker(startDateField));
        endDateField.setOnClickListener(v -> showDatePicker(endDateField));

        // Image upload button handler
        uploadBtn.setOnClickListener(v -> showImageSourceDialog());

        // Save button handler
        saveBtn.setOnClickListener(v -> saveEntry(startDateField, endDateField,
                titleField, countryIn,
                descField, errorDateRange));
    }

    // Displays default image icon in the image view
    private void showPlaceholder() {
        imageUri = null;
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.image_icon2);
    }

    // Opens a date picker and writes selected date into the input field
    private void showDatePicker(EditText target) {
        // get today’s year/month/day
        Calendar c = Calendar.getInstance();

        // build the dialog
        DatePickerDialog dpd = new DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                (view, year, month, day) -> {
                    // when they pick a date, write it back to your field
                    target.setText(String.format(
                            Locale.getDefault(),
                            "%04d-%02d-%02d",
                            year, month + 1, day));
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        // disallow dates after today
        dpd.getDatePicker().setMaxDate(System.currentTimeMillis());

        dpd.show();
    }


    // Dialog to choose between gallery or camera image source
    private void showImageSourceDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Image")
                .setItems(new CharSequence[]{"Choose from Gallery", "Take Photo"},
                        (d,i)->{ if(i==0) openGallery(); else openCamera(); })
                .show();
    }

    // Opens image picker for gallery
    private void openGallery() {
        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pick.setType("image/*");
        startActivityForResult(pick, REQUEST_IMAGE_PICK);
    }

    // Opens camera to take a new photo
    private void openCamera() {
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cam.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photo = createImageFile();
            if (photo != null) {
                imageUri = FileProvider.getUriForFile(requireContext(),
                        "com.example.wanderlognew.fileprovider", photo);
                cam.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cam, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // Creates a temporary image file with timestamped name
    private File createImageFile() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try { return File.createTempFile("JPEG_"+ts+"_",".jpg", dir); }
        catch (IOException e) { e.printStackTrace(); return null; }
    }

   // Handles image pick/capture result and displays image
    @Override
    public void onActivityResult(int req,int res,Intent data) {
        super.onActivityResult(req,res,data);

        if (req==REQUEST_IMAGE_PICK && res==Activity.RESULT_OK && data!=null) {
            Uri picked = data.getData();
            if (picked!=null) {
                imageUri = copyToInternalStorage(picked);
                imageView.setImageURI(imageUri);
            }
        }

        if (req==REQUEST_IMAGE_CAPTURE && res==Activity.RESULT_OK) {
            imageView.setImageURI(imageUri);   // already set in openCamera()
        }
    }

    // Copies selected image to app's internal storage and compresses it
    private Uri copyToInternalStorage(Uri src) {
        try {
            // Decode only bounds to decide scaling
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream in = requireContext().getContentResolver().openInputStream(src)) {
                BitmapFactory.decodeStream(in, null, options);
            }

            // Calculate a sample size (e.g. max 1024×1024)
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;

            // Decode the scaled‐down bitmap
            Bitmap bitmap;
            try (InputStream in2 = requireContext().getContentResolver().openInputStream(src)) {
                bitmap = BitmapFactory.decodeStream(in2, null, options);
            }

            if (bitmap == null) throw new IOException("Failed to decode bitmap");

            // Write it out compressed
            File dst = new File(requireContext().getFilesDir(),
                    "img_"+System.currentTimeMillis()+".jpg");
            try (FileOutputStream out = new FileOutputStream(dst)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            }

            return FileProvider.getUriForFile(requireContext(),
                    "com.example.wanderlognew.fileprovider", dst);

        } catch (Exception e) {
            Log.e("AddFragment","image copy/compress error", e);
            return null;
        }
    }

    // Calculates an appropriate sample size to downscale large images
    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width  = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth  = width  / 2;

            // Increase inSampleSize until fits
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth  / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // Validates form fields and saves a journal entry to the database
    private void saveEntry(EditText startF, EditText endF, EditText titleF,
                           AutoCompleteTextView countryF, EditText descF,
                           TextView errorLbl) {

        // Read and clean input values
        String start = startF.getText().toString().trim();
        String end   = endF  .getText().toString().trim();
        String title = titleF.getText().toString().trim();
        String country = countryF.getText().toString().trim();
        String desc   = descF .getText().toString().trim();

        errorLbl.setVisibility(View.GONE);

        // Validate date input
        if (start.isEmpty()||end.isEmpty()) {
            errorLbl.setText("Both start and end dates are required");
            errorLbl.setVisibility(View.VISIBLE); return;
        }
        if (start.compareTo(end)>0) {
            errorLbl.setText("Start date must be before end date");
            errorLbl.setVisibility(View.VISIBLE); return;
        }

        // Get image URI or use fallback placeholder
        String imgPath = (imageUri!=null)? imageUri.toString() : "placeholder";

        // Build and save new journal entry
        JournalEntry entry = new JournalEntry(0,title,country,start,end,desc,imgPath);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String user = prefs.getString("logged_in_user", null);
        if (user==null) { Toast.makeText(requireContext(),"No user",Toast.LENGTH_SHORT).show(); return; }

        DatabaseHelper db = new DatabaseHelper(requireContext());
        db.insertJournalEntry(entry, user);

        // Reset UI and return to HomeFragment
        showPlaceholder();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment,new HomeFragment())
                .commit();
    }
}
