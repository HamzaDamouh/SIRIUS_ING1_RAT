package edu.ezip.ing1.pds.business.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ezip.ing1.pds.commons.Request;
import edu.ezip.ing1.pds.commons.Response;

import edu.ezip.ing1.pds.business.dto.Meal;
import edu.ezip.ing1.pds.business.dto.MealPlan;
import edu.ezip.ing1.pds.business.dto.User;

import java.io.IOException;
import java.util.*;
import java.sql.*;

public class MealPlanService {

    private final ObjectMapper mapper;
    private final Random rnd = new Random();

    public MealPlanService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // SQL Statements
    private static final String GET_USER_BY_ID =
            "SELECT id, email, full_name, height_cm, weight_kg, sex, daily_kcal_target FROM users WHERE id = ?";

    private static final String SELECT_MEALS =
            "SELECT id, name, meal_type, kcal FROM meals";

    private static final String UPSERT_MEAL_PLAN =
            "INSERT INTO meal_plans (user_id, plan) VALUES (?, ?::jsonb) " +
                    "ON CONFLICT (user_id) DO UPDATE SET plan = EXCLUDED.plan";

    private static final String GET_MEAL_PLAN =
            "SELECT plan FROM meal_plans WHERE user_id = ?";

    // Endpoints
    public Response generateAndUpsertMealPlan(final Request req, final Connection conn) throws SQLException, IOException {
        // Read the request body to get the meal plan generation request
        final MealPlan generationRequest = mapper.readValue(req.getRequestBody(), MealPlan.class);
        final long userId = generationRequest.getUserId();

        // Use a default tolerance if none is provided
        final double tolerance;
        if (generationRequest.getTolerance() == null) {
            tolerance = 0.10;
        } else {
            tolerance = generationRequest.getTolerance();
        }

        // Load the user data from the database
        final User user = loadUser(conn, userId);
        if (user == null) {
            return err(req, "user_not_found");
        }

        // Use a default target if the user's daily kcal target is null
        final int targetKcal;
        if (user.getDailyKcalTarget() == null) {
            targetKcal = 2000;
        } else {
            targetKcal = user.getDailyKcalTarget();
        }

        // Load all available meals from the database, grouped by meal type
        final Map<String, List<Meal>> mealsByType = loadMealsByType(conn);

        // Build a new meal plan for the week
        final MealPlan plan = buildWeeklyPlan(userId, targetKcal, tolerance, mealsByType);

        // Convert the plan to a JSON string for database storage
        final String jsonPlan = mapper.writeValueAsString(planToJson(plan));

        // Use a prepared statement to insert or update the meal plan
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_MEAL_PLAN)) {
            ps.setLong(1, userId);
            ps.setString(2, jsonPlan);
            ps.executeUpdate();
        }

        // Return a successful response with the newly generated plan
        return ok(req, plan);
    }

    public Response getMealPlan(final Request req, final Connection conn) throws SQLException, IOException {
        // Read the request body to get the user ID
        final MealPlan inputPlan = mapper.readValue(req.getRequestBody(), MealPlan.class);

        // Prepare the SQL statement to get the meal plan
        try (PreparedStatement ps = conn.prepareStatement(GET_MEAL_PLAN)) {
            ps.setLong(1, inputPlan.getUserId());

            // Execute the query and get the result
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // If a plan is found, get the JSON string and return it in the response
                    final String json = rs.getString(1);
                    return new Response(req.getRequestId(), json);
                }
            }
        }

        // If no plan is found, return an error
        return err(req, "no_plan");
    }

    // Networking and debugging helpers
    private Response ok(Request req, Object body) throws JsonProcessingException {
        // Convert the body object to a JSON string and create a success response
        String jsonBody = mapper.writeValueAsString(body);
        return new Response(req.getRequestId(), jsonBody);
    }

    private Response err(Request req, String code) {
        // Create an error response with a simple JSON object
        return new Response(req.getRequestId(), "{\"error\":\"" + code + "\"}");
    }

    // Data loading from database
    private User loadUser(Connection conn, long id) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        User user = null;

        try {
            ps = conn.prepareStatement(GET_USER_BY_ID);
            ps.setLong(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                user = new User();
                user.setId(rs.getLong("id"));

                // Handle the daily_kcal_target, which could be stored as a different number type
                Object kcalObject = rs.getObject("daily_kcal_target");
                if (kcalObject instanceof Number) {
                    Number kcalNumber = (Number) kcalObject;
                    user.setDailyKcalTarget(kcalNumber.intValue());
                }
            }
        } finally {
            // Ensure resources are closed properly
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        }
        return user;
    }

    private Map<String, List<Meal>> loadMealsByType(Connection conn) throws SQLException {
        // Create a map to store meals, categorized by type
        Map<String, List<Meal>> byType = new HashMap<>();
        byType.put("breakfast", new ArrayList<>());
        byType.put("lunch", new ArrayList<>());
        byType.put("dinner", new ArrayList<>());
        byType.put("snack", new ArrayList<>());

        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery(SELECT_MEALS);

            while (rs.next()) {
                Meal meal = new Meal();
                meal.setId(rs.getLong("id"));
                meal.setName(rs.getString("name"));
                meal.setMealType(rs.getString("meal_type"));
                meal.setKcal(rs.getInt("kcal"));

                // Add the meal to the correct list in the map
                List<Meal> mealList = byType.get(meal.getMealType());
                if (mealList != null) {
                    mealList.add(meal);
                }
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
        }
        return byType;
    }


    // Business logic helpers

    private MealPlan buildWeeklyPlan(long userId, int target, double tolerance, Map<String, List<Meal>> mealsByType) {
        // Calculate the allowed min and max kcal values based on the tolerance
        final int minKcal = (int) Math.round(target * (1.0 - tolerance));
        final int maxKcal = (int) Math.round(target * (1.0 + tolerance));

        MealPlan plan = new MealPlan();
        plan.setUserId(userId);
        plan.setTargetKcal(target);
        plan.setTolerance(tolerance);

        final String[] daysOfTheWeek = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        // Loop through each day of the week to create a plan
        for (String day : daysOfTheWeek) {
            MealPlan.DayPlan dayPlan = new MealPlan.DayPlan();
            dayPlan.setDay(day);

            int attempts = 0;
            // Try to find a combination of meals that fits the calorie range
            while (attempts < 40) {
                attempts++;

                // Pick a random meal for each meal type
                Meal breakfast = pickRandomMeal(mealsByType.get("breakfast"));
                Meal lunch = pickRandomMeal(mealsByType.get("lunch"));
                Meal dinner = pickRandomMeal(mealsByType.get("dinner"));
                Meal snack = pickRandomMeal(mealsByType.get("snack"));

                // Calculate the total calories for the day
                int totalCalories = getKcal(breakfast) + getKcal(lunch) + getKcal(dinner) + getKcal(snack);

                // If the total is within the target range, or we've reached max attempts, use this plan
                if ((totalCalories >= minKcal && totalCalories <= maxKcal) || attempts >= 40) {
                    dayPlan.setBreakfast(createMealRef(breakfast));
                    dayPlan.setLunch(createMealRef(lunch));
                    dayPlan.setDinner(createMealRef(dinner));
                    dayPlan.setSnack(createMealRef(snack));
                    dayPlan.setTotalKcal(totalCalories);
                    break;
                }
            }
            plan.getWeek().add(dayPlan);
        }
        return plan;
    }

    private int getKcal(Meal m) {
        if (m == null) {
            return 0;
        }
        if (m.getKcal() == null) {
            return 0;
        }
        return m.getKcal();
    }

    private Meal pickRandomMeal(List<Meal> mealList) {
        // Return null if the list is empty or doesn't exist
        if (mealList == null || mealList.isEmpty()) {
            return null;
        }
        // Pick a random meal from the list
        int randomIndex = rnd.nextInt(mealList.size());
        return mealList.get(randomIndex);
    }

    private MealPlan.MealRef createMealRef(Meal m) {
        if (m == null) {
            return null;
        }
        MealPlan.MealRef mealRef = new MealPlan.MealRef();
        mealRef.setMealId(m.getId());
        mealRef.setName(m.getName());
        mealRef.setKcal(m.getKcal());
        return mealRef;
    }

    // Compact JSON for database storage
    private Map<String, Object> planToJson(MealPlan p) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("target_kcal", p.getTargetKcal());
        root.put("tolerance", p.getTolerance());

        List<Map<String, Object>> week = new ArrayList<>();
        // Loop through each day plan and convert it to a map
        for (MealPlan.DayPlan dayPlan : p.getWeek()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", dayPlan.getDay());
            day.put("breakfast", convertRefToMap(dayPlan.getBreakfast()));
            day.put("lunch", convertRefToMap(dayPlan.getLunch()));
            day.put("dinner", convertRefToMap(dayPlan.getDinner()));
            day.put("snack", convertRefToMap(dayPlan.getSnack()));
            day.put("total_kcal", dayPlan.getTotalKcal());
            week.add(day);
        }
        root.put("week", week);
        return root;
    }

    private Map<String, Object> convertRefToMap(MealPlan.MealRef ref) {
        if (ref == null) {
            return null;
        }
        Map<String, Object> mealMap = new LinkedHashMap<>();
        mealMap.put("meal_id", ref.getMealId());
        mealMap.put("name", ref.getName());
        mealMap.put("kcal", ref.getKcal());
        return mealMap;
    }
}