package me.taskmanager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import me.taskmanager.R;
import me.taskmanager.database.UserRepository;
import me.taskmanager.model.User;
import me.taskmanager.preferences.UserPreferencesManager;

import androidx.lifecycle.ViewModelProvider;
import me.taskmanager.viewmodel.LoginViewModel;

/**
 * Login screen. Acts as the launcher activity.
 * If a session already exists, redirects immediately to MainActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private TextView tvError, tvRegisterLink;
    private ProgressBar progressBar;

    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Apply dark mode preference on startup
        if (loginViewModel.isDarkModeEnabled()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        // If already logged in, go straight to MainActivity
        if (loginViewModel.isAlreadyLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        tilUsername = findViewById(R.id.til_username);
        tilPassword = findViewById(R.id.til_password);
        etUsername  = findViewById(R.id.et_username);
        etPassword  = findViewById(R.id.et_password);
        btnLogin    = findViewById(R.id.btn_login);
        tvError     = findViewById(R.id.tv_error);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        progressBar = findViewById(R.id.progress_bar);

        // Set up observers
        loginViewModel.getLoginSuccessLiveData().observe(this, user -> {
            if (user != null) {
                goToMain();
            }
        });

        loginViewModel.getLoginErrorLiveData().observe(this, this::showError);

        loginViewModel.getIsLoadingLiveData().observe(this, this::setLoading);

        btnLogin.setOnClickListener(v -> attemptLogin());

        // Allow pressing Done on keyboard to trigger login
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String username = getText(etUsername);
        String password = getText(etPassword);

        // Clear previous errors
        tilUsername.setError(null);
        tilPassword.setError(null);
        hideError();

        // Validate
        if (username.isEmpty()) {
            tilUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        loginViewModel.login(username, password);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
