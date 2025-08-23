package edu.ezip.ing1.pds.requests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginCR extends ClientRequest<Object, User> {
    public LoginCR(NetworkConfig net, int birth, String email, String password) throws IOException {
        super(net, birth, buildRequest(email, password), null, serialize(buildRequest(email, password)));
    }

    private static Request buildRequest(String email, String password) throws IOException {
        Map<String,Object> m = new HashMap<>();
        m.put("email", email);
        m.put("password", password);
        String body = new ObjectMapper().writeValueAsString(m);
        Request r = new Request();
        r.setRequestId(UUID.randomUUID().toString());
        r.setRequestOrder("AUTH_USER");
        r.setRequestContent(body);
        return r;
    }

    private static byte[] serialize(Request r) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return om.writerWithDefaultPrettyPrinter().writeValueAsBytes(r);
    }

    @Override
    public User readResult(String body) throws IOException {
        if (body != null && body.contains("\"error\"")) return null;
        return new ObjectMapper().readValue(body, User.class);
    }

}
