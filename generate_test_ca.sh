#!/bin/bash

# Генерируем тестовый CA сертификат для подписи других сертификатов

# Создаем приватный ключ CA
openssl genrsa -out ca.key 4096

# Создаем самоподписанный CA сертификат
openssl req -new -x509 -key ca.key -out ca.crt -days 365 -subj "/C=RU/ST=Moscow/L=Moscow/O=TestCA/OU=IT/CN=TestCA"

echo "Создан тестовый CA сертификат:"
echo "- ca.key - приватный ключ CA"
echo "- ca.crt - сертификат CA"
echo ""
echo "Для запуска сервера используйте:"
echo "java -cp build/libs/Task_1J.jar ru.nsu.nocode.Main server 8080 4 ca.key 'CN=TestCA'"
