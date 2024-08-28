#! /bin/bash


function command_wrapper {
    log_message=''
    timestamp=`date +%G-%m-%eT%T.%3N`
    output_log=$($@ 2>&1 )
    log_message+="{\"timestamp\":\"$timestamp\","
    log_message+="\"version\":\"1.0.0\","
    log_message+="\"message\":\"$output_log\","
    log_message+="\"logger\":\"bash_logger\","
    log_message+="\"thread\":\"init_image_entrypoint_execution command: $@\","
    log_message+="\"path\":\"/entrypoint.sh\","
    log_message+="\"service_id\":\"eric-lcm-helm-executor\","
    log_message+="\"severity\":\"info\"}"

    echo $log_message
}

function init_sql {
    until
     pg_isready; do
       command_wrapper echo "Database instance $PGHOST is not ready. Waiting ..."
       sleep 3
    done


    #===DB-SQL-SCRIPT======
    cat << EOF | psql
SELECT 'CREATE DATABASE "$TARGET_DB"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$TARGET_DB')\gexec
DO \$\$
BEGIN
   IF EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = '$TARGET_USERNAME') THEN
      RAISE NOTICE 'Role $TARGET_USERNAME already exists.';
   ELSE
      BEGIN
         CREATE ROLE "$TARGET_USERNAME" LOGIN PASSWORD '$TARGET_PASSWORD';
         GRANT ALL PRIVILEGES ON DATABASE "$TARGET_DB" TO "$TARGET_USERNAME";
      EXCEPTION
         WHEN duplicate_object THEN
            RAISE NOTICE 'Role $TARGET_USERNAME was created by another transaction.';
      END;
   END IF;
END
\$\$;
EOF
    #===
   if [ $? -eq 0 ];
     then
       command_wrapper echo "SQL script loaded into the $TARGET_DB DB";
     else
       command_wrapper echo "PG login failed";
       exit 1;
   fi
}


init_sql

command_wrapper echo "The Init container completed preparation"
