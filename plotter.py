import matplotlib.pyplot as plt
import pandas as pd

iterations  = [5,10,15]
colors      = ['red', 'blue','green']

def read_results(fileName):
    df = pd.read_csv(fileName)

    df['ordenes'] = df.iloc[:,0]
    df['pasillos'] = df.iloc[:,1]
    df['items'] = df.iloc[:,2]
    df['factibilidad'] = df.iloc[:,3]
    df['obj'] = df.iloc[:,4]
    df['tiempo'] = df.iloc[:,5]
    df['it'] = df.iloc[:,6]
    df['cplex_var'] = df.iloc[:,0] + df.iloc[:,1]

    return df.sort_values(by='cplex_var')

def add_to_plot(plt, x, y, color, label):
    plt.plot(x, y, color=color, label=label, linestyle='-', marker='o')

def plot(plt, xlabel, ylabel, title, fileName):
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    plt.title(title)
    plt.legend()
    plt.savefig('./graficos/' + fileName + '.png')
    plt.show()

def plot_time():
    for i in range(len(iterations)):
        it = iterations[i]

        df = read_results(f'results_{it}.csv')

        plt.grid(True)
        add_to_plot(plt, df['cplex_var'], df['tiempo'], colors[i], '#Iter ' + str(it))

    plot(plt, 'Variables', 'Tiempo (seg)', 'Tiempos', 'time')
    plt.clf()

def plot_it():
    for i in range(len(iterations)):
        it = iterations[i]

        df = read_results(f'results_{it}.csv')

        plt.grid(True)
        add_to_plot(plt, df['cplex_var'], df['it'], colors[i], '#Iter ' + str(it))

    plot(plt, 'Variables', 'Iteraciones', 'Iteraciónes máximas', 'max_it')
    plt.clf()

def plot_obj():
    for i in range(len(iterations)):
        it = iterations[i]

        df = read_results(f'results_{it}.csv')

        plt.grid(True)
        add_to_plot(plt, df['cplex_var'], df['obj'], colors[i], '#Iter ' + str(it))

    plot(plt, 'Variables', 'Valor objetivo', 'Valores objetivos', 'val')
    plt.clf()

plot_it()
plot_time()
plot_obj()

