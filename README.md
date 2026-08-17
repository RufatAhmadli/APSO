# APSO — Adaptive Priority-Aware Scheduling Optimization

A priority-aware scheduling discipline for public service centers, compared against First-Come-First-Served (FCFS) through discrete-event simulation. This repository contains the implementation and analysis accompanying the paper *"Adaptive Priority-Aware Scheduling Optimization for Public Service Delivery"* by Rufat Ahmadli.

## Overview

APSO replaces arrival-order scheduling with a composite priority score computed over urgency, vulnerability, and service complexity, with weights derived through the Analytic Hierarchy Process (AHP). A time-based aging term, following the Accumulating Priority Queue principle, prevents any citizen from being indefinitely postponed. A static-key formulation allows the aging discipline to run on a standard max-heap in O(log n) time.

The priority score is:

```
P(t) = w1·U + w2·V + w3·(1 − dNorm) + α·t
```

where `U` is urgency, `V` vulnerability, `dNorm` normalized service complexity, and `α·t` the aging term. The AHP-derived weights are `w1 = 0.539`, `w2 = 0.297`, `w3 = 0.164` (consistency ratio CR = 0.005).

In the implementation, each trace is generated once per seed and replayed under both policies. A citizen's static heap key is precomputed as `key = base − α·arrival`, where `base = w1·U + w2·V + w3·(1 − dNorm)`. Because `key` only decreases with time at a fixed rate, the relative order of any two waiting citizens can flip as the clock advances (older, lower-priority citizens eventually overtake newer, higher-priority ones) without needing to re-key the heap on every tick.

## Key results

At high load (λ = 0.8), averaged over 30 seeds:

- APSO redistributes waiting and abandonment rather than reducing them overall.
- Urgent citizens' mean wait falls from 1.08 to 0.64; vulnerable from 1.06 to 0.78.
- Urgent abandonment falls from 14.4% to 8.9%; vulnerable from 14.2% to 10.6%.
- The aging term bounds worst-case delay, preventing starvation of low-priority citizens.
- The trade-off is a small reduction in within-category fairness (Jain index ≈ 0.35 → 0.29).

The simulator was validated against the closed-form M/M/1 result (simulated 1.009 vs. theoretical 1.000, see `MM1Validation`).

## Repository structure

```
src/
  enums/
    Category.java          # NORMAL, PREGNANT, ELDERLY, DISABLED
    Kind.java               # ARRIVAL, DEPARTURE, CANCELLATION (event types)
  CustomerData/
    CustomerRecord.java     # immutable per-citizen record: arrival, category, urgency,
                             #   service time, dNorm, U, V, base, key, maxWait
    Event.java               # (time, kind, customer) discrete-event record
    ServiceResult.java       # mutable per-citizen outcome: service start/end, abandoned flag
  trace/
    TraceGenerator.java     # Poisson arrivals, exponential service/patience, dNorm, keys
    TraceTest.java           # reproducibility check for a fixed seed
  simulator/
    FCFSSimulator.java      # first-come-first-served baseline (discrete-event engine)
    APSOSimulator.java      # priority + aging scheduler (max-heap on static key)
    MM1Validation.java      # validates the engine against the M/M/1 formula
    MainComparison.java     # 30-seed FCFS vs APSO comparison (mean ± 95% CI)
  sweep/
    CSVWriter.java          # writes per-citizen results to CSV
    LoadSweep.java           # varies arrival rate λ (0.3–0.95) -> load_sweep.csv
    AgingSweep.java          # varies aging rate α (0–0.1) -> aging_sweep.csv
    PatienceSweep.java       # varies mean patience (2–32) -> patience_sweep.csv
    ServerSweep.java         # varies number of counters (1–16) -> server_sweep.csv
analysis.py                  # reads the CSVs, generates figures, computes Jain fairness
load_sweep.csv, aging_sweep.csv, patience_sweep.csv, server_sweep.csv
fig1_load_urgent_wait.png
fig2_aging_worstcase.png
fig3_patience_abandonment.png
fig4_server_urgent_wait.png
fig5_fairness.png
```

## How it works

- **Trace generation** (`TraceGenerator`): a single workload is generated once per seed — arrival times (Poisson process, exponential inter-arrival gaps via inverse-transform sampling), service times, categories (60% normal, 25% elderly, 15% disabled), an urgency flag (probability 0.3), and a patience threshold (exponential, mean `meanPatience`). Service-complexity `dNorm` is normalized against the min/max service time observed in that trace. The same trace is replayed under both policies, so any difference reflects only the scheduling discipline.
- **Simulation** (`FCFSSimulator`, `APSOSimulator`): a discrete-event engine processes arrival, departure, and cancellation events in time order via a `PriorityQueue<Event>`, and supports multiple parallel servers. FCFS serves waiting citizens in arrival order (`ArrayDeque`); APSO serves the highest-priority citizen from a max-heap keyed on the static value `key = base − α·arrival`. A citizen who is still waiting when its patience threshold elapses abandons the queue.
- **Validation** (`MM1Validation`): with abandonment effectively disabled (patience mean = 1,000,000) and a single server, the FCFS engine's simulated mean wait is compared against the closed-form M/M/1 formula `Wq = ρ / (μ − λ)`, confirming the engine is correct.
- **Experiments** (`MainComparison`, `sweep/*`): `MainComparison` runs 30 seeds at λ = 0.8 and reports mean ± 95% CI for wait time and abandonment rate, broken out by urgency and vulnerability. The four sweep classes each vary one parameter at a time, run 30 seeds per setting, and export every citizen's outcome to CSV via `CSVWriter`.
- **Analysis** (`analysis.py`): reads the CSV output, generates the five figures, and computes the within-category Jain fairness index at the λ = 0.8 operating point.

## Running

Requirements: JDK 17+ (tested with JDK 22), Python 3 with `pandas`, `numpy`, and `matplotlib`.

Compile everything into `out/`:

```bash
javac -d out src/*.java src/*/*.java
```

Run the main comparison:

```bash
java -cp out simulator.MainComparison
```

Validate the engine against the M/M/1 formula:

```bash
java -cp out simulator.MM1Validation
```

Run a sweep (writes a CSV to the repo root), then generate figures and the fairness table:

```bash
java -cp out sweep.LoadSweep
python analysis.py
```

Each sweep class (`LoadSweep`, `AgingSweep`, `PatienceSweep`, `ServerSweep`) can be run the same way; `analysis.py` expects all four CSVs to be present in the repo root and regenerates all five figures in one pass.

## Default parameters

| Parameter | Value |
|---|---|
| Customers per run | 1000 |
| Service rate μ | 1.0 |
| Aging rate α | 0.01 |
| Mean patience | 8 |
| Urgency probability | 0.3 |
| Category split | 60% normal / 25% elderly / 15% disabled |
| Weights (w1, w2, w3) | 0.539, 0.297, 0.164 |
| Seeds | 30 |
