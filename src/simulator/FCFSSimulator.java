package simulator;

import CustomerData.CustomerRecord;
import CustomerData.Event;
import CustomerData.ServiceResult;
import enums.Category;
import enums.Kind;
import trace.TraceGenerator;

import java.util.*;

public class FCFSSimulator {

    public Map<Integer, ServiceResult> buildFCFS(List<CustomerRecord> trace, int numServers) {
        Map<Integer, ServiceResult> results = new HashMap<>();
        for (CustomerRecord c : trace) {
            results.put(c.id(), new ServiceResult(c.id(), c.arrival()));
        }

        // ordering events by arrival time
        PriorityQueue<Event> events = new PriorityQueue<>(
                Comparator.comparingDouble(Event::time)
                        .thenComparing(e -> e.kind() == Kind.DEPARTURE ? 0 : 1)
        );

        // load all arrivals up front from the trace
        for (CustomerRecord c : trace) {
            events.add(new Event(c.arrival(), Kind.ARRIVAL, c));
        }

        Deque<CustomerRecord> waiting = new ArrayDeque<>();
        int freeServers = numServers;

        while (!events.isEmpty()) {
            Event ev = events.poll();
            double now = ev.time();
            CustomerRecord c = ev.customer();

            if (ev.kind() == Kind.ARRIVAL) {
                if (freeServers > 0) {
                    freeServers--;
                    startService(c, now, results, events);   // serve immediately
                } else {
                    waiting.addLast(c); // must wait
                    double giveUpTime = c.arrival() + c.maxWait();
                    events.add(new Event(giveUpTime, Kind.CANCELLATION, c));
                }
            } else if (ev.kind() == Kind.CANCELLATION) {
                if (results.get(c.id()).getServiceStart() < 0) {   // still waiting → abandons
                    waiting.remove(c);
                    results.get(c.id()).setAbandoned(true);
                }
            } else { // DEPARTURE
                results.get(c.id()).setServiceEnd(now);       // record finish
                if (!waiting.isEmpty()) {
                    CustomerRecord next = waiting.pollFirst(); // FCFS: longest-waiting
                    startService(next, now, results, events);  // server stays busy
                } else {
                    freeServers++;                             // server goes idle
                }
            }
        }
        return results;
    }

    private void startService(CustomerRecord c, double now,
                              Map<Integer, ServiceResult> results,
                              PriorityQueue<Event> events) {
        results.get(c.id()).setServiceStart(now);
        double departureTime = now + c.serviceTime();
        events.add(new Event(departureTime, Kind.DEPARTURE, c));
    }

