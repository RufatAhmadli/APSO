package sweep;

import CustomerData.CustomerRecord;
import CustomerData.ServiceResult;
import simulator.APSOSimulator;
import simulator.FCFSSimulator;
import trace.TraceGenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CSVWriter {
    public static void run(BufferedWriter writer, String sweepName,
                           double lambda, double mu, double alpha, double patience,
                           int servers, int numSeeds) throws IOException {
        for (int seed = 0; seed < numSeeds; seed++) {
            var input = new TraceGenerator(0.539, 0.297, 0.164, alpha, patience);
            List<CustomerRecord> customerRecords = input.generateTrace(seed, 1000, lambda, mu);

            Map<Integer, ServiceResult> fcfs = new FCFSSimulator().buildFCFS(customerRecords, servers);
            Map<Integer, ServiceResult> apso = new APSOSimulator().buildAPSO(customerRecords, servers);

            for (CustomerRecord c : customerRecords) {
                writeRow(writer, sweepName, lambda, alpha, patience, servers, seed, c, fcfs.get(c.id()), "FCFS");
                writeRow(writer, sweepName, lambda, alpha, patience, servers, seed, c, apso.get(c.id()), "APSO");
            }
        }
    }

    private static void writeRow(BufferedWriter writer, String sweepName, double lambda,
                                 double alpha, double patience, int servers, int seed,
                                 CustomerRecord c, ServiceResult result, String type) throws IOException {
        double wait = result.isAbandoned() ? -1 : result.getWaitTime();   // -1 flags abandoned
        writer.write(String.format("%s,%s,%.3f,%.4f,%.1f,%d,%d,%d,%s,%b,%.4f,%b%n",
                sweepName, type, lambda, alpha, patience, servers, seed,
                c.id(), c.category(), c.urgent(), wait, result.isAbandoned()));
    }

    public static void writeHeader(BufferedWriter out) throws IOException {
        out.write(
                "sweep,type,lambda,alpha,patience,servers,seed,id,category,urgent,wait,abandoned\n"
        );
    }
}

