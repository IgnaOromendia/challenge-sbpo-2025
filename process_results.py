import pandas as pd
import os
import re
import sys

def amount_of_orders_instance(dataset):
    folder_path = "./datasets/" + dataset
    result = {}
    for filename in os.listdir(folder_path):
        file_path = os.path.join(folder_path, filename)
        if os.path.isfile(file_path):
            with open(file_path, 'r') as file:
                content = file.read()
                match = re.search(r'\d+', content)
                if match:
                    result[int(match.group())] = filename.replace(".txt","")
    return result

def lambda_gap(obj, real_obj):
    error = abs(obj - real_obj)
    return 0 if error < 1e-4 else error

def process_file(result_path, dataset):
    instancias = amount_of_orders_instance(dataset)

    df = pd.read_csv(result_path, dtype={"ordenes": int, "pasillos": int, "usados": int, "it": int})

    df_gt = pd.read_csv("./results/ground_truth_set_" + dataset + ".csv")

    df.insert(0, "instancia", df["ordenes"].apply(lambda x: instancias.get(int(x), "Total")))

    df = df.sort_values(by="instancia", ascending=True, ignore_index=True)

    df.insert(5, "expected", df_gt["obj"]) 
    df.insert(6, "error", df.apply(lambda row: lambda_gap(row["obj"], df_gt.loc[row.name, "obj"]), axis=1))

    new_row = {"instancia": "Tiempo total", "tiempo": df["tiempo"].sum() / 60}
    df.loc[len(df)] = new_row

    df["ordenes"] = df["ordenes"].fillna(0).astype(int)
    df["pasillos"] = df["pasillos"].fillna(0).astype(int)
    df["usados"] = df["usados"].fillna(0).astype(int)
    df["it"] = df["it"].fillna(0).astype(int)

    df.to_csv(result_path, index=False)

if __name__ == "__main__":
    if (len(sys.argv) < 3): 
        print("<path al archivo a procesar> <dataset>")
        exit()
    
    process_file(sys.argv[1], sys.argv[2])
    