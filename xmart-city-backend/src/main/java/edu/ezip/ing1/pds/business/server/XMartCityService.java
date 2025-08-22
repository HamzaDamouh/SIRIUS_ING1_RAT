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

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserService userService = new UserService(mapper);
    private final MealService mealService = new MealService(mapper);
    private final MealPlanService mealPlanService = new MealPlanService(mapper);


    public static XMartCityService inst = null;
    public static final XMartCityService getInstance()  {
        if(inst == null) {
            inst = new XMartCityService();
        }
        return inst;
    }
    private XMartCityService() {}

    public final Response dispatch(final Request request, final Connection connection)
            throws InvocationTargetException, IllegalAccessException, SQLException, IOException {
        Response response = null;

        final String lesgoo = request.getRequestOrder();

        switch(lesgoo) {

            // User Service
            case "INSERT_USER":    return userService.insertUser(request, connection);
            case "AUTH_USER":      return userService.authUser(request, connection);
            case "GET_USER_BY_ID": return userService.getUserById(request, connection);
            case "UPDATE_USER":    return userService.updateUser(request, connection);

            // Meals Service
            case "SELECT_MEALS":   return mealService.selectMeals(request, connection);

            // Meal Plans Service
            case "UPSERT_MEAL_PLAN": return mealPlanService.generateAndUpsertMealPlan(request, connection); // generate & save
            case "GET_MEAL_PLAN":    return mealPlanService.getMealPlan(request, connection);

            default:
                logger.warn("Unknown request order: {}", lesgoo);
                return new Response(request.getRequestId(), "{\"error\":\"unknown_request\"}");
        }
    }



}
