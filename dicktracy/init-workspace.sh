#!/bin/bash

WORKSPACE_DIR="./workspace"

if [ -d ${WORKSPACE_DIR} ]; then
   cd ${WORKSPACE_DIR}
else
   rm -rf ${WORKSPACE_DIR}
   mkdir ${WORKSPACE_DIR}
   cd ${WORKSPACE_DIR}
   git clone git@github.com:softwarevidal/arthur.git --depth 500 --no-single-branch
fi



