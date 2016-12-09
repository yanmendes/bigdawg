echo "STARTING POSTGRES SERVER in background"
/usr/lib/postgresql/9.4/bin/postgres -D /var/lib/postgresql/9.4/main  -p 5401 -c config_file=/etc/postgresql/9.4/main/postgresql.conf 2>&1 &

echo "WAITING 5 SECONDS"
sleep 5

echo "STARTING BIGDAWG"
java -classpath "istc.bigdawg-1.0-SNAPSHOT-jar-with-dependencies.jar" istc.bigdawg.Main bigdawg-postgres1
