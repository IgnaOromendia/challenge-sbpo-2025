import random

random.seed(420)

num_tests = 20

for i in range(num_tests):

    with open(f'instances/random_instance_{i}.txt', "w+") as file:

        num_orders = random.randint(3, 10)
        num_aisles = random.randint(3, 10)
        num_elems = random.randint(3, 10)

        file.write(f'{num_orders} {num_elems} {num_aisles}\n')
        
        elems = range(num_elems)

        for _ in range(num_orders):
            dif_elements_in_order = random.randint(1, min(3, num_elems))
            type_elems_in_order = random.sample(elems, dif_elements_in_order)

            file.write(f'{dif_elements_in_order}')
            for type_elem in type_elems_in_order:
                file.write(f' {type_elem} {random.randint(1, 3)}')
            file.write('\n')

        for _ in range(num_aisles):
            dif_elements_in_aisle = random.randint(1, min(6, num_elems))
            type_elems_in_aisle = random.sample(elems, dif_elements_in_aisle)

            file.write(f'{dif_elements_in_aisle}')
            for type_elem in type_elems_in_aisle:
                file.write(f' {type_elem} {random.randint(1, 6)}')
            file.write('\n')

        LB = 0
        UB = random.randint(20, 40)
        file.write(f'{LB} {UB}\n')
