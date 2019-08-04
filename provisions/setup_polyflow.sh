#!/bin/bash

# This script does the following:
# 	- docker initialization: creates the "polyflow" docker network if not already created
# 	- image pull: pulls postgres base image from dockerhub
# 	- container run: runs the above containers
# 	- data prep: download and insert data into running containers

docker rm -f bigdawg-postgres-catalog
docker rm -f bigdawg-postgres-swift
docker rm -f bigdawg-postgres-kepler

echo
echo "=============================================================="
echo "===== Creating the bigdawg docker network if not present ====="
echo "=============================================================="
$(docker network inspect polyflow &>/dev/null) || {   docker network create polyflow; }

echo
echo "========================================"
echo "===== Pulling images from Dockerhub====="
echo "========================================"
docker pull yanmendes/bigdawg-polyflow

echo
echo "============================="
echo "===== Running containers====="
echo "============================="
docker run -d -h bigdawg-postgres-catalog --net=polyflow -p 5400:5400 -p 8080:8080 -e "PGPORT=5400" -e "BDHOST=bigdawg-postgres-catalog" --name bigdawg-postgres-catalog yanmendes/bigdawg-polyflow
docker run -d -h bigdawg-postgres-kepler --net=polyflow -p 5401:5401 -e "PGPORT=5401" -e "BDHOST=bigdawg-postgres-kepler" --name bigdawg-postgres-kepler yanmendes/bigdawg-polyflow
docker run -d -h bigdawg-postgres-swift --net=polyflow -p 5402:5402 -e "PGPORT=5402" -e "BDHOST=bigdawg-postgres-swift" --name bigdawg-postgres-swift yanmendes/bigdawg-polyflow

echo
echo "========================"
echo "===== Loading data ====="
echo "========================"

# Download the swift dump
if [ -f "swift.dump" ]
then
       echo "Swift dump already exists. Skipping download"
else
       echo "Downloading the swift dump"
       curl -L -o swift.dump https://drive.google.com/uc?id=146L0rDu3U2jN4KM65YcfS-Ob1QYKaz3A
fi

# Download the Kepler dump
if [ -f "kepler.dump" ]
then
       echo "Kepler dump already exists. Skipping download"
else
       echo "Downloading the kepler dump"
       curl -L -o kepler.dump https://drive.google.com/uc?id=1c7nHHCfwqXcDtdhHNrdMPHe-cgxCW52N
fi

# postgres-catalog
docker exec -u root bigdawg-postgres-catalog mkdir -p /src/main/resources
docker cp ../src/main/resources/PostgresParserTerms.csv bigdawg-postgres-catalog:/src/main/resources
docker cp cluster_setup/polyflow-catalog/bdsetup bigdawg-postgres-catalog:/
docker exec bigdawg-postgres-catalog /bdsetup/setup.sh

# postgres-swift
docker exec -u root bigdawg-postgres-swift mkdir -p /bdsetup
docker cp swift_setup.sh bigdawg-postgres-swift:/bdsetup/
docker cp swift.dump bigdawg-postgres-swift:/bdsetup/
docker exec --user=root bigdawg-postgres-swift /bdsetup/swift_setup.sh

# postgres-kepler
docker exec -u root bigdawg-postgres-kepler mkdir -p /bdsetup
docker cp kepler_setup.sh bigdawg-postgres-kepler:/bdsetup/
docker cp kepler.dump bigdawg-postgres-kepler:/bdsetup/
docker exec --user=root bigdawg-postgres-kepler /bdsetup/kepler_setup.sh

echo
echo "======================================="
echo "===== Starting BigDAWG Middleware ====="
echo "======================================="
docker exec -d bigdawg-postgres-swift java -classpath "istc.bigdawg-1.0-SNAPSHOT-jar-with-dependencies.jar" istc.bigdawg.Main bigdawg-postgres-swift
docker exec -d bigdawg-postgres-kepler java -classpath "istc.bigdawg-1.0-SNAPSHOT-jar-with-dependencies.jar" istc.bigdawg.Main bigdawg-postgres-kepler
docker exec -it bigdawg-postgres-catalog java -classpath "istc.bigdawg-1.0-SNAPSHOT-jar-with-dependencies.jar" istc.bigdawg.Main bigdawg-postgres-catalog

echo
echo "================="
echo "===== Done. ====="
echo "================="