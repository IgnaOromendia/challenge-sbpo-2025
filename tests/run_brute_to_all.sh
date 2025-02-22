#!/bin/bash

directory="instances/"

for file in "$directory"*; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        python3 "brute_force_solver.py" "instances/"$filename "outputs_baseline/"$filename "solutions/"$filename
    fi
done