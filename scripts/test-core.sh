#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p test-out
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d test-out app/src/main/java/io/wayfarer/rpg/core/*.java tests/CoreTests.java
java -cp test-out CoreTests
