import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

TYPE = "type"   # ← the column holding FCFS / APSO (rename here if needed)

def load(path):
    df = pd.read_csv(path)
    df["abandoned"] = df["abandoned"].astype(str).str.upper().map({"TRUE": True, "FALSE": False})
    df["urgent"]    = df["urgent"].astype(str).str.upper().map({"TRUE": True, "FALSE": False})
    return df

def mean_wait_served(df, group_col):
    served = df[~df["abandoned"]]
    return served.groupby([group_col, TYPE])["wait"].mean().unstack()

def abandonment_rate(df, group_col):
    return (df.groupby([group_col, TYPE])["abandoned"].mean() * 100).unstack()

def jain(waits):
    """Jain's fairness index: 1/n (unfair) .. 1.0 (perfectly fair)."""
    w = np.asarray(waits, dtype=float)
    if len(w) == 0 or w.sum() == 0:
        return 1.0
    return (w.sum() ** 2) / (len(w) * (w ** 2).sum())

# FIGURE 1 — LOAD: urgent wait vs load
load_df = load("load_sweep.csv")
urgent = load_df[load_df["urgent"]]
mean_wait_served(urgent, "lambda").plot(marker="o")
plt.xlabel("Load (λ)"); plt.ylabel("Mean urgent wait")
plt.title("Urgent-customer wait vs load: APSO vs FCFS")
plt.grid(True, alpha=0.3); plt.tight_layout()
plt.savefig("fig1_load_urgent_wait.png", dpi=150); plt.close()

# FIGURE 2 — AGING: worst-case wait vs alpha
aging_df = load("aging_sweep.csv")
low = aging_df[(aging_df["category"]=="NORMAL") & (~aging_df["urgent"]) & (~aging_df["abandoned"])]
low.groupby(["alpha", TYPE])["wait"].max().unstack().plot(marker="o")
plt.xlabel("Aging rate (α)"); plt.ylabel("Worst-case wait (normal+non-urgent)")
plt.title("Aging controls starvation of low-priority customers")
plt.grid(True, alpha=0.3); plt.tight_layout()
plt.savefig("fig2_aging_worstcase.png", dpi=150); plt.close()

# FIGURE 3 — PATIENCE: urgent abandonment vs patience
pat_df = load("patience_sweep.csv")
urg_pat = pat_df[pat_df["urgent"]]
abandonment_rate(urg_pat, "patience").plot(marker="o")
plt.xlabel("Mean patience"); plt.ylabel("Urgent abandonment rate (%)")
plt.title("APSO reduces urgent abandonment across patience levels")
plt.grid(True, alpha=0.3); plt.tight_layout()
plt.savefig("fig3_patience_abandonment.png", dpi=150); plt.close()

# FIGURE 4 — SERVER: urgent wait vs server count
srv_df = load("server_sweep.csv")
urg_srv = srv_df[srv_df["urgent"]]
mean_wait_served(urg_srv, "servers").plot(marker="o")
plt.xlabel("Number of servers"); plt.ylabel("Mean urgent wait")
plt.title("APSO advantage holds across server counts")
plt.grid(True, alpha=0.3); plt.tight_layout()
plt.savefig("fig4_server_urgent_wait.png", dpi=150); plt.close()

print("Done. Four figures saved.")

# ============================================================
# FAIRNESS — Jain index WITHIN each category (FCFS vs APSO)
# measured at the main operating point: load sweep, lambda = 0.8, served only
# ============================================================
op = load_df[(load_df["lambda"] == 0.8) & (~load_df["abandoned"])]
categories = sorted(op["category"].unique())

print("\n=== Jain fairness within class (λ=0.8, served customers) ===")
print(f"{'category':<10} {'FCFS':>8} {'APSO':>8}")
fcfs_vals, apso_vals = [], []
for cat in categories:
    f = jain(op[(op["category"] == cat) & (op[TYPE] == "FCFS")]["wait"])
    a = jain(op[(op["category"] == cat) & (op[TYPE] == "APSO")]["wait"])
    fcfs_vals.append(f); apso_vals.append(a)
    print(f"{cat:<10} {f:>8.4f} {a:>8.4f}")

# bar chart of the fairness table
x = np.arange(len(categories)); width = 0.35
plt.bar(x - width/2, fcfs_vals, width, label="FCFS")
plt.bar(x + width/2, apso_vals, width, label="APSO")
plt.xticks(x, categories); plt.ylim(0, 1.05)
plt.ylabel("Jain fairness index (within class)")
plt.title("APSO maintains within-class fairness")
plt.legend(); plt.grid(True, alpha=0.3, axis="y"); plt.tight_layout()
plt.savefig("fig5_fairness.png", dpi=150); plt.close()

print("\nDone. Five figures saved.")