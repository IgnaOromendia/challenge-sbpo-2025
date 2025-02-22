#!/bin/bash

python3 run_challenge.py . tests/instances/ tests/outputs/
python3 tests/check_solutions.py tests/instances/ tests/outputs/ tests/solutions/
