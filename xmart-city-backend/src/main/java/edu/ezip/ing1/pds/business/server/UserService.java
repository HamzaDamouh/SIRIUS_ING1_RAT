package edu.ezip.ing1.pds.business.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.commons.Request;
import edu.ezip.ing1.pds.commons.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;

public class UserService {

    private final ObjectMapper mapper;
    public UserService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // SQL Statements
    private static final String INSERT_USER =
            "INSERT INTO users (email, password_hash, full_name, height_cm, weight_kg, sex, age, activity_level, daily_kcal_target) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

    private static final String AUTH_USER =
            "SELECT id, email, full_name, height_cm, weight_kg, sex, age, activity_level, daily_kcal_target " +
                    "FROM users WHERE email = ? AND password_hash = ?";

    private static final String GET_USER_BY_ID =
            "SELECT id, email, full_name, height_cm, weight_kg, sex, age, activity_level, daily_kcal_target " +
                    "FROM users WHERE id = ?";

    private static final String UPDATE_USER =
            "UPDATE users SET full_name = ?, height_cm = ?, weight_kg = ?, sex = ?, age = ?, activity_level = ?, daily_kcal_target = ? " +
                    "WHERE id = ?";

    // Endpoints

    public Response insertUser(final Request req, final Connection conn) throws IOException, SQLException {
        final User user = mapper.readValue(req.getRequestBody(), User.class);
        final int computedTarget = computeDailyTarget(user);

        try (PreparedStatement ps = conn.prepareStatement(INSERT_USER)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setBigDecimal(4, toBigDec(user.getHeightCm()));
            ps.setBigDecimal(5, toBigDec(user.getWeightKg()));
            ps.setString(6, user.getSex());
            ps.setObject(7, user.getAge(), Types.INTEGER);
            ps.setString(8, safeActivity(user.getActivityLevel()));
            ps.setObject(9, computedTarget, Types.INTEGER);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
        }

        user.setPassword(null);
        user.setDailyKcalTarget(computedTarget);
        user.setBmi(computeBmi(user.getHeightCm(), user.getWeightKg()));
        return ok(req, user);
    }

