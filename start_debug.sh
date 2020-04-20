#!/bin/bash

pip install -qr requirements.txt

export FLASK_ENV=development
export FLASK_APP=routes/__init__.py 
export IRENE_URL='http://localhost:4444'
export DATA_PATH='/media/flash/trec-covid'

# ensure tables created:
sqlite3 labels.db < schema.sql

python -m flask run --port 1234