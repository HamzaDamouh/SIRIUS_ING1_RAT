package edu.ezip.ing1.pds.business.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ezip.ing1.pds.commons.Request;
import edu.ezip.ing1.pds.commons.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;

public class XMartCityService {

    private final static String LoggingLabel = "B u s i n e s s - S e r v e r";
    private final Logger logger = LoggerFactory.getLogger(LoggingLabel);

    private enum Queries {
        SELECT_STUDENTS("SELECT t.name, t.firstname, t.groupname FROM students t"),
        INSERT_STUDENT("INSERT into students (name, firstname, groupname) values (?, ?, ?)");
        private final String query;

        private Queries(final String query) {
            this.query = query;
        }
    }

    public static XMartCityService inst = null;
    public static final XMartCityService getInstance()  {
        if(inst == null) {
            inst = new XMartCityService();
        }
        return inst;
    }

    private XMartCityService() {

    }

    public final Response dispatch(final Request request, final Connection connection)
            throws InvocationTargetException, IllegalAccessException, SQLException, IOException {
        Response response = null;

        final Queries queryEnum = Enum.valueOf(Queries.class, request.getRequestOrder());
        switch(queryEnum) {
            case SELECT_STUDENTS:
                response = SelectAllStudents(request, connection);
                break;
            case INSERT_STUDENT:
                response = InsertStudent(request, connection);
                break;
            default:
                break;
        }

        return response;
    }

    private Response InsertStudent(final Request request, final Connection connection) throws SQLException, IOException {

        final ObjectMapper mapper = new ObjectMapper();

        Student student = mapper.readValue(request.getRequestBody(), Student.class);

        PreparedStatement pstmt = connection.prepareStatement(Queries.INSERT_STUDENT.query);

        pstmt.setString(1, student.getName());
        pstmt.setString(2, student.getFirstname());
        pstmt.setString(3, student.getGroup());
        pstmt.executeUpdate();


        return new Response(request.getRequestId(), "\"Student inserted successfully\"");
    }


    private Response SelectAllStudents(final Request request, final Connection connection) throws SQLException, JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final Statement stmt = connection.createStatement();
        final ResultSet res = stmt.executeQuery(Queries.SELECT_STUDENTS.query);
        Students students = new Students();
        while (res.next()) {
            Student student = new Student();
            student.setName(res.getString(1));
            student.setFirstname(res.getString(2));
            student.setGroup(res.getString(3));
            students.add(student);
        }
        return new Response(request.getRequestId(), objectMapper.writeValueAsString(students));

    }

}
