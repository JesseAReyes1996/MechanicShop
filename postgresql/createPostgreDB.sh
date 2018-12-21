#! /bin/bash
echo "creating db named ... Jesse_DB"
createdb -h localhost -p $PGPORT "Jesse_DB"
pg_ctl status

echo "Copying csv files ... "
sleep 1
cp ../data/*.csv /tmp/Jesse/myDB/data/.

echo "Initializing tables .. "
sleep 1
psql -h localhost -p $PGPORT "Jesse_DB" < ../sql/create.sql
