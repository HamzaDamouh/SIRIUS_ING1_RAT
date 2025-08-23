package edu.ezip.ing1.pds.business.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ezip.ing1.pds.business.dto.Meal;
import edu.ezip.ing1.pds.business.dto.Meals;
import edu.ezip.ing1.pds.commons.Request;
import edu.ezip.ing1.pds.commons.Response;
import java.sql.*;

public class MealService {

    private final ObjectMapper mapper;

    public MealService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static final String SELECT_MEALS =
            "SELECT id, name, meal_type, kcal, protein_g, carbs_g, fat_g FROM meals";

    public Response selectMeals(final Request req, final Connection conn) throws SQLException, JsonProcessingException {
        // Create an object to hold the list of meals
        final Meals out = new Meals();

        // Declare Statement and ResultSet variables outside the try-with-resources
        Statement st = null;
        ResultSet rs = null;

        try {
            // Create a Statement and execute the query
            st = conn.createStatement();
            rs = st.executeQuery(SELECT_MEALS);

            // Loop through the results and add each meal to the list
            while (rs.next()) {
                Meal currentMeal = mapMeal(rs);
                out.add(currentMeal);
            }
        } finally {
            // Close the resources in a finally block to ensure they are released
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
        }

        // Convert the list of meals to a JSON string and return it in a Response object
        return new Response(req.getRequestId(), mapper.writeValueAsString(out));
    }

    // Helper method to map a row from the ResultSet to a Meal object
    private Meal mapMeal(ResultSet rs) throws SQLException {
        // Create a new Meal object
        final Meal m = new Meal();

        // Set the basic string and integer values
        m.setId(rs.getLong("id"));
        m.setName(rs.getString("name"));
        m.setMealType(rs.getString("meal_type"));
        m.setKcal(rs.getInt("kcal"));

        // Get the nutrient values as generic Objects because they might be null
        Object proteinObject = rs.getObject("protein_g");
        Object carbsObject = rs.getObject("carbs_g");
        Object fatObject = rs.getObject("fat_g");

        // Check for null and cast to Double before setting the value
        if (proteinObject != null) {
            m.setProteinG(((Number) proteinObject).doubleValue());
        } else {
            m.setProteinG(null);
        }

        if (carbsObject != null) {
            m.setCarbsG(((Number) carbsObject).doubleValue());
        } else {
            m.setCarbsG(null);
        }

        if (fatObject != null) {
            m.setFatG(((Number) fatObject).doubleValue());
        } else {
            m.setFatG(null);
        }

        return m;
    }
}