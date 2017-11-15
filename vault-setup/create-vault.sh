#!/bin/bash


#############
# This script starts a vault instance with the container file system as the vault's storage backend.
# This vault instance is exclusively meant to be used as a demo, it *must not* be used in a production-like
# scenario!
#

VAULT_LOCAL_CONFIG='{"backend": {"file": {"path": "/vault/file"}}, "listener": { "TCP": {"address": "0.0.0.0:8200", "tls_disable": true}}, "default_lease_ttl": "168h", "max_lease_ttl": "720h"}'
docker run -d --cap-add=IPC_LOCK -e VAULT_LOCAL_CONFIG="${VAULT_LOCAL_CONFIG}" -p 8200:8200 --name local-vault vault:0.8.3 server

if [ $? -ne "0" ] ; then
    echo "Could not start vault server" >&2
    exit 1
fi

# give the vault instance some time to come up
sleep 5

export VAULT_ADDR="http://localhost:8200"

VAULT_INIT_OUTPUT="$(vault init --key-shares=1 --key-threshold=1)"

UNSEAL_KEY=$(echo "${VAULT_INIT_OUTPUT}" | grep 'Unseal Key 1:' | sed 's/^.*: \(.*\)$/\1/')
ROOT_TOKEN=$(echo "${VAULT_INIT_OUTPUT}" | grep 'Initial Root Token:' | sed 's/^.*: \(.*\)$/\1/')

cat <<EOF
${VAULT_INIT_OUTPUT}

+++
+++ Again: Please COPY the following two tokens to your local password store or similar,
+++        you will need them from time to time to work with your local Vault server!!
+++
+++ Unseal Key:         ${UNSEAL_KEY}
+++ Initial Root Token: ${ROOT_TOKEN}
+++
EOF

vault unseal "${UNSEAL_KEY}"
vault auth "${ROOT_TOKEN}"

# Create generic secret backend vault-demo-dev and write "secrets" for use in the demo
vault mount --path=vault-demo-dev --description="a secret backend for the vault demonstration" generic
vault write vault-demo-dev/all-apps spring.cloud.config.username=configserverUsername spring.cloud.config.password=configserverPassword
vault write vault-demo-dev/demo vault.demo.db.user=dbAccessor vault.demo.db.passord=dbPassword

echo 'path "vault-demo-dev/*" { capabilities = ["read"] }' | vault policy-write read-demo-dev -

echo "Going to issue a Vault token for TOKEN authentication with access to the locations vault-demo-dev/*:"
vault token-create -metadata="user=demoUser" -display-name=demoUser-token -policy=read-demo-dev -ttl="768h"








