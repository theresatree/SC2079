package com.example.sc2079;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.example.sc2079.databinding.ActivityMainBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity"; // Helps to filter logs from MainActivity

    boolean showBTFragment = false;
    private GridFragment gridFragment = new GridFragment();
    private BluetoothSetUpFragment bluetoothFragment = new BluetoothSetUpFragment();

    ActivityMainBinding binding;
    private final int[] ICONS = new int[]{
            R.drawable.baseline_home_24,
            R.drawable.baseline_bluetooth_24
    };
    private final String[] TAB_TITLE = new String[]{
            "Home",
            "Bluetooth"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // view binding for navbar
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        //setContentView(binding.getRoot());
//
//        binding.bottomNavigationView3.setOnItemSelectedListener(item -> {
//            switch(item.getItemId()){
//                case R.id.home:
//                    break;
//                case R.id.bluetooth:
//                    break;
//            }
//
//            return true;
//        });

        // Setup navbar tabs
        TabLayout tabLayout = findViewById(R.id.tabs);

        ViewPager2 viewPager2 = findViewById(R.id.view_pager);
        //help to preload and keep the other fragment
        viewPager2.setOffscreenPageLimit(3);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);

        viewPager2.setAdapter(adapter);
        viewPager2.setUserInputEnabled(false);

        tabLayout.addTab(tabLayout.newTab().setText("Home").setIcon(ICONS[0]));
        tabLayout.addTab(tabLayout.newTab().setText("Bluetooth").setIcon(ICONS[1]));

        tabLayout.setSelectedTabIndicator(R.color.black);
        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(TAB_TITLE[position]);
                tab.setIcon(ICONS[position]);

            }
        }).attach();

        getBTPermission();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load GridFragment when the activity is started, added bluetoothFragment
        if (savedInstanceState == null) {
            //openGridFragment();
            //createBTFragment();
            //bluetoothFragment = BluetoothSetUpFragment.newInstance("", "");
            //gridFragment = GridFragment.newInstance("", "");
        }


//  TO BE REMOVED, DEPRECATED
//        ImageButton btnBluetooth = findViewById(R.id.btnBluetooth);
//        btnBluetooth.setOnClickListener(v -> {
//            //Intent goToBlueTooth = new Intent(this, BluetoothSetUpActivity.class);
//            //startActivity(goToBlueTooth);
//            Log.d(TAG, "clicked");
//            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//            if(showBTFragment == false)
//            {
//                ft.show(bluetoothFragment);
//                ft.hide(gridFragment);
//            }
//            else
//            {
//                ft.hide(bluetoothFragment);
//                ft.show(gridFragment);
//            }
//            ft.commit();
//            showBTFragment = !showBTFragment;
//        });
    }

//    private void replaceFragment(Fragment fragment){
//        FragmentManager fragmentManager = getSupportFragmentManager();
//        FragmentTransaction transaction = fragmentManager.beginTransaction();
//        transaction.replace(R.id.frame_layout, fragment);
//
//    }

//    private void openGridFragment() {
//        // Create an instance of GridFragment
//        GridFragment gridFragment = new GridFragment();
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        // Replace the container with the new fragment
//        transaction.add(R.id.fragmentGrid, gridFragment); // changed .replace to .add to test
//        transaction.addToBackStack(null);
//        // Commit the transaction to apply the change
//        transaction.commit();
//    }
//
//    private void createBTFragment(){
//        BluetoothSetUpFragment bluetoothFragment = new BluetoothSetUpFragment();
//        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.add(R.id.bluetoothFragment, bluetoothFragment);
//        transaction.addToBackStack(null);
//        transaction.commit();
//
//
//    }

    public void getBTPermission(){
        // For some reason, we need to ask for bluetooth permission during runtime to enable.
        // Therefore, we got to repeat the permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above, request Bluetooth permissions
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

}