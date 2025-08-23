package edu.ezip.ing1.pds.requests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.ing1.pds.business.dto.Meals;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;

import java.io.IOException;
import java.util.UUID;

public class SelectMealsCR extends ClientRequest <Object, Meals>{
    public SelectMealsCR(NetworkConfig net, int birth) throws IOException {
        super(net, birth, buildRequest(), null, serialize(buildRequest()));
    }
    private static Request buildRequest() {
        Request r = new Request();
        r.setRequestId(UUID.randomUUID().toString());
        r.setRequestOrder("SELECT_MEALS");
        return r;
    }
    private static byte[] serialize(Request r) throws IOException {
        com.fasterxml.jackson.databind.ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return om.writerWithDefaultPrettyPrinter().writeValueAsBytes(r);
    }
    @Override
    public Meals readResult(String body) throws IOException {
        return new ObjectMapper().readValue(body, Meals.class);
    }
}
