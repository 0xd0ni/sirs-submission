#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo /sbin/iptables -F
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A OUTPUT -o lo -j ACCEPT
sudo iptables -A INPUT -s  192.168.56.11 -j ACCEPT
sudo ifconfig eth0 192.168.56.11/24 up