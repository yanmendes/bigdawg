#!/bin/bash

# Build the catalog database
cd /bdsetup
psql -c "create database bigdawg_catalog"
psql -f catalog_ddl.sql -d bigdawg_catalog
psql -f catalog_inserts.sql -d bigdawg_catalog
sleep 5
psql -f monitor_ddl.sql -d bigdawg_catalog
psql -c "create database bigdawg_schemas"
psql -f swift_schemas_ddl.sql -d bigdawg_schemas
psql -f kepler_schemas_ddl.sql -d bigdawg_schemas
sleep 5
psql -c "create database logs owner pguser"
psql -f logs_ddl.sql -d logs
