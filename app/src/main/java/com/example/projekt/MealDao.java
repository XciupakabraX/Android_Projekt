package com.example.projekt;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MealDao {
    // Dodanie nowego posiłku
    @Insert
    void insert(Meal meal);

    // Pobranie wszystkich posiłków dla danego dnia
    @Query("SELECT * FROM meals WHERE date = :date")
    List<Meal> getMealsByDate(String date);

    // Usunięcie konkretnego posiłku
    @Delete
    void delete(Meal meal);

    // Usunięcie wszystkich posiłków
    @Query("DELETE FROM meals")
    void deleteAll();

    @Query("DELETE FROM meals WHERE date= :date")
    void deleteByDate(String date);

    @Update
    void update(Meal meal);
}

