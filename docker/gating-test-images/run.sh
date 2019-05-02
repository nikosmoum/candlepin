#!/bin/bash
#
# Script that downloads candlepin & postgres containers from the internal registry, and runs them as services
# using 'docker stack'. The postgresql container has a sql dump with the required test data and imports them
# on startup.

REGISTRY=docker-registry.engineering.redhat.com/candlepin

retry() {
    local -r -i max_attempts="$1"; shift
    local -r name="$1"; shift
    local -r cmd="$@"
    local -i attempt_num=1
    echo -n "Waiting for $name to start..."
    until ${cmd}
    do
        if (( attempt_num == max_attempts ))
        then
            echo "Attempt $attempt_num failed and there are no more attempts left!"
            return 1
        else
            echo -n '.'
            sleep $(( attempt_num++ ))
        fi
    done
    echo
}

evalrc() {
    if [ "$1" -ne "0" ]; then
        echo "$2"
        exit $1
    fi
}

echo "============ Pulling latest candlepin image from registry... ============ "
docker pull $REGISTRY/cp_latest_stage:latest
evalrc $? "cp_latest_stage:latest image pull was not successful."

echo "============ Pulling latest postgres image from registry... ============ "
docker pull $REGISTRY/cp_postgres:latest
evalrc $? "cp_postgres:latest image pull was not successful."

# need to be in a swarm in order to use the stack command
docker swarm init

echo "============ Deploying candlepin stack... ============ "
docker stack deploy -c cp-latest-stage/docker-compose.yml main_stack

retry 20 "candlepin" curl -k https://127.0.0.1:8443/candlepin/status
evalrc $? "Candlepin server did not start in time. Exiting..."

echo "Candlepin server is ready at 'https://127.0.0.1:8443/candlepin'..."
