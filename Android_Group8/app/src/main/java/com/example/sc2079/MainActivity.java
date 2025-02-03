package com.example.sc2079;


import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity"; // Helps to filter logs from MainActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load GridFragment when the activity is started
        if (savedInstanceState == null) {
            openGridFragment();
        }

        ImageButton btnBluetooth = findViewById(R.id.btnBluetooth);
        btnBluetooth.setOnClickListener(v -> {
            Intent goToBlueTooth = new Intent(this, BluetoothSetUpActivity.class);
            startActivity(goToBlueTooth);
        });
    }

    private void openGridFragment() {
        // Create an instance of GridFragment
        GridFragment gridFragment = new GridFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        // Replace the container with the new fragment
        transaction.replace(R.id.fragmentGrid, gridFragment);
        transaction.addToBackStack(null);
        // Commit the transaction to apply the change
        transaction.commit();
    }

}