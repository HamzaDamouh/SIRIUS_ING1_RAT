package edu.ezip.ing1.pds.requests;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.ing1.pds.business.dto.MealPlan;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GenerateMealPlanCR extends ClientRequest<Object, MealPlan> {
    public GenerateMealPlanCR(NetworkConfig net, int birth, long userId, double tolerance) throws IOException {
        super(net, birth, buildRequest(userId, tolerance), null, serialize(buildRequest(userId, tolerance)));
    }
    private static Request buildRequest(long userId, double tolerance) throws IOException {
        Map<String,Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("tolerance", tolerance);
        String body = new ObjectMapper().writeValueAsString(m);
        Request r = new Request();
        r.setRequestId(UUID.randomUUID().toString());
        r.setRequestOrder("UPSERT_MEAL_PLAN");
        r.setRequestContent(body);
        return r;
    }
    private static byte[] serialize(Request r) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return om.writerWithDefaultPrettyPrinter().writeValueAsBytes(r);
    }
    @Override public MealPlan readResult(String body) throws IOException {
        return new ObjectMapper().readValue(body, MealPlan.class);
    }
}
