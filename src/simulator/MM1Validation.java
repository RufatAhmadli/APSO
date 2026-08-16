package simulator;

import CustomerData.CustomerRecord;
import CustomerData.ServiceResult;
import trace.TraceGenerator;

import java.util.Map;

public class MM1Validation {
    public static void main(String[] args) {
        double lambda = 0.5, mu = 1.0;   // λ < μ for stability
        int n = 1_000_000;                // large n for a stable average

        // patience VERY high so nobody abandons → pure M/M/1 (no reneging)
        var gen = new TraceGenerator(0.539, 0.297, 0.164, 0.01, 1_000_000.0);
        var trace = gen.generateTrace(42L, n, lambda, mu);

        // FCFS on a single server = plain M/M/1
        Map<Integer, ServiceResult> fcfs = new FCFSSimulator().buildFCFS(trace, 1);

        double totalWait = 0; int count = 0;
        for (CustomerRecord c : trace) {
            if (!fcfs.get(c.id()).isAbandoned()) {
                totalWait += fcfs.get(c.id()).getWaitTime();
                count++;
            }
        }
        double meanWait = totalWait / count;

        // M/M/1 theory:  Wq = λ / (μ(μ − λ)) = ρ / (μ − λ)
        double rho = lambda / mu;
        double theoryWq = rho / (mu - lambda);

        System.out.printf("Simulated mean wait (Wq): %.4f%n", meanWait);
        System.out.printf("M/M/1 theory        (Wq): %.4f%n", theoryWq);
        System.out.printf("Ratio (should be ≈ 1)   : %.4f%n", meanWait / theoryWq);
    }
}