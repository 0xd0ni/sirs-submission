#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive
sudo apt-get install gnupg
wget -qO - https://www.mongodb.org/static/pgp/server-4.2.asc | sudo apt-key add -
echo "deb http://repo.mongodb.org/apt/debian buster/mongodb-org/4.2 main" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.2.list
sudo apt-get update -y
sudo apt-get install -y mongodb-org
sudo apt-get -y install maven
sudo apt-get -y install openssl
sudo apt-get -y install default-jdk
#setxkbmap -model abnt2 -layout pt
#sudo ifconfig eth0 192.168.56.11/24 up
