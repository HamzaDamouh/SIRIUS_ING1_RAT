package edu.ezip.ing1.pds.requests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.ing1.pds.business.dto.MealPlan;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class GetMealPlanCR extends ClientRequest<Object, MealPlan> {
    public GetMealPlanCR(NetworkConfig net, int birth, long userId) throws IOException {
        super(net, birth, buildRequest(userId), null, serialize(buildRequest(userId)));
    }
    private static Request buildRequest(long userId) throws IOException {
        Map<String,Object> m = Map.of("userId", userId);
        String body = new ObjectMapper().writeValueAsString(m);
        Request r = new Request();
        r.setRequestId(UUID.randomUUID().toString());
        r.setRequestOrder("GET_MEAL_PLAN");
        r.setRequestContent(body);
        return r;
    }
    private static byte[] serialize(Request r) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return om.writerWithDefaultPrettyPrinter().writeValueAsBytes(r);
    }
    @Override public MealPlan readResult(String body) throws IOException {
        Map<String,Object> m = new ObjectMapper().readValue(body, new TypeReference<Map<String,Object>>(){});
        MealPlan plan = new MealPlan();
        plan.setTargetKcal(((Number)m.getOrDefault("target_kcal", 0)).intValue());
        plan.setTolerance(Double.valueOf(m.getOrDefault("tolerance", 0.10).toString()));
        return plan;
    }
}