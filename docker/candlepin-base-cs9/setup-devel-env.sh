#!/bin/sh
#
# Sets a system up for a candlepin development environment (minus a db,
# handled separately), and an initial clone of candlepin.

set -ve

source /root/dockerlib.sh

export JAVA_VERSION=11
export JAVA_HOME=/usr/lib/jvm/java-$JAVA_VERSION

# Install & configure dev environment
dnf install -y epel-release

PACKAGES=(
    createrepo_c
    expect
    gettext
    git
    hostname
    java-$JAVA_VERSION-openjdk-devel
    jss-5.0.3-1.el9 # last jss package that support Java version 11
    mariadb
    openssl
    pki-servlet-engine
    postgresql
    procps
    python3-pip
    python3-pyyaml
    python3-requests
    python3-libxml2
    python-unversioned-command
    rpm-build
    rpm-sign
    rsyslog
    wget
    which
)

dnf install -y ${PACKAGES[@]}

# Setup for autoconf:
mkdir /etc/candlepin
echo "# AUTOGENERATED" > /etc/candlepin/candlepin.conf

cat > /root/.bashrc <<BASHRC
if [ -f /etc/bashrc ]; then
  . /etc/bashrc
fi

export HOME=/root
export JAVA_HOME=/usr/lib/jvm/java-$JAVA_VERSION
BASHRC

git clone https://github.com/candlepin/candlepin.git /candlepin
cd /candlepin

# Installs all Java deps into the image, big time saver
./gradlew --no-daemon dependencies

# Fix issue with owner on mount volume
git config --global --add safe.directory /candlepin-dev

cd /
rm -rf /candlepin
cleanup_env