package edu.ezip.ing1.pds.services;

import edu.ezip.ing1.pds.business.dto.MealPlan;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.requests.GenerateMealPlanCR;
import edu.ezip.ing1.pds.requests.GetMealPlanCR;

public class MealPlanClientService {
    private final NetworkConfig net;
    public MealPlanClientService(NetworkConfig net) { this.net = net; }

    public MealPlan generate(long userId, double tolerance) throws Exception {
        var req =  new GenerateMealPlanCR(net, 0, userId, tolerance);
        req.join();
        return req.getResult();
    }

    public MealPlan get(long userId) throws Exception {
        var req =  new GetMealPlanCR(net, 0, userId);
        req.join();
        return req.getResult();
    }
}
