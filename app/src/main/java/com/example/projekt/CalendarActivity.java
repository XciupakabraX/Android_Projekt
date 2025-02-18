package com.example.projekt;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalendarActivity extends AppCompatActivity implements EditMealDialogFragment.OnMealUpdatedListener {

    private CalendarView calendarView;
    private RecyclerView recyclerViewMealsForDay;
    private MealAdapter mealAdapter;
    private List<Meal> mealListForDay = new ArrayList<>();
    private AppDatabase database;
    private MealDao mealDao;
    private ImageView emptyListIcon;
    private TextView emptyListText;
    private String selectedDate;
    private Button btnAddMeal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> {
            // Przejdź do MainActivity
            setResult(RESULT_CANCELED);
            finish();
        });

        database = AppDatabase.getInstance(this);
        mealDao = database.mealDao();

        // Inicjalizacja CalendarView i RecyclerView
        calendarView = findViewById(R.id.calendarView);
        recyclerViewMealsForDay = findViewById(R.id.recyclerViewMealsForDay);
        emptyListIcon = findViewById(R.id.emptyListIcon);
        emptyListText = findViewById(R.id.emptyListText);
        btnAddMeal = findViewById(R.id.btnAddMeal);

        recyclerViewMealsForDay.setLayoutManager(new LinearLayoutManager(this));

        // Inicjalizowanie MealAdapter z listenerem
        mealAdapter = new MealAdapter(mealListForDay, meal -> {
            // Po kliknięciu na posiłek, wywołaj dialog edycji
            showEditMealDialog(meal);
        });
        recyclerViewMealsForDay.setAdapter(mealAdapter);

        setupSwipeToDelete();

        btnAddMeal.setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, AddMealActivity.class);
            intent.putExtra("KEY_SELECTED_DATE", selectedDate);
            addMealLauncher.launch(intent);
        });


        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        String todayDate = today.format(formatter);
        loadMealsForDate(todayDate);

        // Obsługa wybrania daty na kalendarzu
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = year + "/" + (month + 1) + "/" + dayOfMonth; // Formatujemy datę na "YYYY/MM/DD"
            loadMealsForDate(selectedDate);
        });
    }

    private void loadMealsForDate(String date) {

        new Thread(() -> {
            // Pobranie posiłków z bazy danych na podstawie daty
            List<Meal> mealsFromDb = mealDao.getMealsByDate(date);

            // Zaktualizowanie listy posiłków w głównym wątku
            runOnUiThread(() -> {
                mealListForDay.clear();
                if (!mealsFromDb.isEmpty()) {
                    mealListForDay.addAll(mealsFromDb);
                    emptyListIcon.setVisibility(View.GONE);
                    emptyListText.setVisibility(View.GONE);
                    recyclerViewMealsForDay.setVisibility(View.VISIBLE);
                } else {
                    emptyListIcon.setVisibility(View.VISIBLE);
                    emptyListText.setVisibility(View.VISIBLE);
                    recyclerViewMealsForDay.setVisibility(View.GONE);
                }
                mealAdapter.notifyDataSetChanged();

            });
        }).start();
    }

    private void showEditMealDialog(Meal meal) {
        if(meal.getName() == "Brak posiłków" && meal.getCalories() == 0){

        }
        else {
            EditMealDialogFragment dialogFragment = EditMealDialogFragment.newInstance(meal);
            dialogFragment.show(getSupportFragmentManager(), "edit_meal_dialog");
        }
    }

    @Override
    public void onMealUpdated(Meal updatedMeal) {
        new Thread(() -> {
            String currentlySelectedDate = getSelectedDateFromCalendar();

            mealDao.update(updatedMeal); // Zapisz zmiany w bazie danych

            // Odśwież listę
            runOnUiThread(() -> {
                loadMealsForDate(selectedDate);
            });
        }).start();
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                // Nie obsługujemy przesuwania w górę/dół
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Meal mealToDelete = mealListForDay.get(position);

                if(mealToDelete.getName() == "Brak posiłków" && mealToDelete.getCalories() == 0){
                    mealAdapter.notifyItemChanged(position);
                }
                else {
                    new AlertDialog.Builder(CalendarActivity.this)
                            .setTitle("Usuń posiłek")
                            .setMessage("Czy na pewno chcesz usunąć ten posiłek?")
                            .setPositiveButton("Tak", (dialog, which) -> {
                                // Usuń posiłek z bazy danych i listy
                                new Thread(() -> {
                                    mealDao.delete(mealToDelete); // Usuń z bazy danych

                                    // Aktualizacja interfejsu użytkownika
                                    runOnUiThread(() -> {
                                        mealListForDay.remove(position); // Usuń z listy
                                        mealAdapter.notifyItemRemoved(position); // Powiadom adapter
                                        Toast.makeText(CalendarActivity.this, "Posiłek usunięty", Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            })
                            .setNegativeButton("Nie", (dialog, which) -> {
                                // Przywróć element na listę, jeśli użytkownik anuluje
                                mealAdapter.notifyItemChanged(position);
                            })
                            .show();
                }
            }
        };

        // Przypisz gesty do RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewMealsForDay);
    }

    private String getSelectedDateFromCalendar() {
        long selectedDateMillis = calendarView.getDate();
        LocalDate selectedDate = LocalDate.ofEpochDay(selectedDateMillis / (24 * 60 * 60 * 1000));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        System.out.println(selectedDate.format(formatter));
        return selectedDate.format(formatter);
    }

    private final ActivityResultLauncher<Intent> addMealLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Odswież listę po dodaniu nowego posiłku
                    loadMealsForDate(selectedDate);
                }
                else if (result.getResultCode() == RESULT_CANCELED) {
                    Toast.makeText(this, "Dodawanie anulowane", Toast.LENGTH_SHORT).show();
                }
            }
    );
}