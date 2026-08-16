package trace;

import CustomerData.CustomerRecord;
import enums.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

public class TraceGenerator {
    private final double w1, w2, w3, alpha, meanPatience;

    public TraceGenerator(double w1, double w2, double w3, double alpha, double meanPatience) {
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        this.alpha = alpha;
        this.meanPatience = meanPatience;
    }

    /**
     * seed -
     * n - number of customers
     * lambda - arrival rate
     * mu - service rate
     * @return a list of CustomerRecords
     */
    public List<CustomerRecord> generateTrace(long seed, int n, double lambda, double mu) {
        var rng = new SplittableRandom(seed);
        double clock = 0;

        // ---- PASS 1: generate raw data, don't compute dNorm/base/key yet ----
        double[] arrivals = new double[n];
        double[] serviceTimes = new double[n];
        Category[] categories = new Category[n];
        boolean[] urgents = new boolean[n];
        double[] maxWaits = new double[n];

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            clock += -Math.log(1 - rng.nextDouble()) / lambda;
            double serviceTime = -Math.log(1 - rng.nextDouble()) / mu;

            arrivals[i] = clock;
            serviceTimes[i] = serviceTime;
            categories[i] = drawCategory(rng);
            urgents[i] = rng.nextDouble() < 0.3;
            maxWaits[i] = -Math.log(1 - rng.nextDouble()) * meanPatience;

            min = Math.min(min, serviceTime);   // track as we go
            max = Math.max(max, serviceTime);
        }
        // min and max are now FINAL — true across all n customers

        // ---- PASS 2: now normalize and build the records ----
        var jobs = new ArrayList<CustomerRecord>(n);
        for (int i = 0; i < n; i++) {
            double U = urgents[i] ? 1 : 0;
            double V = (categories[i] == Category.NORMAL) ? 0 : 1;

            double dNorm = (serviceTimes[i] - min) / (max - min);   // ✓ correct now

            double base = w1 * U + w2 * V + w3 * (1 - dNorm);
            double key = base - alpha * arrivals[i];

            jobs.add(new CustomerRecord(
                    i, "Name" + i, "Surname" + i,
                    arrivals[i], categories[i], urgents[i], serviceTimes[i],
                    dNorm, U, V, base, key, maxWaits[i]
            ));
        }
        return jobs;
    }

    // creating 60/25/15 distribution for V
    private Category drawCategory(SplittableRandom rng) {
        double r = rng.nextDouble();
        if (r < 0.60) return Category.NORMAL;      // 60%
        if (r < 0.85) return Category.ELDERLY;     // 25%
        return Category.DISABLED;                   // 15%
    }
}
