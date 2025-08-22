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

    public MealPlanService(ObjectMapper mapper) { this.mapper = mapper; }

    // SQL
    private static final String GET_USER_BY_ID =
            "SELECT id, email, full_name, height_cm, weight_kg, sex, daily_kcal_target FROM users WHERE id = ?";

    private static final String SELECT_MEALS =
            "SELECT id, name, meal_type, kcal FROM meals"; // keep minimal for planning

    private static final String UPSERT_MEAL_PLAN =
            "INSERT INTO meal_plans (user_id, plan) VALUES (?, ?::jsonb) " +
                    "ON CONFLICT (user_id) DO UPDATE SET plan = EXCLUDED.plan";

    private static final String GET_MEAL_PLAN =
            "SELECT plan FROM meal_plans WHERE user_id = ?";

    // Endpoints
    
    public Response generateAndUpsertMealPlan(final Request req, final Connection conn) throws SQLException, IOException {
        final MealPlan genReq = mapper.readValue(req.getRequestBody(), MealPlan.class);
        final long userId = genReq.getUserId();
        final double tolerance = genReq.getTolerance() == null ? 0.10 : genReq.getTolerance();

        final User user = loadUser(conn, userId);
        if (user == null) return err(req, "user_not_found");
        final int target = user.getDailyKcalTarget() == null ? 2000 : user.getDailyKcalTarget();

        final Map<String, List<Meal>> byType = loadMealsByType(conn);
        final MealPlan plan = buildWeeklyPlan(userId, target, tolerance, byType);

        final String json = mapper.writeValueAsString(planToJson(plan));
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_MEAL_PLAN)) {
            ps.setLong(1, userId);
            ps.setString(2, json);
            ps.executeUpdate();
        }
        return ok(req, plan);
    }

    public Response getMealPlan(final Request req, final Connection conn) throws SQLException, IOException {
        final MealPlan in = mapper.readValue(req.getRequestBody(), MealPlan.class);
        try (PreparedStatement ps = conn.prepareStatement(GET_MEAL_PLAN)) {
            ps.setLong(1, in.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final String json = rs.getString(1);
                    return new Response(req.getRequestId(), json); // return raw JSON plan
                }
            }
        }
        return err(req, "no_plan");
    }


    // networking + debugging helpers
    private Response ok(Request req, Object body) throws JsonProcessingException {
        return new Response(req.getRequestId(), mapper.writeValueAsString(body));
    }
    private Response err(Request req, String code) {
        return new Response(req.getRequestId(), "{\"error\":\"" + code + "\"}");
    }



    private User loadUser(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(GET_USER_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                User u = new User();
                u.setId(rs.getLong("id"));
                u.setDailyKcalTarget((Integer) ((Number)rs.getObject("daily_kcal_target")).intValue());
                return u;
            }
        }
    }

    private Map<String, List<Meal>> loadMealsByType(Connection conn) throws SQLException {
        Map<String, List<Meal>> byType = new HashMap<>();
        byType.put("breakfast", new ArrayList<>());
        byType.put("lunch", new ArrayList<>());
        byType.put("dinner", new ArrayList<>());
        byType.put("snack", new ArrayList<>());
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(SELECT_MEALS)) {
            while (rs.next()) {
                Meal m = new Meal();
                m.setId(rs.getLong("id"));
                m.setName(rs.getString("name"));
                m.setMealType(rs.getString("meal_type"));
                m.setKcal(rs.getInt("kcal"));
                List<Meal> list = byType.get(m.getMealType());
                if (list != null) list.add(m);
            }
        }
        return byType;
    }


    // business helpers

    private MealPlan buildWeeklyPlan(long userId, int target, double tolerance, Map<String, List<Meal>> byType) {
        final int min = (int)Math.round(target * (1.0 - tolerance));
        final int max = (int)Math.round(target * (1.0 + tolerance));

        MealPlan plan = new MealPlan();
        plan.setUserId(userId);
        plan.setTargetKcal(target);
        plan.setTolerance(tolerance);

        final String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        for (String d : days) {
            MealPlan.DayPlan dp = new MealPlan.DayPlan();
            dp.setDay(d);

            int attempts = 0;
            while (attempts++ < 40) {
                Meal b = pick(byType.get("breakfast"));
                Meal l = pick(byType.get("lunch"));
                Meal di = pick(byType.get("dinner"));
                Meal s = pick(byType.get("snack"));

                int total = kcal(b) + kcal(l) + kcal(di) + kcal(s);
                if (total >= min && total <= max || attempts >= 40) {
                    dp.setBreakfast(ref(b));
                    dp.setLunch(ref(l));
                    dp.setDinner(ref(di));
                    dp.setSnack(ref(s));
                    dp.setTotalKcal(total);
                    break;
                }
            }
            plan.getWeek().add(dp);
        }
        return plan;
    }

    private int kcal(Meal m) { return m == null ? 0 : (m.getKcal() == null ? 0 : m.getKcal()); }
    private Meal pick(List<Meal> list) { return (list == null || list.isEmpty()) ? null : list.get(rnd.nextInt(list.size())); }

    private MealPlan.MealRef ref(Meal m) {
        if (m == null) return null;
        MealPlan.MealRef r = new MealPlan.MealRef();
        r.setMealId(m.getId());
        r.setName(m.getName());
        r.setKcal(m.getKcal());
        return r;
    }

    // compact JSON for DB storage
    private Map<String, Object> planToJson(MealPlan p) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("target_kcal", p.getTargetKcal());
        root.put("tolerance", p.getTolerance());
        List<Map<String, Object>> week = new ArrayList<>();
        for (MealPlan.DayPlan dp : p.getWeek()) {
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("day", dp.getDay());
            day.put("breakfast", refToMap(dp.getBreakfast()));
            day.put("lunch", refToMap(dp.getLunch()));
            day.put("dinner", refToMap(dp.getDinner()));
            day.put("snack", refToMap(dp.getSnack()));
            day.put("total_kcal", dp.getTotalKcal());
            week.add(day);
        }
        root.put("week", week);
        return root;
    }
    private Map<String, Object> refToMap(MealPlan.MealRef r) {
        if (r == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("meal_id", r.getMealId());
        m.put("name", r.getName());
        m.put("kcal", r.getKcal());
        return m;
    }
}
