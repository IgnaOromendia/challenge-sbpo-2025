import matplotlib.pyplot as plt
import pandas as pd

instancias_x4 = [
    ("standard_10_A.csv", "not_using_greedy_10_A.csv", "Instance 10 A"),
    ("standard_04_B.csv", "not_using_greedy_04_B.csv", "Instance 4 B"),
    ("standard_12_B.csv", "not_using_greedy_12_B.csv", "Instance 12 B"),
    ("standard_15_B.csv", "not_using_greedy_15_B.csv", "Instance 15 B")
]

instancias_x2 = [
    ("standard_10_A.csv", "not_using_local_search_10_A.csv", "Instance 10 A"),
    ("standard_12_B.csv", "not_using_local_search_12_B.csv", "Instance 12 B")
]


def plot_standard_vs_not_local_search_obj():
    fig, axes = plt.subplots(1, 2, figsize=(12, 4))
    axes = axes.flatten()

    for i, (std_file, ng_file, title) in enumerate(instancias_x2):
        standard = pd.read_csv(std_file)
        not_greedy = pd.read_csv(ng_file)

        ax = axes[i]
        ax.plot(standard["it"], standard["obj"], '-o', label="Standard", linewidth=2, markersize=4)
        ax.plot(not_greedy["it"], not_greedy["obj"], '-o', label="Not Local Search", linewidth=2, markersize=4)

        ax.set_title(title)
        ax.set_xlabel("Iteration")
        ax.set_ylabel("Objective value")
        ax.grid(True, linestyle='--', alpha=0.5)
        ax.legend()

        for it in standard["it"]:
            if it % 2 == 1:  
                ax.axvline(x=it, color='red', linestyle='--', linewidth=1, alpha=0.2)

    plt.tight_layout()
    plt.savefig("plots/local_search_cmp.svg")
    plt.show()

def plot_standard_vs_not_greedy_obj():
    fig, axes = plt.subplots(2, 2, figsize=(12, 8))
    axes = axes.flatten()

    for i, (std_file, ng_file, title) in enumerate(instancias_x4):
        standard = pd.read_csv(std_file)
        not_greedy = pd.read_csv(ng_file)

        ax = axes[i]
        ax.plot(standard["it"], standard["obj"], '-o', label="Standard", linewidth=2, markersize=4)
        ax.plot(not_greedy["it"], not_greedy["obj"], '-o', label="Not Greedy", linewidth=2, markersize=4)

        ax.set_title(title)
        ax.set_xlabel("Iteration")
        ax.set_ylabel("Objective value")
        ax.grid(True, linestyle='--', alpha=0.5)
        ax.legend()

    plt.tight_layout()
    plt.savefig("plots/greedy_cmp.svg")
    plt.show()

def plot_standard_vs_not_local_search_time():
    fig, axes = plt.subplots(1, 2, figsize=(12, 4))
    axes = axes.flatten()

    for i, (std_file, ng_file, title) in enumerate(instancias_x2):
        standard = pd.read_csv(std_file)
        not_local_search = pd.read_csv(ng_file)

        standard["sum_time"] = standard["tiempo"].cumsum()
        not_local_search["sum_time"] = not_local_search["tiempo"].cumsum()

        ax = axes[i]
        ax.plot(standard["it"], standard["sum_time"], '-o', label="Standard", linewidth=2, markersize=4)
        ax.plot(not_local_search["it"], not_local_search["sum_time"], '-o', label="Not Local Search", linewidth=2, markersize=4)

        ax.set_title(title)
        ax.set_xlabel("Iteration")
        ax.set_ylabel("Objective value")
        ax.grid(True, linestyle='--', alpha=0.5)
        ax.legend()

        for it in standard["it"]:
            if it % 2 == 1:  
                ax.axvline(x=it, color='red', linestyle='--', linewidth=1, alpha=0.2)

    plt.tight_layout()
    plt.savefig("plots/local_search_cmp_time.svg")
    plt.show()

def plot_standard_vs_not_greedy_time():
    fig, axes = plt.subplots(2, 2, figsize=(12, 8))
    axes = axes.flatten()

    for i, (std_file, ng_file, title) in enumerate(instancias_x4):
        standard = pd.read_csv(std_file)
        not_greedy = pd.read_csv(ng_file)

        standard["sum_time"] = standard["tiempo"].cumsum()
        not_greedy["sum_time"] = not_greedy["tiempo"].cumsum()

        ax = axes[i]
        ax.plot(standard["it"], standard["sum_time"], '-o', label="Standard", linewidth=2, markersize=4)
        ax.plot(not_greedy["it"], not_greedy["sum_time"], '-o', label="Not Local Search", linewidth=2, markersize=4)

        ax.set_title(title)
        ax.set_xlabel("Iteration")
        ax.set_ylabel("Objective value")
        ax.grid(True, linestyle='--', alpha=0.5)
        ax.legend()

    plt.tight_layout()
    plt.savefig("plots/greedy_cmp_time.svg")
    plt.show()

# plot_standard_vs_not_greedy_time()
# plot_standard_vs_not_greedy_obj()
plot_standard_vs_not_local_search_obj()
plot_standard_vs_not_local_search_time()