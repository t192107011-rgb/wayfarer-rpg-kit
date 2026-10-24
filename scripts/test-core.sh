#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p test-out
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d test-out app/src/main/java/io/wayfarer/rpg/core/*.java tests/CoreTests.java tests/ActionTests.java tests/BuildTests.java tests/SoulTests.java tests/RelicTests.java tests/CareerTests.java tests/PactTests.java
java -cp test-out CoreTests
java -cp test-out ActionTests

java -cp test-out BuildTests

java -cp test-out SoulTests

java -cp test-out RelicTests

java -cp test-out CareerTests

java -cp test-out PactTests