    public Response authUser(final Request req, final Connection conn) throws IOException, SQLException {
        final User login = mapper.readValue(req.getRequestBody(), User.class);
        try (PreparedStatement ps = conn.prepareStatement(AUTH_USER)) {
            ps.setString(1, login.getEmail());
            ps.setString(2, login.getPassword());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final User user = mapUser(rs);
                    user.setBmi(computeBmi(user.getHeightCm(), user.getWeightKg()));
                    return ok(req, user);
                }
            }
        }
        return err(req, "not_found");
    }

    public Response getUserById(final Request req, final Connection conn) throws SQLException, IOException {
        final User requestUser = mapper.readValue(req.getRequestBody(), User.class);
        try (PreparedStatement ps = conn.prepareStatement(GET_USER_BY_ID)) {
            ps.setLong(1, requestUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final User user = mapUser(rs);
                    user.setBmi(computeBmi(user.getHeightCm(), user.getWeightKg()));
                    return ok(req, user);
                }
            }
        }
        return err(req, "not_found");
    }

    public Response updateUser(final Request req, final Connection conn) throws SQLException, IOException {
        final User userToUpdate = mapper.readValue(req.getRequestBody(), User.class);
        final int computedTarget = computeDailyTarget(userToUpdate);

        try (PreparedStatement ps = conn.prepareStatement(UPDATE_USER)) {
            ps.setString(1, userToUpdate.getFullName());
            ps.setBigDecimal(2, toBigDec(userToUpdate.getHeightCm()));
            ps.setBigDecimal(3, toBigDec(userToUpdate.getWeightKg()));
            ps.setString(4, userToUpdate.getSex());
            ps.setObject(5, userToUpdate.getAge(), Types.INTEGER);
            ps.setString(6, safeActivity(userToUpdate.getActivityLevel()));
            ps.setObject(7, computedTarget, Types.INTEGER);
            ps.setLong(8, userToUpdate.getId());
            ps.executeUpdate();
        }

        // Return the updated user by fetching it again from the database
        return getUserById(new Request(), conn);
    }

    // networking + debugging helpers

    private Response ok(Request req, Object body) throws JsonProcessingException {
        // Convert the body object to a JSON string and create a success response
        String jsonBody = mapper.writeValueAsString(body);
        return new Response(req.getRequestId(), jsonBody);
    }

    private Response err(Request req, String code) {
        // Create an error response with a simple JSON object
        return new Response(req.getRequestId(), "{\"error\":\"" + code + "\"}");
    }

    // conversion helpers

    private BigDecimal toBigDec(Double d) {
        if (d == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(d));
    }

    private Double toDouble(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return Double.valueOf(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Object getAs(Object o, Class<?> target) {
        if (o == null) {
            return null;
        }
        if (target == Integer.class && o instanceof Number) {
            return ((Number) o).intValue();
        }
        return o;
    }

    // business helpers

    private User mapUser(ResultSet rs) throws SQLException {
        // Create a new User object and populate it with data from the ResultSet
        final User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setHeightCm(toDouble(rs.getObject("height_cm")));
        user.setWeightKg(toDouble(rs.getObject("weight_kg")));
        user.setSex(rs.getString("sex"));

        // Get and cast the daily kcal target, handling potential nulls
        Object dailyKcalObject = rs.getObject("daily_kcal_target");
        if (dailyKcalObject instanceof Number) {
            user.setDailyKcalTarget(((Number) dailyKcalObject).intValue());
        } else {
            user.setDailyKcalTarget(null);
        }
        return user;
    }

    private Double computeBmi(Double heightCm, Double weightKg) {
        // Check for invalid input to prevent errors
        if (heightCm == null || heightCm <= 0 || weightKg == null || weightKg <= 0) {
            return null;
        }
        // Convert height to meters for the calculation
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
        // Round the result to two decimal places
        return Math.round(bmi * 100.0) / 100.0;
    }

    // Check if the activity level is a valid option, otherwise default to "sedentary"
    private String safeActivity(String activityLevel) {
        if (activityLevel == null) {
            return "sedentary";
        }

        switch (activityLevel) {
            case "light":
            case "moderate":
            case "active":
            case "sedentary":
                return activityLevel;
            default:
                return "sedentary";
        }
    }

    private int computeDailyTarget(User u) {
        // Use default values if user data is missing
        double kg = (u.getWeightKg() == null) ? 70.0 : u.getWeightKg();
        double cm = (u.getHeightCm() == null) ? 170.0 : u.getHeightCm();
        int age = (u.getAge() == null) ? 30 : u.getAge();
        String sex = (u.getSex() == null) ? "O" : u.getSex();

        // Calculate Basal Metabolic Rate (BMR) using Mifflin-St Jeor equation
        double bmr;
        if ("M".equals(sex)) {
            bmr = 10 * kg + 6.25 * cm - 5 * age + 5;
        } else if ("F".equals(sex)) {
            bmr = 10 * kg + 6.25 * cm - 5 * age - 161;
        } else {
            // For other/unknown sex
            bmr = 10 * kg + 6.25 * cm - 5 * age - 78;
        }

        // Determine the activity factor
        double factor;
        String activity = safeActivity(u.getActivityLevel());
        if ("light".equals(activity)) {
            factor = 1.375;
        } else if ("moderate".equals(activity)) {
            factor = 1.55;
        } else if ("active".equals(activity)) {
            factor = 1.725;
        } else {
            // Default to sedentary
            factor = 1.20;
        }

        // Calculate Total Daily Energy Expenditure (TDEE)
        int tdee = (int) Math.round(bmr * factor);

        // Ensure the target is within a safe range
        int minTarget = 1200;
        int maxTarget = 5000;
        int finalTarget;

        if (tdee < minTarget) {
            finalTarget = minTarget;
        } else if (tdee > maxTarget) {
            finalTarget = maxTarget;
        } else {
            finalTarget = tdee;
        }
        return finalTarget;
    }
}