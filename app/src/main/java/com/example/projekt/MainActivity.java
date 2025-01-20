package com.example.projekt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppDatabase database;
    private MealDao mealDao;

    private EditText editTextName, editTextCalories, editTextDate;
    private Button btnAddMeal, btnShowMap;
    private RecyclerView recyclerView;
    private MealAdapter mealAdapter;

    private SensorManager sensorManager;
    private float acelVal, acelLast, shake;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicjalizacja UI
        editTextName = findViewById(R.id.editTextName);
        editTextCalories = findViewById(R.id.editTextCalories);
        editTextDate = findViewById(R.id.editTextDate);
        btnAddMeal = findViewById(R.id.btnAddMeal);
        btnShowMap = findViewById(R.id.btnShowMap);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicjalizacja bazy danych
        database = AppDatabase.getInstance(this);
        mealDao = database.mealDao();

        // Obsługa dodawania posiłków
        btnAddMeal.setOnClickListener(view -> addMeal());

        // Przycisk do otwierania mapy
        btnShowMap.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });

        // Wczytaj posiłki z bazy danych
        loadMeals();

        // Obsługa czujnika potrząśnięcia
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void addMeal() {
        String name = editTextName.getText().toString().trim();
        String caloriesStr = editTextCalories.getText().toString().trim();
        String date = editTextDate.getText().toString().trim();

        if (name.isEmpty() || caloriesStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Wypełnij wszystkie pola!", Toast.LENGTH_SHORT).show();
            return;
        }

        int calories = Integer.parseInt(caloriesStr);
        Meal meal = new Meal(name, calories, date);

        new Thread(() -> {
            mealDao.insert(meal);
            runOnUiThread(() -> {
                Toast.makeText(this, "Dodano posiłek!", Toast.LENGTH_SHORT).show();
                loadMeals();
            });
        }).start();
    }

    private void loadMeals() {
        new Thread(() -> {
            List<Meal> meals = mealDao.getMealsByDate("2024-01-20");
            runOnUiThread(() -> {
                mealAdapter = new MealAdapter(meals);
                recyclerView.setAdapter(mealAdapter);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
    }

    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            acelLast = acelVal;
            acelVal = (float) Math.sqrt((x * x + y * y + z * z));
            float delta = acelVal - acelLast;
            shake = shake * 0.9f + delta;

            if (shake > 12) {
                new Thread(() -> {
                    mealDao.deleteAll();
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Lista posiłków została wyczyszczona!", Toast.LENGTH_SHORT).show();
                        loadMeals();
                    });
                }).start();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };
}
