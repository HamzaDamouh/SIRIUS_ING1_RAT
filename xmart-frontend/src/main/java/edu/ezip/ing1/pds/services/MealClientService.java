package edu.ezip.ing1.pds.services;

import edu.ezip.ing1.pds.business.dto.Meals;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.requests.SelectMealsCR;

public class MealClientService {
    private final NetworkConfig net;
    public MealClientService(NetworkConfig net) { this.net = net; }
    public Meals listMeals() throws Exception {
        var req = new SelectMealsCR(net, 0);
        req.join();
        return req.getResult();
    }
}
