package edu.ezip.ing1.pds.services;

import edu.ezip.ing1.pds.business.dto.User;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.requests.GetUserByIdCR;
import edu.ezip.ing1.pds.requests.LoginCR;
import edu.ezip.ing1.pds.requests.RegisterUserCR;
import edu.ezip.ing1.pds.requests.UpdateUserCR;

import java.io.IOException;

public class UserClientService {

    private final NetworkConfig net;
    public UserClientService(NetworkConfig net) {this.net = net;}

    public User register(User u) throws Exception {
        var req = new RegisterUserCR(net,0,u);
        req.join();
        return req.getResult();
    }
    public User login(String email, String password) throws Exception {
        var req = new LoginCR(net,0,email,password);
        req.join();
        return req.getResult();
    }
    public User getById(long id) throws Exception {
        var req = new GetUserByIdCR(net,0,id);
        req.join();
        return req.getResult();
    }
    public User update(User u) throws Exception {
        var req = new UpdateUserCR(net,0,u);
        req.join();
        return req.getResult();
    }


}
