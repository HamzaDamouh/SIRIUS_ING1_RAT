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
    public UserService(ObjectMapper mapper) {this.mapper = mapper;}

    // SQL
    private static final String INSERT_USER =
            "INSERT INTO users (email, password_hash, full_name, height_cm, weight_kg, sex, daily_kcal_target) "+
            "VALUES (?,?,?,?,?,?,?) RETURNING id";

    private static final String AUTH_USER =
            "SELECT mail, password_hash, full_name, height_cm, weight_kg, sex, daily_kcal_target "+
             "FROM users WHERE email=? AND password_hash=?";

    private static final String GET_USER_BY_ID =
            "SELECT id, email, full_name, height_cm, weight_kg, sex, daily_kcal_target FROM users WHERE id = ?";

    private static final String UPDATE_USER =
            "UPDATE users SET full_name = ?, height_cm = ?, weight_kg = ?, sex = ?, daily_kcal_target = ? WHERE id = ? ";

    // Endpoints

    public Response insertUser(final Request req, final Connection conn) throws IOException, SQLException {
        final User user = mapper.readValue(req.getRequestBody(), User.class);

        try (PreparedStatement ps = conn.prepareStatement(INSERT_USER)){
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setBigDecimal(4, toBigDec(user.getHeightCm()));
            ps.setBigDecimal(5, toBigDec(user.getWeightKg()));
            ps.setString(6, user.getSex());
            ps.setObject(7, user.getDailyKcalTarget(), Types.INTEGER);

            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()) user.setId(rs.getLong(1));
            }
        }

        user.setPassword(null);
        user.setBmi(computeBmi(user.getHeightCm(), user.getWeightKg()));
        return ok(req, user);
    }


    public Response authUser(final Request req, final Connection conn) throws IOException, SQLException {
        final User login = mapper.readValue(req.getRequestBody(), User.class);
        try (PreparedStatement ps = conn.prepareStatement(AUTH_USER)){
            ps.setString(1, login.getEmail());
            ps.setString(2, login.getPassword());
            try (ResultSet rs = ps.executeQuery()){
                if (rs.next()){
                    final User u = mapUser(rs);
                    u.setBmi(computeBmi(u.getHeightCm(), u.getWeightKg()));
                    return ok(req, u);
                }
            }
        }
        return err(req, "not_found");
    }

    public Response getUserById(final Request req, final Connection conn) throws SQLException, IOException {
        final User r = mapper.readValue(req.getRequestBody(), User.class);
        try (PreparedStatement ps = conn.prepareStatement(GET_USER_BY_ID)) {
            ps.setLong(1, r.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    final User u = mapUser(rs);
                    u.setBmi(computeBmi(u.getHeightCm(), u.getWeightKg()));
                    return ok(req, u);
                }
            }
        }
        return err(req, "not_found");
    }

    public Response updateUser(final Request req, final Connection conn) throws SQLException, IOException {
        final User u = mapper.readValue(req.getRequestBody(), User.class);
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_USER)) {
            ps.setString(1, u.getFullName());
            ps.setBigDecimal(2, toBigDec(u.getHeightCm()));
            ps.setBigDecimal(3, toBigDec(u.getWeightKg()));
            ps.setString(4, u.getSex());
            ps.setObject(5, u.getDailyKcalTarget(), Types.INTEGER);
            ps.setLong(6, u.getId());
            ps.executeUpdate();
        }
        return getUserById(new Request(), conn);
    }


    // networking + debugging helpers

    // no check
    private Response ok(Request req, Object body) throws JsonProcessingException {
        return new Response(req.getRequestId(), mapper.writeValueAsString(body));
    }

    private Response err(Request req, String code) {
        return new Response(req.getRequestId(), "{\"error\":\"" + code + "\"}");
    }


    // conversion helpers

    private BigDecimal toBigDec(Double d) {
        return d == null ? null : new BigDecimal(String.valueOf(d));
    }

    private Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.valueOf(o.toString()); } catch (Exception e) { return null; }
    }

    private Object getAs(Object o, Class<?> target) {
        if (o == null) return null;
        if (target == Integer.class && o instanceof Number) return ((Number)o).intValue();
        return o;
    }


    // business helpers

    private User mapUser(ResultSet rs) throws SQLException {
        final User u = new User();
        u.setId(rs.getLong("id"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setHeightCm(toDouble(rs.getObject("height_cm")));
        u.setWeightKg(toDouble(rs.getObject("weight_kg")));
        u.setSex(rs.getString("sex"));
        u.setDailyKcalTarget((Integer) getAs(rs.getObject("daily_kcal_target"), Integer.class));
        return u;
    }

    private Double computeBmi(Double heightCm, Double weightKg) {
        if (heightCm == null || heightCm <= 0 || weightKg == null || weightKg <= 0) return null;
        double h = heightCm / 100.0;
        return Math.round((weightKg / (h*h)) * 100.0) / 100.0;
    }



}
