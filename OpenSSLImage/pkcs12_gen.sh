#!/usr/bin/env sh

CLIENT_CRT="/opt/certs/pem/cert.pem"
CLIENT_KEY="/opt/certs/pem/key.pem"
TRUST_PEM="/opt/certs/pem/ca.pem"

CLIENT_PFX="/opt/certs/pkcs12/function_cert.pfx"
CLIENT_PWD="password"
TRUST_PFX="/opt/certs/pkcs12/truststore.pfx"
TRUST_PWD="password"

CLIENT_TMP=$(mktemp -u)
TRUST_PEMD=$(mktemp -d)

openssl pkcs12 -export -in ${CLIENT_CRT} -inkey ${CLIENT_KEY} -out ${CLIENT_TMP} -passout pass:${CLIENT_PWD}

keytool -importkeystore -srcstoretype PKCS12  -deststoretype PKCS12 -noprompt \
        -srckeystore   ${CLIENT_TMP} -destkeystore ${CLIENT_PFX} \
        -deststorepass ${CLIENT_PWD} -srcstorepass ${CLIENT_PWD}


cat ${TRUST_PEM} | awk '/BEGIN CERTIFICATE/,/END CERTIFICATE/{ if(/BEGIN CERTIFICATE/){a++}; out="'${TRUST_PEMD}'/cert"a".pem"; print >out}'
for crt in ${TRUST_PEMD}/*.pem
do
  keytool -import -alias ${crt} -noprompt -file ${crt} -keystore ${TRUST_PFX} -storetype PKCS12 -storepass ${TRUST_PWD}
done
