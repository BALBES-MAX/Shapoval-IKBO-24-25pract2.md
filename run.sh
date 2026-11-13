#!/usr/bin/env bash
set -e
mkdir -p out
javac -encoding UTF-8 -d out src/ru/mirea/depvis/*.java
java -cp out ru.mirea.depvis.Main "${1:-config.csv}"
