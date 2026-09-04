#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
if [ ! -d venv ]; then
  python -m venv venv
fi
if [ -x venv/Scripts/python.exe ]; then
  PYBIN=venv/Scripts/python.exe
else
  PYBIN=venv/bin/python
fi
"$PYBIN" -m pip install -q -r requirements.txt
exec "$PYBIN" app.py