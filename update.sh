#!/bin/bash
cd /home/chouser/proj/clojure-log/
java -cp ../../build/clojure/clojure.jar:../clojure-contrib/ clojure.lang.Script irc-log-split.clj
rsync -ua * n01se.net:clojure-log.n01se.net/