    public static void main(String[] args) {
        double lambda = 0.8, mu = 1.0;
        int n = 100_000;

        var gen = new TraceGenerator(0.539, 0.297, 0.164, 0.01, 8.0);
        var trace = gen.generateTrace(42L, n, lambda, mu);

        var fcfs = new FCFSSimulator().buildFCFS(trace, 1);
        var apso = new APSOSimulator().buildAPSO(trace, 1);

        // --- overall mean wait (SKIP abandoned — they have no valid wait) ---
        double fcfsAll = 0, apsoAll = 0;
        int servedCnt = 0;
        for (CustomerRecord c : trace) {
            if (fcfs.get(c.id()).isAbandoned() || apso.get(c.id()).isAbandoned()) continue;
            fcfsAll += fcfs.get(c.id()).getWaitTime();
            apsoAll += apso.get(c.id()).getWaitTime();
            servedCnt++;
        }
        fcfsAll /= servedCnt;
        apsoAll /= servedCnt;

        // --- mean wait for URGENT vs NON-URGENT (served only) ---
        double fcfsUrg = 0, apsoUrg = 0;
        int urgCnt = 0;
        double fcfsNon = 0, apsoNon = 0;
        int nonCnt = 0;
        for (CustomerRecord c : trace) {
            if (fcfs.get(c.id()).isAbandoned() || apso.get(c.id()).isAbandoned()) continue;
            double fw = fcfs.get(c.id()).getWaitTime();
            double aw = apso.get(c.id()).getWaitTime();
            if (c.urgent()) {
                fcfsUrg += fw;
                apsoUrg += aw;
                urgCnt++;
            } else {
                fcfsNon += fw;
                apsoNon += aw;
                nonCnt++;
            }
        }

        // --- mean wait for VULNERABLE vs NON-VUL (served only) ---
        double fcfsVul = 0, apsoVul = 0;
        int VulCnt = 0;
        double fcfsNonVul = 0, apsoNonVul = 0;
        int nonCntVul = 0;
        for (CustomerRecord c : trace) {
            if (fcfs.get(c.id()).isAbandoned() || apso.get(c.id()).isAbandoned()) continue;
            double fw = fcfs.get(c.id()).getWaitTime();
            double aw = apso.get(c.id()).getWaitTime();
            if (c.category() != Category.NORMAL) {
                fcfsVul += fw;
                apsoVul += aw;
                VulCnt++;
            } else {
                fcfsNonVul += fw;
                apsoNonVul += aw;
                nonCntVul++;
            }
        }

        System.out.printf("=== MEAN WAIT (served customers only) ===%n");
        System.out.printf("OVERALL      FCFS=%.4f  APSO=%.4f  Δ=%+.4f%n",
                fcfsAll, apsoAll, apsoAll - fcfsAll);
        System.out.printf("URGENT       FCFS=%.4f  APSO=%.4f  Δ=%+.4f  (n=%d)%n",
                fcfsUrg / urgCnt, apsoUrg / urgCnt, (apsoUrg - fcfsUrg) / urgCnt, urgCnt);
        System.out.printf("NON-URGENT   FCFS=%.4f  APSO=%.4f  Δ=%+.4f  (n=%d)%n",
                fcfsNon / nonCnt, apsoNon / nonCnt, (apsoNon - fcfsNon) / nonCnt, nonCnt);
        System.out.printf("VULNERABLE   FCFS=%.4f  APSO=%.4f  Δ=%+.4f  (n=%d)%n",
                fcfsVul / VulCnt, apsoVul / VulCnt, (apsoVul - fcfsVul) / VulCnt, VulCnt);
        System.out.printf("NON-VUL      FCFS=%.4f  APSO=%.4f  Δ=%+.4f  (n=%d)%n",
                fcfsNonVul / nonCntVul, apsoNonVul / nonCntVul, (apsoNonVul - fcfsNonVul) / nonCntVul, nonCntVul);

        // --- ABANDONMENT RATE by category (the cancellation result) ---
        int fAbUrg = 0, aAbUrg = 0, urgTot = 0;
        int fAbNon = 0, aAbNon = 0, nonTot = 0;
        int fAbVul = 0, aAbVul = 0, vulTot = 0;
        int fAbNonVul = 0, aAbNonVul = 0, nonVulTot = 0;
        for (CustomerRecord c : trace) {
            boolean fAb = fcfs.get(c.id()).isAbandoned();
            boolean aAb = apso.get(c.id()).isAbandoned();
            if (c.urgent()) {
                if (fAb) fAbUrg++;
                if (aAb) aAbUrg++;
                urgTot++;
            } else {
                if (fAb) fAbNon++;
                if (aAb) aAbNon++;
                nonTot++;
            }
            if (c.category() != Category.NORMAL) {
                if (fAb) fAbVul++;
                if (aAb) aAbVul++;
                vulTot++;
            } else {
                if (fAb) fAbNonVul++;
                if (aAb) aAbNonVul++;
                nonVulTot++;
            }
        }

        int fAbAll = fAbUrg + fAbNon;   // total abandoned FCFS
        int aAbAll = aAbUrg + aAbNon;   // total abandoned APSO

        System.out.printf("%n=== ABANDONMENT RATE ===%n");
        System.out.printf("OVERALL      FCFS=%.2f%%  APSO=%.2f%%%n",
                100.0 * fAbAll / n, 100.0 * aAbAll / n);
        System.out.printf("URGENT       FCFS=%.2f%%  APSO=%.2f%%  (n=%d)%n",
                100.0 * fAbUrg / urgTot, 100.0 * aAbUrg / urgTot, urgTot);
        System.out.printf("NON-URGENT   FCFS=%.2f%%  APSO=%.2f%%  (n=%d)%n",
                100.0 * fAbNon / nonTot, 100.0 * aAbNon / nonTot, nonTot);
        System.out.printf("VULNERABLE   FCFS=%.2f%%  APSO=%.2f%%  (n=%d)%n",
                100.0 * fAbVul / vulTot, 100.0 * aAbVul / vulTot, vulTot);
        System.out.printf("NON-VUL      FCFS=%.2f%%  APSO=%.2f%%  (n=%d)%n",
                100.0 * fAbNonVul / nonVulTot, 100.0 * aAbNonVul / nonVulTot, nonVulTot);
    }


}
