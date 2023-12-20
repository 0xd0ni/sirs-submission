#!/usr/bin/env bash

export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y
sudo apt-get -y install maven
sudo apt-get -y install openssl
sudo apt-get -y install default-jdk