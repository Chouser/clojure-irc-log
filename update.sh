#!/bin/bash
cd `dirname $0`
mvn --offline --quiet clojure:run
