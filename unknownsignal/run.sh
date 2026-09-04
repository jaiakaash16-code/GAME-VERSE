#!/usr/bin/env bash
# UNKNOWN SIGNAL — compile and launch (works in Git Bash on Windows too)
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out $(find server/src -name '*.java')
exec java -cp out com.freebuff.signal.Main "$@"