package edu.ezip.ing1.pds.business.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MealPlan {
    private Long id;          // meal_plans.id
    private Long userId;      // user
    private Integer targetKcal;
    private Double tolerance; // 0.10 +- 10%
    private List<DayPlan> week = new ArrayList<>(); // 7 items

    public MealPlan() {}


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getTargetKcal() { return targetKcal; }
    public void setTargetKcal(Integer targetKcal) { this.targetKcal = targetKcal; }
    public Double getTolerance() { return tolerance; }
    public void setTolerance(Double tolerance) { this.tolerance = tolerance; }
    public List<DayPlan> getWeek() { return week; }
    public void setWeek(List<DayPlan> week) { this.week = week; }




    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DayPlan {
        private String day;
        private MealRef breakfast;
        private MealRef lunch;
        private MealRef dinner;
        private MealRef snack;
        private Integer totalKcal;

        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }

        public MealRef getBreakfast() { return breakfast; }
        public void setBreakfast(MealRef breakfast) { this.breakfast = breakfast; }

        public MealRef getLunch() { return lunch; }
        public void setLunch(MealRef lunch) { this.lunch = lunch; }

        public MealRef getDinner() { return dinner; }
        public void setDinner(MealRef dinner) { this.dinner = dinner; }

        public MealRef getSnack() { return snack; }
        public void setSnack(MealRef snack) { this.snack = snack; }

        public Integer getTotalKcal() { return totalKcal; }
        public void setTotalKcal(Integer totalKcal) { this.totalKcal = totalKcal; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MealRef {
        private Long mealId;
        private String name;
        private Integer kcal;

        public Long getMealId() { return mealId; }
        public void setMealId(Long mealId) { this.mealId = mealId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getKcal() { return kcal; }
        public void setKcal(Integer kcal) { this.kcal = kcal; }
    }
}



// NO WRAPPER NEEDED - JSONB