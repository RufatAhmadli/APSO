package sweep;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class PatienceSweep {
    public static void main(String[] args) throws Exception {
        double[] patiences = {2, 4, 8, 16, 32};
        try (BufferedWriter out = new BufferedWriter(new FileWriter("patience_sweep.csv"))) {
            CSVWriter.writeHeader(out);
            for (double patience : patiences) {
                CSVWriter.run(out, "patience", 0.8, 1.0,
                        0.01, patience, 1, 30);
                System.out.println("Done with " + patience + " patience.");
            }
        }
        System.out.println("PatienceSweep complete -> patience_sweep.csv");
    }
}
