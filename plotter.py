import matplotlib.pyplot as plt
import pandas as pd
import os
import numpy as np

# Leemos los archivos results y nos guardamos para cada prueba sus datos
folder_path = "./results/"

result = {}
total_times = {}
approach = []

for filename in os.listdir(folder_path):
    file_path = os.path.join(folder_path, filename)
    if ("ground_truth" in filename): continue
    if os.path.isfile(file_path):
        name = filename.replace(".csv", "").replace("results_", "")
        df = pd.read_csv(file_path)
        total_times[name] = float(df.iloc[-1]["tiempo"])
        result[name] = df.drop(df.index[-1])
        approach.append(name)

instances = result["parametric_default"]["instancia"].tolist()
  
bar_width = 0.2  
colors = plt.cm.tab20.colors 

def plot_errors():
    fig, axes = plt.subplots(4, 5, figsize=(13, 7)) 
    
    for i, (instance, ax) in enumerate(zip(instances, axes.flat)):
        errors = []

        for name in approach:  # Asegura el orden correcto
            df = result[name]
            if instance in df["instancia"].values:
                error = df[df["instancia"] == instance]["error"].values[0]
                errors.append(error)

        x = np.arange(len(errors))  # Posiciones en el eje X
        bar_colors = [colors[j % len(colors)] for j in range(len(errors))] 
        ax.bar(x, errors, bar_width, color=bar_colors)
        ax.set_title(instance)
        ax.set_xticks(x)
        ax.set_xticklabels([])
        ax.set_ylabel("Error")

    # Crear la leyenda en la parte superior centrada
    legend_handles = [plt.Line2D([0], [0], color=colors[i % len(colors)], lw=4, label=label) for i, label in enumerate(approach)]
    fig.legend(
        handles=legend_handles,
        loc='center right',
        title="Estrategias",
        bbox_to_anchor=(1, 0.5)
    )

    plt.suptitle("Errores por instancia")
    plt.tight_layout(rect=[0, 0, 0.9, 1])
    plt.savefig("./plots/plot_errores_dataset_a.png")
    plt.show()

def plot_total_times():
    fig, ax = plt.subplots(figsize=(10, 6))

    x = np.arange(len(approach)) 
    y = [total_times[name] for name in approach]  
    bar_colors = [colors[i % len(colors)] for i in range(len(approach))] 

    ax.bar(x, y, color=bar_colors, width=bar_width)

    ax.set_title("Tiempos Totales")
    ax.set_xlabel("Approach")
    ax.set_ylabel("Minutos")
    ax.set_xticks(x)
    ax.set_xticklabels(approach, rotation=45)
    ax.grid(axis='y', linestyle='--', alpha=0.5)
    
    plt.tight_layout()
    plt.savefig("./plots/plot_tiempos_dataset_a.png")
    plt.show()


plot_errors()
plot_total_times()