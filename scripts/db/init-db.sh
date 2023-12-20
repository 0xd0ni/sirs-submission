#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo /sbin/iptables -F
sudo iptables -A INPUT -s 192.168.56.11 -p tcp --destination-port 27017 -m state --state NEW,ESTABLISHED -j ACCEPT
sudo iptables -A OUTPUT -d 192.168.56.11 -p tcp --source-port 27017 -m state --state ESTABLISHED -j ACCEPT
sudo iptables -P INPUT DROP
sudo iptables -P OUTPUT DROP

