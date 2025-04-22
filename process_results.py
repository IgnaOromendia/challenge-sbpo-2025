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

result_path = "./results/base_results_parametric_gap25prec4.csv"

instancias = amount_of_orders_instance()


df = pd.read_csv(result_path)

df.insert(0, "instancia", df["ordenes"].apply(lambda x: instancias.get(int(x), "Total")))

df = df.sort_values(by="instancia", ascending=True)

new_row = {"instancia": "Tiempo total", "tiempo": df["tiempo"].sum() / 60}
df.loc[len(df)] = new_row

df.to_csv(result_path, index=False)