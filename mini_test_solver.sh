#!/bin/bash

python3 run_challenge.py . tests/mini_instances/ tests/mini_outputs/
python3 tests/check_solutions.py tests/mini_instances/ tests/mini_outputs/ tests/solutions/
