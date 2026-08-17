package sweep;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ServerSweep {
    public static void main(String[] args) throws IOException {
        int[] serverCounts = {1, 2, 4, 8, 16};
        try (var output = new BufferedWriter(new FileWriter("server_sweep.csv"))) {
            CSVWriter.writeHeader(output);
            for (int server : serverCounts) {
                CSVWriter.run(output, "server_sweep",
                        0.8 * server, 1, 0.01, 8.0, server, 30);
                System.out.println("Done with " + server + " servers.");
            }
        }
        System.out.println("ServerSweep complete -> server_sweep.csv");
    }
}
