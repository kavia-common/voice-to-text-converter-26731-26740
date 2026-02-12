#!/bin/bash
cd /home/kavia/workspace/code-generation/voice-to-text-converter-26731-26740/frontend_react
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

