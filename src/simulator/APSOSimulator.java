package simulator;

import CustomerData.CustomerRecord;
import CustomerData.Event;
import CustomerData.ServiceResult;
import enums.Kind;

import java.util.*;

public class APSOSimulator {
    public Map<Integer, ServiceResult> buildAPSO(List<CustomerRecord> trace, int numServers) {
        Map<Integer, ServiceResult> results = new HashMap<>();
        for (CustomerRecord c : trace) {
            results.put(c.id(), new ServiceResult(c.id(), c.arrival()));
        }


        PriorityQueue<Event> events = new PriorityQueue<>(
                Comparator.<Event>comparingDouble(Event::time)
                        .thenComparing(e -> e.kind() == Kind.DEPARTURE ? 0 : 1)
        );

        // load all arrivals up front from the trace
        for (CustomerRecord c : trace) {
            events.add(new Event(c.arrival(), Kind.ARRIVAL, c));
        }

        // max-heap implementation
        PriorityQueue<CustomerRecord> waiting = new PriorityQueue<>(
                Comparator.comparingDouble(CustomerRecord::key).reversed()
        );
        int freeServers = numServers;

        while (!events.isEmpty()) {
            Event ev = events.poll();
            double now = ev.time();
            CustomerRecord c = ev.customer();

            if (ev.kind() == Kind.ARRIVAL) {
                if (freeServers > 0) {
                    freeServers--;
                    startService(c, now, results, events); // serve immediately
                } else {
                    waiting.add(c); // must wait
                    double giveUpTime = c.arrival() + c.maxWait();
                    events.add(new Event(giveUpTime, Kind.CANCELLATION, c));
                }
            } else if (ev.kind() == Kind.CANCELLATION) {
                if (results.get(c.id()).getServiceStart() < 0) {   // -1 = never started = still waiting
                    waiting.remove(c);
                    results.get(c.id()).setAbandoned(true);
                }
            } else { // DEPARTURE
                results.get(c.id()).setServiceEnd(now);       // record finish
                if (!waiting.isEmpty()) {
                    CustomerRecord next = waiting.poll(); // FCFS: longest-waiting
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
}
