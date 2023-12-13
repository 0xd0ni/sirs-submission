#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo ifconfig eth0 192.168.56.10/24 up
sudo ifconfig eth0 192.168.56.12/24 up
