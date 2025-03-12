#!/bin/bash

directory="datasets/a"
output="output/"

python3 run_challenge.py . $directory $output

for file in "$directory"/*; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "Result for $filename"
        python3 "checker.py" $directory"/"$filename $output$filename
    fi
done

