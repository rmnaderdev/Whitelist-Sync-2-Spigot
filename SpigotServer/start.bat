@echo off
java -Xms1G -Xmx1G -XX:+UseConcMarkSweepGC -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar spigot-1.14.2.jar
pause