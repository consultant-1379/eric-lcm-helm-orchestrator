#!/bin/bash


cmd_wrapper(){
    # wrapper function works with two positional arguments
    # first one is the command to execute and second for
    # managing script termination.
    # First argument would be parsed with IFS to determine
    # its stdout, stderr and rc_code accordingly and for
    # wrapping up with specific msg format.
    # Second argument can by only "skip_rc" for omit raising an script exit
    local cmd="$1"
    local rc="$2"
    if [[ ! -z $rc ]] && [[ $rc != "skip_rc" ]]; then
        echo "Wrapper format is incorrect. Usage: 'cmd_wrapper \"command\" - for regular flows or cmd_wrapper \"command\" \"skip_rc\" - for skipping possible return code"
        exit 1
    fi
    {
        IFS=$'\n' read -r -d '' std_err;
        IFS= read -r -d '' return_code;
        IFS=$'\n' read -r -d '' std_out;
    } < <((printf '\0%s\n\0' "$($cmd; printf '\0%d' "${?}" 1>&2)" 1>&2) 2>&1)
    msg="{\"timestamp\":\"$(date +"%G-%m-%dT%T.%3N%:z")\",\"version\":1.0.0,`
          `\"message\":\"$std_out,err:$std_err,rc:$return_code,cmd:$cmd\",`
          `\"thread\":\"$(basename "$0")\",`
          `\"service_id\":\"unknown\",`
          `\"severity\":\"info\"}"
    if [[ $return_code -gt 0 ]]; then
        if [[ $rc == "skip_rc" ]]; then
            echo "$msg" | tee -a "$HE_LOG"
        else
            echo "$msg" | tee -a "$HE_LOG"
            exit "$return_code"
        fi
    else
        echo "$msg" | tee -a "$HE_LOG"
    fi
}


mtls_key_preperation(){

    cp -rf /opt/mtls "$TMPDIR/"
    find "$TMPDIR/mtls/" -name "*.pem" -not -path "*/\.*" -exec chmod 600 {} \;
    cmd_wrapper "echo mTLS keys preparation - Done"
}


key_cert_processing(){

    cmd_wrapper "openssl pkcs8 -topk8 -inform PEM -in $PGSSLKEY -outform DER -out $PGSSLKEYDER -v1 PBE-MD5-DES -nocrypt"
    cmd_wrapper "echo mTLS private key preparation for DB - Done"
}


main(){

    cmd_wrapper "echo Daemon function is running"
    cmd_wrapper "echo Daemon function mode: Initialization"
    mtls_key_preperation
    key_cert_processing
    cmd_wrapper "echo Daemon function mode: Tracking"
    while true; do
        inotifywait -r -e modify /opt/mtls/
        mtls_key_preperation
        key_cert_processing
    done
}

main "$@"
