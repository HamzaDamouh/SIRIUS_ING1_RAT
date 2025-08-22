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

    public MealService(ObjectMapper mapper) { this.mapper = mapper; }

    private static final String SELECT_MEALS =
            "SELECT id, name, meal_type, kcal, protein_g, carbs_g, fat_g FROM meals";



    public Response selectMeals(final Request req, final Connection conn) throws SQLException, JsonProcessingException {
        final Meals out = new Meals();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SELECT_MEALS)) {
            while (rs.next()) out.add(mapMeal(rs));
        }
        return new Response(req.getRequestId(), mapper.writeValueAsString(out));
    }



    // mapper
    private Meal mapMeal(ResultSet rs) throws SQLException {
        final Meal m = new Meal();
        m.setId(rs.getLong("id"));
        m.setName(rs.getString("name"));
        m.setMealType(rs.getString("meal_type"));
        m.setKcal(rs.getInt("kcal"));
        Object p = rs.getObject("protein_g"), c = rs.getObject("carbs_g"), f = rs.getObject("fat_g");
        m.setProteinG(p == null ? null : ((Number) p).doubleValue());
        m.setCarbsG(c == null ? null : ((Number) c).doubleValue());
        m.setFatG(f == null ? null : ((Number) f).doubleValue());
        return m;

    }
}
