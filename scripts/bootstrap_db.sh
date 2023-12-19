#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y
sudo apt-get -y install maven
sudo apt-get -y install openssl
sudo apt-get -y install default-jdk
curl -O http://downloads.mongodb.org/linux/mongodb-linux-x86_64-3.6.1.tgz
tar -zxvf mongodb-linux-x86_64-3.6.1.tgz
mkdir -p mongodb
cp -R -n mongodb-linux-x86_64-3.6.1/ mongodb
mkdir -p mongodb/data/db
rm -r mongodb-linux-x86_64-3.6.1
rm -rf mongodb-linux-x86_64-3.6.1.tgz
# para correr o mongodb: sudo ./mongodb/bin/mongod --dbpath ./mongodb/data/db
