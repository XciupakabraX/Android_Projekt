package com.example.projekt;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class AddMealActivity extends AppCompatActivity {

    private EditText editTextName, editTextCalories, editTextDate;
    private Button btnAddMeal;
//    private Button btnCancel;

    private AppDatabase database;
    private MealDao mealDao;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_meal);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> {
            // Przejdź do MainActivity
            setResult(RESULT_CANCELED);
            finish();
        });
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            // Pobranie koloru z motywu
//            TypedValue typedValue = new TypedValue();
//            getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
//            int primaryColor = typedValue.data;
//
//            // Ustawienie koloru status baru
//            getWindow().setStatusBarColor(primaryColor);
//        }


        editTextName = findViewById(R.id.editTextName);
        editTextCalories = findViewById(R.id.editTextCalories);
        editTextDate = findViewById(R.id.editTextDate);
        btnAddMeal = findViewById(R.id.btnAddMeal);
//        btnCancel = findViewById(R.id.btnCancel);

        database = AppDatabase.getInstance(this);
        mealDao = database.mealDao();

        btnAddMeal.setOnClickListener(view -> addMeal());

//        btnCancel.setOnClickListener(view -> {
//            // Zamknij aktywność bez żadnych działań
//            setResult(RESULT_CANCELED);
//            finish();
//        });

        //
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        String todayDate = year + "/" + (month + 1) + "/" + day;
        editTextDate.setText(todayDate);

        //
        editTextDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddMealActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Wybrana data w formacie
                        String selectedDate = selectedYear + "/" + (selectedMonth + 1) + "/" + selectedDay;
                        editTextDate.setText(selectedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

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
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }
}