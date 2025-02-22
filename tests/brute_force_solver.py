from itertools import chain, combinations
from instance_reader import read_instance

import sys

## Function to create all subsets of a set
def powerset(iterable):
    s = list(iterable)
    return chain.from_iterable(combinations(s, r) for r in range(len(s)+1))

if len(sys.argv) < 4:
    print("Usage: python3 brute_force_solver.py <instance_path> <output_path> <solution_path>")
    sys.exit()

instance_path = sys.argv[1]
output_path = sys.argv[2]
solution_path = sys.argv[3]

## Read input 
orders, aisles, num_orders, num_aisles, num_elems, LB, UB = read_instance(instance_path)

## Solve by brute force. Create all possible subsets of aisles, and for each one
## all possible subsets of orders and check satisfiability. Keep the best solution
all_orders = range(num_orders)
all_aisles = range(num_aisles)

best_order_yet = None
best_aisles_yet = None
best_value = None

for cur_aisles in powerset(all_aisles):
    if len(cur_aisles) == 0:
        continue

    for cur_orders in powerset(all_orders):
        
        elems_collected = 0
        for o in cur_orders:
            for e in range(num_elems):
                elems_collected += orders[o].get(e, 0)

        if elems_collected < LB or UB < elems_collected:
            continue

        fail = False
        for e in range(num_elems):
            elems_collected_type_e = 0
            for o in cur_orders:
                elems_collected_type_e += orders[o].get(e, 0)
            
            elems_in_aisle_type_e = 0
            for a in cur_aisles:
                elems_in_aisle_type_e += aisles[a].get(e, 0)
            
            if elems_collected_type_e > elems_in_aisle_type_e:
                fail = True
        
        if fail:
            continue

        if best_value == None or elems_collected / len(cur_aisles) > best_value:
            best_order_yet = cur_orders
            best_aisles_yet = cur_aisles
            best_value = elems_collected / len(cur_aisles)


print(f'Value obtained: {best_value if best_value != None else "Unfeasible"}')

with open(output_path, "w+") as output_file:
    if best_value == None:
        output_file.write("Unfeasible")
    else:
        output_file.write(f'{len(best_order_yet)}\n')
        for i in range(len(best_order_yet)):
            output_file.write(f'{best_order_yet[i]}\n')

        output_file.write(f'{len(best_aisles_yet)}\n')
        for i in range(len(best_aisles_yet)):
            output_file.write(f'{best_aisles_yet[i]}\n')

with open(solution_path, "w+") as solution_file:
    if best_value != None:
        solution_file.write(f'{best_value}')
    else:
        solution_file.write("Unfeasible")
