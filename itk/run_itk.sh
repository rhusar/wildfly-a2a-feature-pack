#!/bin/bash
# ITK harness for the WildFly A2A feature-pack - thin shim over a2a-itk's shared driver.
# The ITK service runs this repository as the Java SDK's "current" agent: it builds the itk module with
# 'mvn -Pitk -pl itk -am install' and starts it with 'mvn exec:java -Dexec.mainClass=org.a2aproject.sdk.itk.Main'.
set -e
cd "$(dirname "${BASH_SOURCE[0]}")"

ITK_SDK_NAME=java
ITK_SCENARIO_SET=shared
ITK_COPY_PROTO=0
ITK_MOUNT_ITK_DIR=0
ITK_EXTRA_DOCKER_ARGS=(-e ITK_BUILD_TIMEOUT="${ITK_BUILD_TIMEOUT:-1800}")

: "${A2A_ITK_REVISION:?A2A_ITK_REVISION environment variable must be set}"
if [ ! -d a2a-itk ]; then
  git clone https://github.com/a2aproject/a2a-itk.git a2a-itk
  git -C a2a-itk checkout "$A2A_ITK_REVISION"
fi

source a2a-itk/scripts/run_itk_shared.sh
