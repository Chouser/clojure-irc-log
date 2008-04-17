#!/bin/bash
cd /home/chouser/proj/clojure-log/
java -cp ../../build/clojure/clojure.jar clojure.lang.Script irc-log-split.clj
rsync -u *.html *.js *.css *.clj n01se.net:web/chouser/clojure-log/
