#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo /sbin/iptables -F
sudo iptables -A INPUT -s  192.168.56.11 -j ACCEPT
sudo iptables -A INPUT -i icmp -j DROP
#openssl genrsa -out db.key  
#openssl req -new -key db.key -out db.csr
#openssl x509 -req -days 365 -in db.csr -signkey db.key -out db.crt
#openssl x509 -in db.crt -out db.pem

