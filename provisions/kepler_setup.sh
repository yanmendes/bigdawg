#!/bin/bash

su postgres <<'EOF'
psql -c "create database kepler owner pguser"
psql -d kepler < /bdsetup/kepler.dump
EOF