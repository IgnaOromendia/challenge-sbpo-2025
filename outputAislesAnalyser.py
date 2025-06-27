import os, sys, pandas as pd

outputPath  = "output/output_"
datasetPath = "datasets/"
aislesOutputPath = "aisles/output_"
aislesInputPath  = "aisles/input_"

def processOutputFile(filePath):
    with open(filePath, 'r') as file: 
        lines = file.readlines()
        o = int(lines[0].split()[0])
        return [int(aisle) for aisle in lines[o+2:]]
    
def processItems(mapToFill, lines, from_ , to):
    for i, line in enumerate(lines[from_: to]):
        line = line.split()
        mapToFill[i] = {}
        for item, amount in zip(line[1::2], line[2::2]):
            mapToFill[i][int(item)] = int(amount)

def processInstanceFile(filePath):
    with open(filePath, 'r') as file:
        lines = file.readlines()
        firstLine = lines[0].split()

        nOrders = int(firstLine[0])
        nItems  = int(firstLine[1])
        nAisles = int(firstLine[2])

        aisles = {}
        orders = {}

        processItems(orders, lines, 1, nOrders + 1)
        processItems(aisles, lines, nOrders + 1, nOrders + nAisles + 1)

        return orders, aisles, nItems

def processAisle(rows, aisle, orders, aisleItems):
    amountOfItems = sum(aisleItems.values())
    amountOfUniqueItems = len(aisleItems.keys())
    amountOfSatisfiableOrders = 0

    for order, orderItems in orders.items():
        
        canBeSatisfied = True

        for orderItem, orderItemAmount in orderItems.items():
            if orderItemAmount > aisleItems.get(orderItem, 0):
                canBeSatisfied = False
                break
        
        if canBeSatisfied: amountOfSatisfiableOrders += 1

    rows.append({'pasillo' : aisle, 
                    'items': amountOfItems,
                    'uniqueItems': amountOfUniqueItems,
                    'satOrders': amountOfSatisfiableOrders})

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Falta dataset a procesar")
        exit()

    datasetPath += sys.argv[1] + "/"
    outputPath  += sys.argv[1] + "/"
    aislesOutputPath += sys.argv[1] + "/"
    aislesInputPath  += sys.argv[1] + "/"

    if not os.path.exists(outputPath):
        print("El path " + outputPath + " no existe")
        exit()

    if not os.path.exists(aislesOutputPath):
        os.mkdir(aislesOutputPath)

    if not os.path.exists(aislesInputPath):
        os.mkdir(aislesInputPath)

    ### Analiza sobre las instancias
    for fileName in os.listdir(datasetPath):
        inputFilePath  = os.path.join(datasetPath, fileName)

        rows = []

        if os.path.isfile(inputFilePath):
            orders, aisles, nItems = processInstanceFile(inputFilePath)

        for aisle in aisles:
            processAisle(rows, aisle, orders, aisles[aisle])

        df = pd.DataFrame(rows)
        df.to_csv(os.path.join(aislesInputPath, fileName.replace("txt","csv")), index=False)

    ### Analiza sobre el output
    for fileName in os.listdir(outputPath):
        outputFilePath = os.path.join(outputPath, fileName)
        inputFilePath  = os.path.join(datasetPath, fileName)
        orders = {}
        aisles = {}
        nItems = 0

        rows = []
  
        if os.path.isfile(outputFilePath):
            usedAisles = processOutputFile(outputFilePath)

        if os.path.isfile(inputFilePath):
            orders, aisles, nItems = processInstanceFile(inputFilePath)

        for usedAisle in usedAisles:
            processAisle(rows, usedAisle, orders, aisles[usedAisle])

        df = pd.DataFrame(rows)
        df.to_csv(os.path.join(aislesOutputPath, fileName.replace("txt","csv")), index=False)


