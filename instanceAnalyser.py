import os, sys, pandas as pd
import matplotlib.pyplot as plt

outputPath  = "output/output_"
datasetPath = "datasets/"
aislesPath = "aisles/"
ordersPath = "orders/"
aislesPlotPath = "aisles/plots/"
ordersPlotPath = "orders/plots/"
EPS = 1e-5

class Plotter:
    def __init__(self):
        pass

    def makeBars(self, df, ax, col, title):
        if not col in df.columns: return
        d = df.sort_values(col, ascending=False, ignore_index=True)
        colors = ["red" if isUsed else "blue" for isUsed in d["isUsed"]]
        ax.bar(range(len(d)), d[col], color=colors)
        ax.set_title(title)
        ax.tick_params(axis="x", labelbottom=False)
    

    def plotAisleInstanceData(self, df, fileName):
        df.to_csv(os.path.join(aislesPath, fileName.replace("txt","csv")), index=False)
        fig, axes = plt.subplots(2, 3, figsize=(18, 6))
        axes = axes.flatten()
        self.makeBars(df, axes[0], "uniqueItems", "Ítems únicos")
        self.makeBars(df, axes[1], "items", "Total ítems")
        self.makeBars(df, axes[2], "rareness", "Rareza")
        self.makeBars(df, axes[3], "satOrders","Órdenes satisfechas")
        self.makeBars(df, axes[4], "contained", "Pasillos contenidos")

        fig.suptitle(fileName, fontsize=14)
        fig.tight_layout()

        fig.savefig(fileName.replace(".txt", "") + fileName + "_bars.png", dpi=150)
        plt.close(fig)

    def plotOrderInstanceData(self, df, fileName):
        df.to_csv(os.path.join(ordersPath, fileName.replace("txt","csv")), index=False)
        df.to_csv(os.path.join(aislesPath, fileName.replace("txt","csv")), index=False)
        fig, axes = plt.subplots(2, 2, figsize=(18, 6))
        axes = axes.flatten()
        self.makeBars(df, axes[0], "uniqueItems", "Ítems únicos")
        self.makeBars(df, axes[1], "items", "Total ítems")
        self.makeBars(df, axes[2], "rareness", "Rareza")
        self.makeBars(df, axes[3], "cost ", "Costo estimado de satisfacerla") # cantidad de pasillo minimos

        fig.suptitle(fileName, fontsize=14)
        fig.tight_layout()

        fig.savefig(fileName.replace(".txt", "") + fileName + "_bars.png", dpi=150)
        plt.close(fig)

