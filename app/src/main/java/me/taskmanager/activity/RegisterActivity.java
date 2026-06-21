package me.taskmanager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import me.taskmanager.R;
import me.taskmanager.database.UserRepository;

import androidx.lifecycle.ViewModelProvider;
import me.taskmanager.viewmodel.RegisterViewModel;

/**
 * Registration screen for creating a new account.
 * After successful registration, redirects to LoginActivity.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etUsername, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvError, tvLoginLink;
    private ProgressBar progressBar;

    private RegisterViewModel registerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        registerViewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        tilFullName        = findViewById(R.id.til_full_name);
        tilUsername        = findViewById(R.id.til_username);
        tilEmail           = findViewById(R.id.til_email);
        tilPassword        = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFullName        = findViewById(R.id.et_full_name);
        etUsername        = findViewById(R.id.et_username);
        etEmail           = findViewById(R.id.et_email);
        etPassword        = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegister  = findViewById(R.id.btn_register);
        tvError      = findViewById(R.id.tv_error);
        tvLoginLink  = findViewById(R.id.tv_login_link);
        progressBar  = findViewById(R.id.progress_bar);

        // Setup observers
        registerViewModel.getRegisterResultLiveData().observe(this, result -> {
            if (result != null) {
                if (result > 0) {
                    showSuccessAndLogin();
                } else if (result == -2) {
                    tilUsername.setError("Username already taken");
                    etUsername.requestFocus();
                } else {
                    showError("Registration failed. Please try again.");
                }
            }
        });

        registerViewModel.getIsLoadingLiveData().observe(this, this::setLoading);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvLoginLink.setOnClickListener(v -> finish()); // Go back to Login
    }

    private void attemptRegister() {
        String fullName  = getText(etFullName);
        String username  = getText(etUsername);
        String email     = getText(etEmail);
        String password  = getText(etPassword);
        String confirm   = getText(etConfirmPassword);

        // Clear errors
        tilFullName.setError(null);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        hideError();

        // Validate
        if (fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }
        if (username.isEmpty()) {
            tilUsername.setError("Username is required");
            etUsername.requestFocus();
            return;
        }
        if (username.length() < 3) {
            tilUsername.setError("Username must be at least 3 characters");
            etUsername.requestFocus();
            return;
        }
        if (!username.matches("[a-zA-Z0-9_]+")) {
            tilUsername.setError("Username can only contain letters, numbers and _");
            etUsername.requestFocus();
            return;
        }
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        registerViewModel.register(username, password, fullName, email);
    }

    private void showSuccessAndLogin() {
        // Show quick confirmation then go to login
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Account Created!")
                .setMessage("Your account has been created successfully. Please login.")
                .setPositiveButton("Login", (dialog, which) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
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
