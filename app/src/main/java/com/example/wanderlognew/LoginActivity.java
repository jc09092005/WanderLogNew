//package com.example.wanderlognew;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class LoginActivity extends AppCompatActivity{
//    @Override
//    protected void onCreate(Bundle savedInstanceState){
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//
//        // Loading of Strings
//        JsonStringLoader jsonLoader = new JsonStringLoader(this, "login_strings.json");
//
//        // Set text dynamically using IDs from XML
//        ((android.widget.TextView) findViewById(R.id.login_title)).setText(jsonLoader.getString("title"));
//        ((android.widget.TextView) findViewById(R.id.login_subtitle)).setText(jsonLoader.getString("subtitle"));
//        ((android.widget.TextView) findViewById(R.id.login_heading)).setText(jsonLoader.getString("log_in"));
//        inputUsername.setHint(jsonLoader.getString("username_hint"));
//        inputPassword.setHint(jsonLoader.getString("password_hint"));
//        loginButton.setText(jsonLoader.getString("login_button"));
//        ((android.widget.TextView) findViewById(R.id.login_no_account)).setText(jsonLoader.getString("no_account"));
//        signupBtn.setText(jsonLoader.getString("signin_button"));
//
//
//
//
//        DatabaseHelper dbHelper = new DatabaseHelper(this);
//        EditText inputUsername = findViewById(R.id.input_username);
//        EditText inputPassword = findViewById(R.id.input_password);
//
//
//        Button loginButton = findViewById(R.id.btn_login);
//        loginButton.setOnClickListener(v -> {
//            String username = inputUsername.getText().toString().trim();
//            String password = inputPassword.getText().toString().trim();
//
//            if (username.isEmpty()){
//                Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (password.isEmpty()){
//                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if(dbHelper.validateUser(username, password)){
//                // Save logged-in username
//                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
//                prefs.edit().putString("logged_in_user", username).apply();
//                startActivity(new Intent(LoginActivity.this, MainActivity.class));
//                finish();
//            } else {
//                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
//
//            }
//        });
//
//
//        Button signupBtn = findViewById(R.id.btn_signup);
//        signupBtn.setOnClickListener(v ->
//                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
//    }
//}

package com.example.wanderlognew;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity handles user login and redirects authenticated users to the MainActivity.
 * It supports dynamic UI text loading from a JSON file for multilingual support.
 */
public class LoginActivity extends AppCompatActivity {
    // Used to load translated strings from JSON
    private JsonStringLoader stringLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Force portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_login);

        // Load UI strings from JSON file
        stringLoader = new JsonStringLoader(this, "login_strings.json");

        // UI elements
        TextView title = findViewById(R.id.login_title);
        TextView subtitle = findViewById(R.id.login_subtitle);
        TextView logInLabel = findViewById(R.id.login_label);
        TextView noAccount = findViewById(R.id.no_account);
        EditText inputUsername = findViewById(R.id.input_username);
        EditText inputPassword = findViewById(R.id.input_password);
        Button loginButton = findViewById(R.id.btn_login);
        Button signupButton = findViewById(R.id.btn_signup);

        // set translated or fallback text values to UI elements
        title.setText(stringLoader.getString("title"));
        subtitle.setText(stringLoader.getString("subtitle"));
        logInLabel.setText(stringLoader.getString("log_in"));
        noAccount.setText(stringLoader.getString("no_account"));
        inputUsername.setHint(stringLoader.getString("username_hint"));
        inputPassword.setHint(stringLoader.getString("password_hint"));
        loginButton.setText(stringLoader.getString("login_button"));
        signupButton.setText(stringLoader.getString("signin_button"));

        // Initialise database helper for user validation
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // Login button logic
        loginButton.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check credentials using DatabaseHelper
            if (dbHelper.validateUser(username, password)) {
                // Save the logged-in username in SharedPreferences
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                prefs.edit().putString("logged_in_user", username).apply();

                // Redirect to the MainActivity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish(); // close LoginActivity
            } else {
                // Show error if login fails
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to the SignUp screen
        signupButton.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
    }
}
