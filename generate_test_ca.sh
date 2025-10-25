#!/bin/bash
openssl genrsa -out ca.key 4096
openssl req -new -x509 -key ca.key -out ca.crt -days 365 -subj "/C=RU/ST=Moscow/L=Moscow/O=TestCA/OU=IT/CN=TestCA"
