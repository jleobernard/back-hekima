FILE=`date +%Y%m%dH%H%M`
docker exec -i postgres_postgres_1 pg_dump notes -U notes > /opt/data/backups/psql/notes.$FILE.sql