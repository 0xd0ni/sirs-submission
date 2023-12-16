#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo /sbin/iptables -F
sudo iptables -A INPUT -s  192.168.56.10 -j ACCEPT
sudo iptables -A INPUT -s  192.168.56.12 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT
iptables -A INPUT -j ACCEPT
iptables -A OUTPUT -j ACCEPT
sudo ifconfig eth0 192.168.56.10/24 up
sudo ifconfig eth0 192.168.56.12/24 up
