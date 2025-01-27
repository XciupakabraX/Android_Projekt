package com.example.projekt;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity implements EditMealDialogFragment.OnMealUpdatedListener {
    private AppDatabase database;
    private MealDao mealDao;

    private Button btnAddMeal, btnShowMap, btnShowCalendar;
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
        btnAddMeal = findViewById(R.id.btnAddMeal);
        btnShowMap = findViewById(R.id.btnShowMap);
        btnShowCalendar = findViewById(R.id.btnShowCalendar);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Inicjalizacja bazy danych
        database = AppDatabase.getInstance(this);
        mealDao = database.mealDao();

        // Obsługa dodawania posiłków
//        btnAddMeal.setOnClickListener(view -> addMeal());
        btnAddMeal.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddMealActivity.class);
            //startActivity(intent);
            addMealLauncher.launch(intent);
        });

        btnShowCalendar.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
//            startActivity(intent);
            calendarActivityLauncher.launch(intent);
        });

        // Przycisk do otwierania mapy
        btnShowMap.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });

        // Wczytaj posiłki z bazy danych
        loadMeals();

        setupSwipeToDelete();

        // Obsługa czujnika potrząśnięcia
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    private void deleteMeal(Meal meal) {
        new Thread(() -> {
            mealDao.delete(meal);

            // Odświeżenie listy posiłków po usunięciu
            runOnUiThread(() -> {
                Toast.makeText(this, "Usunięto posiłek!", Toast.LENGTH_SHORT).show();
                loadMeals();
            });
        }).start();
    }
    private void loadMeals() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        String todayDate = today.format(formatter);

        new Thread(() -> {
            List<Meal> meals = mealDao.getMealsByDate(todayDate);
            runOnUiThread(() -> {
//                mealAdapter = new MealAdapter(meals);
                mealAdapter = new MealAdapter(meals, meal -> {
                    EditMealDialogFragment dialog = EditMealDialogFragment.newInstance(meal);
                    dialog.show(getSupportFragmentManager(), "EditMealDialog");
                });
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

            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
            String todayDate = today.format(formatter);

            if (shake > 12) {
                new Thread(() -> {
//                    mealDao.deleteAll();
                    mealDao.deleteByDate(todayDate);
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

    private final ActivityResultLauncher<Intent> addMealLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Odswież listę po dodaniu nowego posiłku
                    loadMeals();
                }
                else if (result.getResultCode() == RESULT_CANCELED) {
                    Toast.makeText(this, "Dodawanie anulowane", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // Nie implementujemy przesuwania w górę/dół
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Meal mealToDelete = mealAdapter.getMeals().get(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Usuń posiłek")
                        .setMessage("Czy na pewno chcesz usunąć ten posiłek?")
                        .setPositiveButton("Tak", (dialog, which) -> {
                            // Usuń posiłek
                            deleteMeal(mealToDelete);
                            mealAdapter.getMeals().remove(position);
                            mealAdapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton("Nie", (dialog, which) -> {
                            // Przywróć element
                            mealAdapter.notifyItemChanged(position);
                        })
                        .show();
            }
        };

        // Przypisz callback do RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onMealUpdated(Meal updatedMeal) {
        new Thread(() -> {
            mealDao.update(updatedMeal); // Zapisz zmiany w bazie danych
            runOnUiThread(() -> loadMeals()); // Odśwież listę
        }).start();
    }

    private final ActivityResultLauncher<Intent> calendarActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Odswież listę posiłków po powrocie z CalendarActivity
                    loadMeals();
                }
                else if (result.getResultCode() == RESULT_CANCELED) {
                    // Odswież listę posiłków po powrocie z CalendarActivity
                    loadMeals();
                }
            }
    );
}
