package com.example.wanderlognew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    // UI references
    private EditText inputUsername, inputEmail, inputPassword, inputConfirmPassword;
    private Button btnSignUp, btnLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lock screen orientation to portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this); // Database handler instance

        // Find views by ID
        inputUsername = findViewById(R.id.input_username);
        inputEmail = findViewById(R.id.input_email);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);
        btnSignUp = findViewById(R.id.btn_signup);
        btnLogin = findViewById(R.id.btn_login);

        // Load translated or localized UI strings
        JsonStringLoader stringLoader = new JsonStringLoader(this, "signup_strings.json");

        // Apply strings to UI components
        ((TextView) findViewById(R.id.signup_title)).setText(stringLoader.getString("signup_title"));
        inputUsername.setHint(stringLoader.getString("username_hint"));
        inputEmail.setHint(stringLoader.getString("email_hint"));
        inputPassword.setHint(stringLoader.getString("password_hint"));
        inputConfirmPassword.setHint(stringLoader.getString("confirm_pass_hint"));
        btnSignUp.setText(stringLoader.getString("signup_button"));
        ((TextView) findViewById(R.id.signup_account_text)).setText(stringLoader.getString("account_already"));
        btnLogin.setText(stringLoader.getString("login_button"));

        // Email field validation in real-time
        inputEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                    inputEmail.setError("Enter a valid email address");
                } else {
                    inputEmail.setError(null);
                }
            }
        });

        // Password validation: at least 1 uppercase, 1 digit, 5 characters
        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pattern = "^(?=.*[A-Z])(?=.*\\d).{5,}$";
                if (!s.toString().matches(pattern)) {
                    inputPassword.setError("Password must include: 1 uppercase letter, 1 number");
                } else {
                    inputPassword.setError(null);
                }
            }
        });

        // Confirm password real-time matching
        inputConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(inputPassword.getText().toString())) {
                    inputConfirmPassword.setError("Passwords do not match");
                } else {
                    inputConfirmPassword.setError(null);
                }
            }
        });

        // Sign-up button logic
        btnSignUp.setOnClickListener(v -> {
            // Retrieve and clean inputs
            String username = inputUsername.getText().toString().trim();
            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String confirmPassword = inputConfirmPassword.getText().toString().trim();

            // Input validation
            if (username.isEmpty()) {
                inputUsername.setError("Username is required");
                return;
            }
            if (dbHelper.isUsernameTaken(username)) {
                inputUsername.setError("Username is already taken");
                return;
            }
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inputEmail.setError("Valid email is required");
                return;
            }
            if (password.isEmpty() || !password.matches("^(?=.*[A-Z])(?=.*\\d).{5,}$")) {
                inputPassword.setError("Password must include: 1 uppercase letter, 1 number, min 5 characters");
                return;
            }
            if (!password.equals(confirmPassword)) {
                inputConfirmPassword.setError("Passwords do not match");
                return;
            }

            // Register user in database
            boolean success = dbHelper.registerUser(username, email, password);
            if (success) {
                // Save user session to SharedPreferences
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                prefs.edit().putString("logged_in_user", username).apply();

                // Redirect to main/home page
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Sign up failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to login screen
        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
