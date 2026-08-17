package simulator;

import CustomerData.CustomerRecord;
import CustomerData.ServiceResult;
import enums.Category;
import trace.TraceGenerator;

import java.util.Map;

public class MainComparison {
    public static void main(String[] args) {
        double lambda = 0.8, mu = 1.0;
        int n = 1000;
        int numSeeds = 30;

        // wait metrics (served only)
        double[] overallF = new double[numSeeds], overallA = new double[numSeeds];
        double[] urgentF = new double[numSeeds], urgentA = new double[numSeeds];
        double[] nonUrgF = new double[numSeeds], nonUrgA = new double[numSeeds];
        double[] vulF = new double[numSeeds], vulA = new double[numSeeds];
        double[] nonVulF = new double[numSeeds], nonVulA = new double[numSeeds];
        double[] worstF = new double[numSeeds], worstA = new double[numSeeds];

        // abandonment metrics (percent)
        double[] abOverallF = new double[numSeeds], abOverallA = new double[numSeeds];
        double[] abUrgentF = new double[numSeeds], abUrgentA = new double[numSeeds];
        double[] abNonUrgF = new double[numSeeds], abNonUrgA = new double[numSeeds];
        double[] abVulF = new double[numSeeds], abVulA = new double[numSeeds];
        double[] abNonVulF = new double[numSeeds], abNonVulA = new double[numSeeds];

        for (int seed = 1; seed <= numSeeds; seed++) {
            var gen = new TraceGenerator(0.539, 0.297, 0.164, 0.01, 8.0);
            var trace = gen.generateTrace(seed, n, lambda, mu);

            Map<Integer, ServiceResult> fcfs = new FCFSSimulator().buildFCFS(trace, 1);
            Map<Integer, ServiceResult> apso = new APSOSimulator().buildAPSO(trace, 1);

            double oF = 0, oA = 0;
            int oc = 0;
            double uF = 0, uA = 0;
            int uc = 0;
            double nF = 0, nA = 0;
            int nc = 0;
            double vF = 0, vA = 0;
            int vc = 0;
            double nvF = 0, nvA = 0;
            int nvc = 0;
            double wF = 0, wA = 0;

            // abandonment counters
            int abUF = 0, abUA = 0, uTot = 0;
            int abNF = 0, abNA = 0, nTot = 0;
            int abVF = 0, abVA = 0, vTot = 0;
            int abNVF = 0, abNVA = 0, nvTot = 0;

            for (CustomerRecord c : trace) {
                boolean fAb = fcfs.get(c.id()).isAbandoned();
                boolean aAb = apso.get(c.id()).isAbandoned();

                // --- abandonment counting (ALL customers) ---
                if (c.urgent()) {
                    if (fAb) abUF++;
                    if (aAb) abUA++;
                    uTot++;
                } else {
                    if (fAb) abNF++;
                    if (aAb) abNA++;
                    nTot++;
                }
                if (c.category() != Category.NORMAL) {
                    if (fAb) abVF++;
                    if (aAb) abVA++;
                    vTot++;
                } else {
                    if (fAb) abNVF++;
                    if (aAb) abNVA++;
                    nvTot++;
                }

                // --- wait accumulation: SKIP abandoned (garbage wait) ---
                if (fAb || aAb) continue;
                double fw = fcfs.get(c.id()).getWaitTime();
                double aw = apso.get(c.id()).getWaitTime();

                oF += fw;
                oA += aw;
                oc++;
                if (c.urgent()) {
                    uF += fw;
                    uA += aw;
                    uc++;
                } else {
                    nF += fw;
                    nA += aw;
                    nc++;
                }
                if (c.category() != Category.NORMAL) {
                    vF += fw;
                    vA += aw;
                    vc++;
                } else {
                    nvF += fw;
                    nvA += aw;
                    nvc++;
                }
                if (c.category() == Category.NORMAL && !c.urgent()) {
                    if (fw > wF) wF = fw;
                    if (aw > wA) wA = aw;
                }
            }

            // store waits
            overallF[seed - 1] = oF / oc;
            overallA[seed - 1] = oA / oc;
            urgentF[seed - 1] = uF / uc;
            urgentA[seed - 1] = uA / uc;
            nonUrgF[seed - 1] = nF / nc;
            nonUrgA[seed - 1] = nA / nc;
            vulF[seed - 1] = vF / vc;
            vulA[seed - 1] = vA / vc;
            nonVulF[seed - 1] = nvF / nvc;
            nonVulA[seed - 1] = nvA / nvc;
            worstF[seed - 1] = wF;
            worstA[seed - 1] = wA;

            // store abandonment (as %)
            abOverallF[seed - 1] = 100.0 * (abUF + abNF) / n;
            abOverallA[seed - 1] = 100.0 * (abUA + abNA) / n;
            abUrgentF[seed - 1] = 100.0 * abUF / uTot;
            abUrgentA[seed - 1] = 100.0 * abUA / uTot;
            abNonUrgF[seed - 1] = 100.0 * abNF / nTot;
            abNonUrgA[seed - 1] = 100.0 * abNA / nTot;
            abVulF[seed - 1] = 100.0 * abVF / vTot;
            abVulA[seed - 1] = 100.0 * abVA / vTot;
            abNonVulF[seed - 1] = 100.0 * abNVF / nvTot;
            abNonVulA[seed - 1] = 100.0 * abNVA / nvTot;
        }

        System.out.printf("=== 30-seed results (n=%d, λ=%.1f, μ=%.1f, patience=8) ===%n%n", n, lambda, mu);

        System.out.println("--- MEAN WAIT (served only) ---");
        System.out.printf("%-12s %-22s %-22s%n", "", "FCFS (mean ± CI)", "APSO (mean ± CI)");
        report("OVERALL", overallF, overallA);
        report("URGENT", urgentF, urgentA);
        report("NON-URGENT", nonUrgF, nonUrgA);
        report("VULNERABLE", vulF, vulA);
        report("NON-VUL", nonVulF, nonVulA);
        report("WORST-CASE", worstF, worstA);

        System.out.println("\n--- ABANDONMENT RATE % ---");
        System.out.printf("%-12s %-22s %-22s%n", "", "FCFS (mean ± CI)", "APSO (mean ± CI)");
        report("OVERALL", abOverallF, abOverallA);
        report("URGENT", abUrgentF, abUrgentA);
        report("NON-URGENT", abNonUrgF, abNonUrgA);
        report("VULNERABLE", abVulF, abVulA);
        report("NON-VUL", abNonVulF, abNonVulA);
    }

    private static void report(String label, double[] f, double[] a) {
        System.out.printf("%-12s %8.4f ± %-11.4f %8.4f ± %-11.4f%n",
                label, mean(f), ci95(f), mean(a), ci95(a));
    }

    private static double mean(double[] x) {
        double s = 0;
        for (double v : x) s += v;
        return s / x.length;
    }

    private static double ci95(double[] x) {
        double m = mean(x), sumSq = 0;
        for (double v : x) sumSq += (v - m) * (v - m);
        double stddev = Math.sqrt(sumSq / (x.length - 1));
        return 1.96 * stddev / Math.sqrt(x.length);
    }
}