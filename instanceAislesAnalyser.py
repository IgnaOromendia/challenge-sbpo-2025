import os, sys

datasetPath = "datasets/"
aislesPath = "aisles/"

def processAisleFile(lines):
    firstLine = lines[0].split()

    orders  = int(firstLine[0])
    naisles = int(firstLine[2])

    aisles = {}

    for i, line in enumerate(lines[orders + 1 : orders + naisles + 1]):
        line = line.split()
        aisles[i] = {}
        for item, amount in zip(line[1::2], line[2::2]):
            aisles[i][int(item)] = int(amount)

    return aisles

def findContainedAisles(aisles, aisleToCompare, itemsToCompare):
    containedAisles = []

    for aisle, items in aisles.items():
        if aisleToCompare == aisle: continue
        if len(itemsToCompare.keys()) < len(items.keys()): continue

        contained = True

        # Chequeamos que para todos los items de aisle haya menor canidad que en aisleToCompare
        for item, amount in items.items():
            if (amount > itemsToCompare.get(item, 0)): 
                contained = False
                break

        if contained: containedAisles.append(aisle)

    return containedAisles

def writeResults(resultPath, aislesContained):
    if all(len(items) == 0 for _, items in aislesContained.items()): return

    with open(resultPath, 'w') as resultFile:
        for aisle, items in aislesContained.items():

            if len(items) == 0: continue

            resultFile.write(str(aisle) + " ")

            for item in items:
                resultFile.write(str(item) + " ")

            resultFile.write("\n")

if __name__ == "__main__":
    if len(sys.argv) < 2 : 
        print("Falta dataset a procesar")
        exit()

    datasetPath += sys.argv[1] + "/"
    aislesPath  += sys.argv[1] + "/"

    if not os.path.exists(aislesPath):
        os.mkdir(aislesPath)

    for fileName in os.listdir(datasetPath):
        filePath = os.path.join(datasetPath, fileName)
        if os.path.isfile(filePath):
            with open(filePath, 'r') as file:
                aisles = processAisleFile(file.readlines())
                aislesContained = {}

                for aisle, items in aisles.items():
                    aislesContained[aisle] = findContainedAisles(aisles, aisleToCompare=aisle, itemsToCompare=items)

                writeResults(os.path.join(aislesPath, "contained_aisles_" + fileName), aislesContained)


            