class Processor:
    def __init__(self, inputFilePath, outputFilePath, fileName):
        self.readInputFile(inputFilePath)
        self.readOutputFile(outputFilePath)
        self.calulateRarenessItems()
        self.plotter = Plotter()
        self.fileName = fileName
        
    def calulateRarenessItems(self):
        aisleItemsAmount = [0] * self.nItems
        aisleItemsAppearences = [0] * self.nItems
        orderItemsAmount = [0] * self.nItems
        orderItemsAppearences = [0] * self.nItems

        for aisle, aisleItems in self.aisles.items():
            for item, amount in aisleItems.items():
                aisleItemsAmount[item] += amount
                aisleItemsAppearences[item] += 1

        self.aisleRareness = [1 / (aisleItemsAmount[i] * (aisleItemsAppearences[i] + EPS)) for i in range(self.nItems)]

        for order, orderItems in self.orders.items():
            for item, amount in orderItems.items():
                orderItemsAmount[item] += amount
                orderItemsAppearences[item] += 1

        self.aisleRareness = [0] * self.nItems
        self.orderRareness = [0] * self.nItems

        for i in range(self.nItems):
            self.aisleRareness[i] = 1 / (aisleItemsAmount[i] * (aisleItemsAppearences[i] + EPS))
            self.orderRareness[i] = 1 / (orderItemsAmount[i] * (orderItemsAppearences[i] + EPS))     

    def fillMaps(self, mapToFill, lines, from_ , to):
        for i, line in enumerate(lines[from_: to]):
            line = line.split()
            mapToFill[i] = {}
            for item, amount in zip(line[1::2], line[2::2]):
                mapToFill[i][int(item)] = int(amount)
     
    def readInputFile(self, filePath):
        if not os.path.isfile(filePath): 
            print("No se encontró " + filePath)
            exit()


        with open(filePath, 'r') as file:
            lines = file.readlines()
            firstLine = lines[0].split()

            nOrders = int(firstLine[0])
            self.nItems  = int(firstLine[1])
            nAisles = int(firstLine[2])

            self.aisles = {}
            self.orders = {}

            self.fillMaps(self.orders, lines, 1, nOrders + 1)
            self.fillMaps(self.aisles, lines, nOrders + 1, nOrders + nAisles + 1)

    def readOutputFile(self, filePath):
        if not os.path.isfile(filePath): 
            print("No se encontró " + filePath)
            exit()

        with open(filePath, 'r') as file: 
            lines = file.readlines()
            o = int(lines[0].split()[0])
            self.usedOrders = [int(order) for order in lines[2:o+2]]
            self.usedAisles = [int(aisle) for aisle in lines[o+2:]]
    
    def processAisle(self, rows, aisle):
        aisleItems = self.aisles[aisle];
        amountOfItems = sum(aisleItems.values())
        amountOfUniqueItems = len(aisleItems.keys())
        amountOfSatisfiableOrders = 0
        amountOfcontainedAisles = self.containedAisles(aisle, aisleItems)
        aisleRareness = 0

        for item, amount in aisleItems.items():
            aisleRareness += self.aisleRareness[item] * amount

        for order, orderItems in self.orders.items():
            
            canBeSatisfied = True

            for orderItem, orderItemAmount in orderItems.items():
                if orderItemAmount > aisleItems.get(orderItem, 0):
                    canBeSatisfied = False
                    break
            
            if canBeSatisfied: amountOfSatisfiableOrders += 1

        rows.append({'aisle' : aisle, 
                    'items': amountOfItems,
                    'uniqueItems': amountOfUniqueItems,
                    'contained': amountOfcontainedAisles,
                    'satOrders': amountOfSatisfiableOrders,
                    'isUsed': aisle in self.usedAisles,
                    'rareness': aisleRareness})

    def processAisles(self):
        rows = []

        for aisle in self.aisles:
            self.processAisle(rows, aisle)

        self.plotter.plotAisleInstanceData(pd.DataFrame(rows), self.fileName)

    def containedAisles(self, aisleToCompare, itemsToCompare):
        containedAisles = 0

        for aisle, items in self.aisles.items():
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

    def processOrder(self, rows, order):
        orderItems = self.orders[order]
        amountOfItems = sum(orderItems.values())
        amountOfUniqueItems = sum(orderItems.keys())
        orderRareness = 0

        for item, amount in orderItems.items():
            orderRareness += self.orderRareness[item] * amount

        rows.append({'order' : order, 
                    'items': amountOfItems,
                    'uniqueItems': amountOfUniqueItems,
                    'isUsed': order in self.usedOrders,
                    'rareness': orderRareness})

    def processOrders(self):
        rows = []
        
        for order in self.orders:
            self.processOrder(rows, order)

        self.plotter.plotOrderInstanceData(pd.DataFrame(rows), self.fileName)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Falta dataset a procesar")
        exit()

    datasetPath += sys.argv[1] + "/"
    outputPath  += sys.argv[1] + "/"
    aislesPath  += sys.argv[1] + "/"
    ordersPath  += sys.argv[1] + "/"

    aislesPlotPath += sys.argv[1] + "/"
    ordersPlotPath += sys.argv[1] + "/"

    if not os.path.exists(outputPath):
        print("El path " + outputPath + " no existe")
        exit()

    if not os.path.exists(aislesPath): os.mkdir(aislesPath)

    if not os.path.exists(ordersPath): os.mkdir(ordersPath)

    if not os.path.exists(aislesPlotPath): os.mkdir(aislesPlotPath)

    if not os.path.exists(ordersPlotPath): os.mkdir(ordersPlotPath)

    for fileName in os.listdir(datasetPath):
        inputFilePath  = os.path.join(datasetPath, fileName)
        outputFilePath = os.path.join(outputPath, fileName)

        processor = Processor(inputFilePath, outputFilePath, fileName)

        processor.processAisles()
        processor.processOrders()
