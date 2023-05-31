#!/usr/bin/env sh


function mk_keystore_from_pem() {
    PEM_CRT_PATH=$1
    PEM_KEY_PATH=$2
    PFX_PATH=$3
    PFX_PASSWORD=$4
    INTERMEDIATE=$(mktemp -u)
    echo "Creating private keystore from ${PEM_CRT_PATH} and ${PEM_KEY_PATH} into PKCS12 file: ${PFX_PATH}"
    openssl pkcs12 -export -in ${PEM_CRT_PATH} -inkey ${PEM_KEY_PATH} -out ${INTERMEDIATE} -passout pass:${PFX_PASSWORD}
    keytool -importkeystore -srcstoretype PKCS12  -deststoretype PKCS12 -noprompt \
            -srckeystore   ${INTERMEDIATE} -destkeystore ${PFX_PATH} \
            -deststorepass ${PFX_PASSWORD} -srcstorepass ${PFX_PASSWORD}

    chmod a+r ${PFX_PATH}

}

if [ -n "$PEM2PFX_TRUST_PEM_PATH" ]
then
  TRUST_TMP_DIR=$(mktemp -d)
  cat ${PEM2PFX_TRUST_PEM_PATH} | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/{ if(/BEGIN CERTIFICATE/){a++}; out="'${TRUST_TMP_DIR}'/cert"a".pem"; print >out}'
  for crt in ${TRUST_TMP_DIR}/*.pem
  do
    alias=$(openssl x509 -in $crt -noout -subject -enddate| tr -d '\n'| tr '[:upper:]' '[:lower:]' | \
            sed -r -e 's, = ,=,g' -e 's,subject=,,' -e 's,notafter=([a-z]+ [0-9]+) [0-9:]{8} ([0-9]{4}).*$, -> \1 \2,')
    echo -n "Trust for $alias: "
    keytool -import -alias "${alias}" -noprompt -file ${crt} -keystore ${PEM2PFX_TRUST_PFX_PATH} -storetype PKCS12 -storepass ${PEM2PFX_TRUST_PFX_PASSWORD}
  done
  chmod a+r ${PEM2PFX_TRUST_PFX_PATH}

fi

if [ -n "${PEM2PFX_PRODUCER_PEM_PATH}" ]
then
  mk_keystore_from_pem ${PEM2PFX_PRODUCER_PEM_PATH} ${PEM2PFX_PRODUCER_KEY_PATH} ${PEM2PFX_PRODUCER_PFX_PATH} ${PEM2PFX_PRODUCER_PFX_PASSWORD}
fi
if [ -n "${PEM2PFX_CONSUMER_PEM_PATH}" ]
then
  mk_keystore_from_pem ${PEM2PFX_CONSUMER_PEM_PATH} ${PEM2PFX_CONSUMER_KEY_PATH} ${PEM2PFX_CONSUMER_PFX_PATH} ${PEM2PFX_CONSUMER_PFX_PASSWORD}
fi