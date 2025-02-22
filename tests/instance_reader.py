## Code to read an instance
def read_instance(instance_path):

    ## Variables of the problem
    orders = {}
    aisles = {}
    num_orders, num_aisles, num_elems = -1, -1, -1
    LB, UB = -1, -1

    ## Read input
    with open(instance_path, "r") as instance_file:
        num_orders, num_elems, num_aisles = [int(x) for x in next(instance_file).split(" ")]
        
        orders = {o: {} for o in range(num_orders)}
        aisles = {a: {} for a in range(num_aisles)}

        for o in range(num_orders):
            line = [int(x) for x in next(instance_file).split(" ")]
            N = line[0]
            for i in range(N):
                elem_type, num_elems_type = line[2*i+1], line[2*i+2]
                orders[o][elem_type] = num_elems_type
        
        for a in range(num_aisles):
            line = [int(x) for x in next(instance_file).split(" ")]
            N = line[0]
            for i in range(N):
                elem_type, num_elems_type = line[2*i+1], line[2*i+2]
                aisles[a][elem_type] = num_elems_type

        LB, UB = [int(x) for x in next(instance_file).split(" ")]
    
    return orders, aisles, num_orders, num_aisles, num_elems, LB, UB

## Code to read the solution of an instance
def read_output(output_path):
    orders = None
    aisles = None
    num_orders = None
    num_aisles = None

    with open(output_path, "r") as output_file:
        num_orders = int(next(output_file))

        orders = []
        for _ in range(num_orders):
            o = int(next(output_file))
            orders.append(o)
        
        num_aisles = int(next(output_file))
        
        aisles = []
        for _ in range(num_aisles):
            a = int(next(output_file))
            aisles.append(a)

    return orders, aisles, num_orders, num_aisles