package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.carblookup.db.MealRepository;
import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.models.Treatments;

import java.util.List;
import java.util.UUID;

class MealLogService {
    private final MealRepository mealRepository;
    private final MealItemFactory mealItemFactory;

    MealLogService(MealRepository mealRepository) {
        this(mealRepository, new MealItemFactory());
    }

    MealLogService(MealRepository mealRepository, MealItemFactory mealItemFactory) {
        this.mealRepository = mealRepository;
        this.mealItemFactory = mealItemFactory;
    }

    MealSummary logMeal(String mealName, double totalCarbs, List<MealItem> items) {
        long timestamp = System.currentTimeMillis();
        MealSummary meal = buildMeal(mealName, totalCarbs, timestamp, items, null, null);
        return saveLoggedMeal(totalCarbs, timestamp, meal);
    }

    MealSummary logMeal(String mealName, double totalCarbs, List<MealItem> items, String notes) {
        long timestamp = System.currentTimeMillis();
        MealSummary meal = buildMeal(mealName, totalCarbs, timestamp, items, notes, null);
        return saveLoggedMeal(totalCarbs, timestamp, meal);
    }

    MealSummary logMeal(String mealName, double totalCarbs, List<MealItem> items, String notes, String mealTime) {
        long timestamp = System.currentTimeMillis();
        MealSummary meal = buildMeal(mealName, totalCarbs, timestamp, items, notes, mealTime);
        return saveLoggedMeal(totalCarbs, timestamp, meal);
    }

    MealSummary logMeal(String mealName, double totalCarbs, List<MealItem> items, String notes,
            String mealTime, long timestamp) {
        MealSummary meal = buildMeal(mealName, totalCarbs, timestamp, items, notes, mealTime);
        return saveLoggedMeal(totalCarbs, timestamp, meal);
    }

    private MealSummary saveLoggedMeal(double totalCarbs, long timestamp, MealSummary meal) {
        Treatments treatment = createTreatment(totalCarbs, timestamp);
        meal.treatmentUuid = treatment != null ? treatment.uuid : null;
        mealRepository.saveMeal(meal);
        refreshCharts();
        return meal;
    }

    MealSummary buildMeal(String mealName, double totalCarbs, long timestamp, List<MealItem> items) {
        return buildMeal(mealName, totalCarbs, timestamp, items, null, null);
    }

    MealSummary buildMeal(String mealName, double totalCarbs, long timestamp, List<MealItem> items, String notes) {
        return buildMeal(mealName, totalCarbs, timestamp, items, notes, null);
    }

    MealSummary buildMeal(String mealName, double totalCarbs, long timestamp, List<MealItem> items,
            String notes, String mealTime) {
        MealSummary meal = new MealSummary(mealName, totalCarbs);
        meal.savedAt = timestamp;
        meal.notes = notes;
        meal.mealTime = mealTime == null || mealTime.trim().isEmpty() ? "any" : mealTime;
        meal.items.addAll(mealItemFactory.copyMealItems(items));
        meal.itemCount = meal.items.size();
        return meal;
    }

    protected Treatments createTreatment(double carbs, long timestamp) {
        return Treatments.create(carbs, 0.0, timestamp, UUID.randomUUID().toString());
    }

    protected void refreshCharts() {
        Home.staticRefreshBGCharts();
    }
}