package com.example.projekt;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
    private Button goBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        database = AppDatabase.getInstance(this);
        mealDao = database.mealDao();
        goBack = findViewById(R.id.goBack);

        // Inicjalizacja CalendarView i RecyclerView
        calendarView = findViewById(R.id.calendarView);
        recyclerViewMealsForDay = findViewById(R.id.recyclerViewMealsForDay);

        recyclerViewMealsForDay.setLayoutManager(new LinearLayoutManager(this));
//        mealAdapter = new MealAdapter(mealListForDay);

        // Inicjalizowanie MealAdapter z listenerem
        mealAdapter = new MealAdapter(mealListForDay, meal -> {
            // Po kliknięciu na posiłek, wywołaj dialog edycji
            showEditMealDialog(meal);
        });
        recyclerViewMealsForDay.setAdapter(mealAdapter);

        setupSwipeToDelete();


        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/M/d");
        String todayDate = today.format(formatter);
        loadMealsForDate(todayDate);

        // Obsługa wybrania daty na kalendarzu
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = year + "/" + (month + 1) + "/" + dayOfMonth; // Formatujemy datę na "YYYY/MM/DD"
            loadMealsForDate(selectedDate);
        });

        goBack.setOnClickListener(view -> {
            // Zamknij aktywność bez żadnych działań
            setResult(RESULT_OK);
            finish();
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
                } else {
                    mealListForDay.add(new Meal("Brak posiłków", 0, date));
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
//            runOnUiThread(() -> loadMealsForDate(updatedMeal.getDate())); // Odśwież listę
            runOnUiThread(() -> {
                if (updatedMeal.getDate().equals(currentlySelectedDate)) {
                    // Jeśli posiłek należy do wybranej daty, odśwież listę dla tego dnia
                    loadMealsForDate(currentlySelectedDate);
                } else {
                    // Jeśli zmieniono datę na inną, usuń posiłek z listy aktualnego dnia
                    for (int i = 0; i < mealListForDay.size(); i++) {
                        if (mealListForDay.get(i).getId() == updatedMeal.getId()) {
                            mealListForDay.remove(i);
                            mealAdapter.notifyItemRemoved(i);
                            break;
                        }
                    }
                }
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
}