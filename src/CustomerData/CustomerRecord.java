package CustomerData;

import enums.Category;

public record CustomerRecord(
        int id,
        String name,
        String surname,
        double arrival,       // when the citizen becomes ready
        Category category,    // vulnerability class -> V
        boolean urgent,       // deadline-sensitive -> U
        double serviceTime,   // how long service takes (raw)
        double dNorm,         // service complexity in [0,1] (from generator)
        double U,             // urgency in {0,1}
        double V,             // vulnerability in {0,1}
        double base,          // w1*U + w2*V + w3*(1 - dNorm)
        double key,            // base - alpha*arrival  (static heap key)
        double maxWait        // cancellation threshold
) {}
