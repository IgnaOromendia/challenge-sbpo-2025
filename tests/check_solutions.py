from instance_reader import read_instance, read_output

import numpy as np
import os
import sys

if len(sys.argv) < 4:
    print("Usage: python3 check_solutions.py <instance_dir> <output_dir> <solution_dir>")
    sys.exit()

instance_dir = sys.argv[1]
output_dir = sys.argv[2]
solution_dir = sys.argv[3]

tolerance = 0.001

for filename in os.listdir(instance_dir):
    orders, aisles, num_orders, num_aisles, num_elems, LB, UB = read_instance(instance_dir + filename)
    found_orders, found_aisles, found_num_orders, found_num_aisles = read_output(output_dir + filename)
    
    optimum = None
    with open(solution_dir + filename, "r") as solution_file:
        optimum = float(next(solution_file))

    ## Check solution is well formed
    if len(found_orders) != found_num_orders:
        print(f'Failed in {filename}: number of orders does not coincide with returned orders')
        print(f'Number is {found_num_orders}, while real length is {len(found_orders)}')
        continue
    if len(found_aisles) != found_num_aisles:
        print(f'Failed in {filename}: number of aisles does not coincide with returned aisles')
        continue

    ## Check solution is feasible
    num_elements_in_sol = 0

    fail = False
    for e in range(num_elems):
        num_elements_in_sol_type_e = 0
        for o in found_orders:
            num_elements_in_sol_type_e += orders[o].get(e, 0)
            num_elements_in_sol += orders[o].get(e, 0)
        
        num_elements_in_aisle_type_e = 0
        for a in found_aisles:
            num_elements_in_aisle_type_e += aisles[a].get(e, 0)

        if num_elements_in_sol_type_e > num_elements_in_aisle_type_e:
            print(f'Failed in {filename}: number of elements of type {e} in orders is bigger than in aisles')
            fail = True

    if fail:
        continue

    if LB > num_elements_in_sol:
        print(f'Failed in {filename}: solution has {num_elements_in_sol} elements, but LB is {LB}')
        continue

    if UB < num_elements_in_sol:
        print(f'Failed in {filename}: solution has {num_elements_in_sol} elements, but RB is {LB}')
        continue
    
    if np.abs(num_elements_in_sol / found_num_aisles - optimum) > tolerance:
        print(f'Failed in {filename}: solution has value {num_elements_in_sol / found_num_aisles}, but {optimum} is the optimal')
        continue

    print(f'Success at {filename}!')

    ## Check solution is optimal
    
