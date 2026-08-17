package sweep;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class LoadSweep {
    public static void main(String[] args) throws Exception {
        double[] loads = {0.3, 0.5, 0.7, 0.8, 0.9, 0.95};
        try (BufferedWriter out = new BufferedWriter(new FileWriter("load_sweep.csv"))) {
            CSVWriter.writeHeader(out);
            for (double lambda : loads) {
                CSVWriter.run(out, "load", lambda, 1.0,
                        0.01, 8.0, 1, 30);
                System.out.println("Done with " + lambda + " load.");
            }
        }
        System.out.println("LoadSweep complete -> load_sweep.csv");
    }
}