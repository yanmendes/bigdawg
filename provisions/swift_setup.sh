#!/bin/bash

su postgres <<'EOF'
psql -c "create database swift owner pguser"
psql -d swift < /bdsetup/swift.dump
EOF