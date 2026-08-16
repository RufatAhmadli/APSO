package trace;

import CustomerData.CustomerRecord;

public class TraceTest {
    public static void main(String[] args) {
        var gen = new TraceGenerator(0.539, 0.297, 0.164, 0.01,8.0);
        var trace = gen.generateTrace(42L, 10, 0.5, 0.2);

        System.out.println("=== Trace (seed=42, n=10) ===");
        for (CustomerRecord c : trace) {
            System.out.printf(
                    "id=%2d  arrival=%7.3f  svc=%6.3f  cat=%-8s  urgent=%-5b  U=%.0f V=%.0f  dNorm=%.3f  base=%.3f  key=%.3f%n",
                    c.id(), c.arrival(), c.serviceTime(), c.category(), c.urgent(),
                    c.U(), c.V(), c.dNorm(), c.base(), c.key()
            );
        }

        // --- reproducibility check: same seed must give identical output ---
        var trace2 = gen.generateTrace(42L, 10, 0.5, 0.2);
        boolean identical = true;
        for (int i = 0; i < trace.size(); i++) {
            if (trace.get(i).arrival() != trace2.get(i).arrival()
                    || trace.get(i).serviceTime() != trace2.get(i).serviceTime()) {
                identical = false;
                break;
            }
        }
        System.out.println("\nReproducible (seed 42 twice identical): " + identical);
    }
}


