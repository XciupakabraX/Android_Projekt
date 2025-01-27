package com.example.projekt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MealAdapter extends RecyclerView.Adapter<MealAdapter.MealViewHolder> {
    private List<Meal> mealList;
//    private OnMealDeleteListener deleteListener;
    private OnMealClickListener clickListener;

    public interface OnMealClickListener {
        void onMealClick(Meal meal);
    }
//
//    public interface OnMealDeleteListener {
//        void onMealDelete(Meal meal);
//    }

//    public MealAdapter(List<Meal> mealList, OnMealDeleteListener deleteListener) {
//        this.mealList = mealList;
//        this.deleteListener = deleteListener;
//    }

    //tymczasowo?
    public MealAdapter(List<Meal> mealList) {
        this.mealList = mealList;
    }

    public MealAdapter(List<Meal> meals, OnMealClickListener clickListener) {
        this.mealList = meals;
        this.clickListener = clickListener;
    }

    @Override
    public MealViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MealViewHolder holder, int position) {
        Meal meal = mealList.get(position);
        holder.name.setText(meal.getName());
        holder.calories.setText("Kalorie: " + meal.getCalories());

//        // Obsługa długiego kliknięcia w celu usunięcia posiłku
//        holder.itemView.setOnLongClickListener(v -> {
//            if (deleteListener != null) {
//                deleteListener.onMealDelete(meal);
//            }
//            return true;
//        });
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMealClick(meal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mealList.size();
    }
    public List<Meal> getMeals() {
        return mealList;
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView name, calories;
        public MealViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            calories = itemView.findViewById(android.R.id.text2);
        }
    }
}

