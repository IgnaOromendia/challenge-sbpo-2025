import matplotlib.pyplot as plt
import pandas as pd

iterations  = [5,10,15]
colors      = [['red', 'blue','green'], ['purple', 'orange']]
strategies  = ['binary', 'parametric']

def read_results(fileName):
    df = pd.read_csv(fileName)

    df['ordenes'] = df.iloc[:,0]
    df['pasillos'] = df.iloc[:,1]
    df['items'] = df.iloc[:,2]
    df['factibilidad'] = df.iloc[:,3]
    df['obj'] = df.iloc[:,4]
    df['tiempo'] = df.iloc[:,5]
    df['it'] = df.iloc[:,6]
    df['cplex'] = df['ordenes'] + df['pasillos'] + df['items']

    print("Tiempo total de " + fileName + ": " + str(df['tiempo'].sum() / 60))

    return df.sort_values(by='cplex')

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
    for s, strat in enumerate(strategies):
        for i in range(len(iterations)):
            if strat == 'parametric' and i > 1: continue
            it = iterations[i]

            df = read_results(f'results_{strat}_{it}.csv')

            plt.grid(True)
            add_to_plot(plt, df['cplex'], df['tiempo'], colors[s][i], strat + ' #Iter ' + str(it))

    plot(plt, 'Variables + restricciónes', 'Tiempo (seg)', 'Tiempos', 'time')
    plt.clf()

def plot_it():
    for s, strat in enumerate(strategies):
        for i in range(len(iterations)):
            if strat == 'parametric' and i > 1: continue
            it = iterations[i]

            df = read_results(f'results_{strat}_{it}.csv')

            plt.grid(True)
            add_to_plot(plt, df['cplex'], df['it'], colors[s][i], strat + ' #Iter ' + str(it))

    plot(plt, 'Variables + restricciónes', 'Iteraciones', 'Iteraciónes máximas', 'max_it')
    plt.clf()

def plot_obj():
    for s, strat in enumerate(strategies):
        for i in range(len(iterations)):
            if strat == 'parametric' and i > 1: continue
            it = iterations[i]

            df = read_results(f'results_{strat}_{it}.csv')

            plt.grid(True)
            add_to_plot(plt, df['cplex'], df['obj'], colors[s][i], strat + ' #Iter ' + str(it))

    plot(plt, 'Variables + restricciónes', 'Valor objetivo', 'Valores objetivos', 'val')
    plt.clf()

plot_it()
plot_time()
plot_obj()

