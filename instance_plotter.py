import matplotlib.pyplot as plt
import os

def read_maps(filename):
    with open(filename, 'r') as f:
        n, T, m = map(int, f.readline().split())
        maps = []
        for _ in range(n):
            parts = f.readline().split()
            s = int(parts[0])
            entries = list(map(int, parts[1:]))
            assert len(entries) == 2 * s
            d = {entries[2*i]: entries[2*i+1] for i in range(s)}
            maps.append(d)
        for _ in range(m):
            _ = f.readline()
    return maps

def compute_sizes(maps):
    return [sum(d.values()) for d in maps]

def plot_all_histograms():
    fig, axes = plt.subplots(4, 5, figsize=(14, 10))  # smaller figure
    fig.suptitle('Histogram of Dictionary Sizes per Instance (Filtered < 5)', fontsize=14)

    for i in range(15):
        number = i + 1
        filename = f'datasets/b/instance_{number:04}.txt'
        if not os.path.exists(filename):
            print(f"Warning: {filename} not found, skipping.")
            continue

        maps = read_maps(filename)
        sizes = [s for s in compute_sizes(maps) if s < 10]  # filtering here

        ax = axes[i // 5][i % 5]
        ax.hist(sizes, bins=range(6), edgecolor='black', align='left')
        ax.set_title(f'Instance {number}', fontsize=10)
        ax.set_xlabel('Size', fontsize=8)
        ax.set_ylabel('Freq', fontsize=8)
        ax.tick_params(axis='both', which='major', labelsize=8)

    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig('histograms_filtered.png', dpi=150)  # Save to file
    plt.show()

if __name__ == '__main__':
    plot_all_histograms()
