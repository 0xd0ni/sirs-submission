#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y
sudo apt-get -y install maven
sudo apt-get -y install openssl
sudo apt-get -y install default-jdk
#setxkbmap -model abnt2 -layout pt
#sudo ifconfig eth0 192.168.56.10/24 up