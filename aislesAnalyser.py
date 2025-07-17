import os, sys, pandas as pd
import matplotlib.pyplot as plt

outputPath  = "output/output_"
datasetPath = "datasets/"
aislesPath = "aisles/"
plotPath = "aisles/plots/"
EPS = 1e-5

def processOutputFile(filePath):
    with open(filePath, 'r') as file: 
        lines = file.readlines()
        o = int(lines[0].split()[0])
        return [int(aisle) for aisle in lines[o+2:]]
    
def fillMaps(mapToFill, lines, from_ , to):
    for i, line in enumerate(lines[from_: to]):
        line = line.split()
        mapToFill[i] = {}
        for item, amount in zip(line[1::2], line[2::2]):
            mapToFill[i][int(item)] = int(amount)

def processRarenessItems(aisles, nItems):
    itemsAmount = [0] * nItems
    itemsAppearences = [0] * nItems

    for aisle, aisleItems in aisles.items():
        for item, amount in aisleItems.items():
            itemsAmount[item] += amount
            itemsAppearences[item] += 1

    return [1 / (itemsAmount[i] * (itemsAppearences[i] + EPS)) for i in range(nItems)]

def processInstanceFile(filePath):
    with open(filePath, 'r') as file:
        lines = file.readlines()
        firstLine = lines[0].split()

        nOrders = int(firstLine[0])
        nItems  = int(firstLine[1])
        nAisles = int(firstLine[2])

        aisles = {}
        orders = {}

        fillMaps(orders, lines, 1, nOrders + 1)
        fillMaps(aisles, lines, nOrders + 1, nOrders + nAisles + 1)

        rareness = processRarenessItems(aisles, nItems)

        return orders, aisles, nItems, rareness

def containedAisles(aisles, aisleToCompare, itemsToCompare):
    containedAisles = 0

    for aisle, items in aisles.items():
        if aisleToCompare == aisle: continue
        if len(itemsToCompare.keys()) < len(items.keys()): continue

        contained = True

        # Chequeamos que para todos los items de aisle haya menor canidad que en aisleToCompare
        for item, amount in items.items():
            if (amount > itemsToCompare.get(item, 0)): 
                contained = False
                break

        if contained: containedAisles += 1

    return containedAisles

def processAisle(rows, aisle, orders, aisles, isUsed, rareness):
    aisleItems = aisles[aisle];
    amountOfItems = sum(aisleItems.values())
    amountOfUniqueItems = len(aisleItems.keys())
    amountOfSatisfiableOrders = 0
    amountOfcontainedAisles = containedAisles(aisles, aisle, aisleItems)
    aisleRareness = 0

    for item, amount in aisleItems.items():
        aisleRareness += rareness[item] * amount

    for order, orderItems in orders.items():
        
        canBeSatisfied = True

        for orderItem, orderItemAmount in orderItems.items():
            if orderItemAmount > aisleItems.get(orderItem, 0):
                canBeSatisfied = False
                break
        
        if canBeSatisfied: amountOfSatisfiableOrders += 1

    rows.append({'aisle' : aisle, 
                 'items': amountOfItems,
                 'uniqueItems': amountOfUniqueItems,
                 'satOrders': amountOfSatisfiableOrders,
                 'contained': amountOfcontainedAisles,
                 'isUsed': isUsed,
                 'rareness': aisleRareness})

def make_bars(df, ax, col, title):
    d = df.sort_values(col, ascending=False, ignore_index=True)
    colors = ["red" if isUsed else "blue" for isUsed in d["isUsed"]]
    ax.bar(range(len(d)), d[col], color=colors)
    ax.set_title(title)
    ax.tick_params(axis="x", labelbottom=False)

def plot_instance(df, fileName):
    fig, axes = plt.subplots(2, 3, figsize=(18, 6))
    make_bars(df, axes[0][0], "uniqueItems", "Ítems únicos")
    make_bars(df, axes[0][1], "items", "Total ítems")
    make_bars(df, axes[0][2], "rareness", "Rareza")
    make_bars(df, axes[1][0], "satOrders","Órdenes satisfechas")
    make_bars(df, axes[1][1], "contained", "Pasillos contenidos")

    fig.suptitle(fileName, fontsize=14)
    fig.tight_layout()

    fig.savefig(plotPath + fileName + "_bars.png", dpi=150)
    plt.close(fig)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Falta dataset a procesar")
        exit()

    datasetPath += sys.argv[1] + "/"
    outputPath  += sys.argv[1] + "/"
    aislesPath  += sys.argv[1] + "/"
    plotPath    += sys.argv[1] + "/"

    if not os.path.exists(outputPath):
        print("El path " + outputPath + " no existe")
        exit()

    if not os.path.exists(aislesPath): os.mkdir(aislesPath)

    if not os.path.exists(plotPath): os.mkdir(plotPath)

    for fileName in os.listdir(datasetPath):
        inputFilePath  = os.path.join(datasetPath, fileName)
        outputFilePath = os.path.join(outputPath, fileName)
        orders = {}
        aisles = {}
        nItems = 0

        rows = []

        if os.path.isfile(outputFilePath):
            usedAisles = processOutputFile(outputFilePath)

        if os.path.isfile(inputFilePath):
            orders, aisles, nItems, rareness = processInstanceFile(inputFilePath)

        for aisle in aisles:
            processAisle(rows, aisle, orders, aisles, aisle in usedAisles, rareness)

        df = pd.DataFrame(rows)
        df.to_csv(os.path.join(aislesPath, fileName.replace("txt","csv")), index=False)
        plot_instance(df, fileName.replace(".txt", ""))
