package edu.ezip.ing1.pds.requests;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;

import java.io.IOException;
import java.util.UUID;



public class UpdateUserCR extends ClientRequest<User, User> {
    public UpdateUserCR(NetworkConfig net, int birth, User updated) throws IOException {
        super(net, birth, buildRequest(updated), updated, serialize(buildRequest(updated)));
    }
    private static Request buildRequest(User u) throws IOException {
        String body = new ObjectMapper().writeValueAsString(u);
        Request r = new Request();
        r.setRequestId(UUID.randomUUID().toString());
        r.setRequestOrder("UPDATE_USER");
        r.setRequestContent(body);
        return r;
    }
    private static byte[] serialize(Request r) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.enable(SerializationFeature.WRAP_ROOT_VALUE);
        return om.writerWithDefaultPrettyPrinter().writeValueAsBytes(r);
    }
    @Override public User readResult(String body) throws IOException {
        if (body != null && body.contains("\"error\"")) return null;
        return new ObjectMapper().readValue(body, User.class);
    }
}
