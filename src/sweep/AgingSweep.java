package sweep;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class AgingSweep {
    public static void main(String[] args) throws Exception {
        double[] alphas = {0.0, 0.005, 0.01, 0.05, 0.1};
        try (BufferedWriter out = new BufferedWriter(new FileWriter("aging_sweep.csv"))) {
            CSVWriter.writeHeader(out);
            for (double alpha : alphas) {
                CSVWriter.run(out, "aging", 0.8, 1.0, alpha, 8.0, 1, 30);
                System.out.println("Done with " + alpha + " alpha.");
            }
        }
        System.out.println("AgingSweep complete -> aging_sweep.csv");
    }
}