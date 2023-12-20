#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y
sudo apt-get -y install maven
sudo apt-get -y install openssl
sudo apt-get -y install default-jdk
wget http://archive.ubuntu.com/ubuntu/pool/main/o/openssl/libssl1.1_1.1.1f-1ubuntu2_amd64.deb
sudo dpkg -i libssl1.1_1.1.1f-1ubuntu2_amd64.deb
rm -rf libssl1.1_1.1.1f-1ubuntu2_amd64.deb
sudo curl -O https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-ubuntu1804-4.2.8.tgz
tar -xvzf mongodb-linux-x86_64-ubuntu1804-4.2.8.tgz
mkdir -p mongodb
sudo cp -R -n mongodb-linux-x86_64-ubuntu1804-4.2.8/ mongodb
mkdir -p mongodb/data/db
rm -r mongodb-linux-x86_64-ubuntu1804-4.2.8
rm -rf mongodb-linux-x86_64-ubuntu1804-4.2.8.tgz
cp project/scripts/db/mongod.conf mongodb/mongodb-linux-x86_64-ubuntu1804-4.2.8/bin/
# para correr o mongodb: sudo ./mongodb/bin/mongod --dbpath ./mongodb/data/db
