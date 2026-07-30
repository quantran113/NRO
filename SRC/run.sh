#!/bin/bash
JAVA_BIN=$(which java 2>/dev/null || find /usr/lib/jvm /usr/bin /usr/local -name java -type f 2>/dev/null | head -n 1)
if [ -z "$JAVA_BIN" ]; then
    JAVA_BIN="java"
fi
$JAVA_BIN -server -Dfile.encoding=UTF-8 -cp "build/classes:20.jar:lib/*" nro.models.server.ServerManager
