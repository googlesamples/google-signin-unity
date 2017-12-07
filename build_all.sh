#!/bin/bash
readonly THIS_DIR="$(cd "$(dirname "$0")"; pwd)"
pushd ${THIS_DIR}
./gradlew -PlintAbortOnError="true" build_all
popd