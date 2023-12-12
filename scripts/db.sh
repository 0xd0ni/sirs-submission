#!/usr/bin/env bash
setxkbmap -model abnt2 -layout pt
sudo ifconfig eth0 192.168.56.11/24 up
