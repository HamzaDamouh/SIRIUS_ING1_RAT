package edu.ezip.ing1.pds.requests;

import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.ing1.pds.commons.Request;

import java.io.IOException;
import java.util.UUID;

public class RegisterUserCR extends ClientRequest<User, User> {

    public RegisterUserCR(NetworkConfig net, int birth, User payload) throws IOException {
        super(net, birth, buildRequest(payload), payload, serialize(buildRequest(payload)));
    }

    private static Request buildRequest(User u) {
        Request r = new Request();
        r.setRequestId(UUID.randomUUID().toString());
        r.setRequestOrder("INSERT_USER");
        try {
            r.setRequestContent(new ObjectMapper().writeValueAsString(u));
        } catch (Exception ignored) {}
        return r;
    }

    private static byte[] serialize(Request r) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return om.writerWithDefaultPrettyPrinter().writeValueAsBytes(r);
    }

    @Override
    public User readResult(String body) throws IOException {
        return new ObjectMapper().readValue(body, User.class);
    }

}
