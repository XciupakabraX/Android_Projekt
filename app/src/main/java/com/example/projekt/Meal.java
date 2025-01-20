package com.example.projekt;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "meals")
public class Meal {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private int calories;
    private String date; // YYYY-MM-DD

    // Konstruktory
    public Meal(String name, int calories, String date) {
        this.name = name;
        this.calories = calories;
        this.date = date;
    }

    // Gettery i Settery
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
