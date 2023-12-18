#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo /sbin/iptables -F
sudo iptables -A INPUT -s  192.168.56.11 -j ACCEPT
sudo iptables -A INPUT -i icmp -j DROP

