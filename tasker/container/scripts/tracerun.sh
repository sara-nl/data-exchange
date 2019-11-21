#!/usr/bin/env bash

ALGORITHM_FILE="$1"
DATA_PATH="$2"
STDOUT_FILE="$3"
STDERR_FILE="$4"
TRACE_FILE="$5"

strace -e trace=network,openat -f sh -c "python \"$ALGORITHM_FILE\" \"$DATA_PATH\" > \"$STDOUT_FILE\" 2> \"$STDERR_FILE\"" 2> "$TRACE_FILE"