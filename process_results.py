import pandas as pd
import os
import re

def amount_of_orders_instance():
    folder_path = "./datasets/a"
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

result_path = "./results/results_parametric_default.csv"

instancias = amount_of_orders_instance()

df = pd.read_csv(result_path)
df_gt = pd.read_csv("./results/ground_truth_set_a.csv")

df.insert(0, "instancia", df["ordenes"].apply(lambda x: instancias.get(int(x), "Total")))

df = df.sort_values(by="instancia", ascending=True, ignore_index=True)


df.insert(3, "error", df.apply(lambda row: lambda_gap(row["obj"], df_gt.loc[row.name, "obj"]), axis=1))

new_row = {"instancia": "Tiempo total", "tiempo": df["tiempo"].sum() / 60}
df.loc[len(df)] = new_row

df.to_csv(result_path, index=False)
