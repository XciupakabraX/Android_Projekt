package com.example.projekt;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EditMealDialogFragment extends DialogFragment {

    public interface OnMealUpdatedListener {
        void onMealUpdated(Meal updatedMeal);
    }

    private OnMealUpdatedListener listener;
    private Meal meal;

    public static EditMealDialogFragment newInstance(Meal meal) {
        EditMealDialogFragment fragment = new EditMealDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("meal", meal); // Przekazanie obiektu Meal
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnMealUpdatedListener) {
            listener = (OnMealUpdatedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnMealUpdatedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_meal, container, false);

        EditText editTextName = view.findViewById(R.id.editTextName);
        EditText editTextCalories = view.findViewById(R.id.editTextCalories);
        EditText editTextDate = view.findViewById(R.id.editTextDate);
        Button btnSave = view.findViewById(R.id.btnSave);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // Pobierz posiłek z argumentów
        if (getArguments() != null) {
            meal = (Meal) getArguments().getSerializable("meal");
            if (meal != null) {
                editTextName.setText(meal.getName());
                editTextCalories.setText(String.valueOf(meal.getCalories()));
                editTextDate.setText(meal.getDate());
            }
        }

        // Obsługa przycisku "Zapisz"
        btnSave.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String caloriesStr = editTextCalories.getText().toString().trim();
            String date = editTextDate.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(caloriesStr) || TextUtils.isEmpty(date)) {
                Toast.makeText(getContext(), "Wypełnij wszystkie pola!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int calories = Integer.parseInt(caloriesStr);
                meal.setName(name);
                meal.setCalories(calories);
                meal.setDate(date);

                if (listener != null) {
                    listener.onMealUpdated(meal); // Powiadomienie o aktualizacji
                }
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Podaj poprawną liczbę kalorii!", Toast.LENGTH_SHORT).show();
            }
        });

        // Obsługa przycisku "Anuluj"
        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}
