#!/bin/bash
java -server -Dfile.encoding=UTF-8 -cp "build/classes:20.jar:lib/*" nro.models.server.ServerManager
