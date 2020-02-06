openssl req \
    -newkey rsa:2048 -nodes -keyout domain.key \
    -subj "/C=SE/ST=Västra Götalands län/L=Göteborg/O=SKLTP/OU=tp/CN=SKLTP-TEST" \
    -x509 -days 7200 -out cert_ou_is_tp.pem

openssl x509 -text -noout -in cert_ou_is_tp.pem
