import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

# --- paths --------------------------------------------------------------
base_in  = Path("aisles/input_a")   # full data
base_out = Path("aisles/output_a")  # subset
plot_dir = Path("aisles/plots")     # where to save the figures
plot_dir.mkdir(parents=True, exist_ok=True)

# --- helper -------------------------------------------------------------
def make_bars(df_full, subset_pasillos, col, ax, title):
    """Draw one colored bar plot on the given axis."""
    d = df_full.sort_values(col, ascending=False, ignore_index=True)
    colors = ["red" if str(p) in subset_pasillos else "blue" for p in d["pasillo"]]
    ax.bar(range(len(d)), d[col], color=colors)
    ax.set_title(title)
    ax.tick_params(axis="x", labelbottom=False)   # hide x labels

# --- main loop ----------------------------------------------------------
for num in range(1, 21):
    tag = f"{num:04d}"                     # 0001 … 0020
    file_full   = base_in  / f"instance_{tag}.csv"
    file_subset = base_out / f"instance_{tag}.csv"

    # read both csvs
    df_full   = pd.read_csv(file_full)
    df_subset = pd.read_csv(file_subset)

    subset_pasillos = set(df_subset["pasillo"].astype(str))

    # build the three-panel figure
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    make_bars(df_full, subset_pasillos, "uniqueItems", axes[0], "Ítems únicos")
    make_bars(df_full, subset_pasillos, "items",        axes[1], "Total ítems")
    make_bars(df_full, subset_pasillos, "satOrders",    axes[2], "Órdenes satisfechas")

    fig.suptitle(f"Instancia {tag}", fontsize=14)
    fig.tight_layout()

    # save and close
    fig.savefig(plot_dir / f"instance_{tag}_bars.png", dpi=150)
    plt.close(fig)