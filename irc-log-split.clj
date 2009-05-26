(ns irc-log-split
    (:require (clojure.contrib [duck-streams :as ds]))
    (:use (clojure.contrib [mmap :only (mmap)]
                           [str-utils :only (re-partition)]))
    (:import (java.util Date)
             (java.text SimpleDateFormat)
             (java.nio  ByteBuffer)
             (java.io   FileReader FileWriter BufferedReader)))

; This is safe only as long as the user consumes the whole seq
(defn file-line-seq [filename]
  (let [reader (BufferedReader. (FileReader. filename))]
    (concat (line-seq reader)
            (lazy-seq (.close reader)
                      nil))))

(defn split-days []
  (let [#^SimpleDateFormat fmt1 (SimpleDateFormat. "MMM dd hh:mm:ss yyyy")
        #^SimpleDateFormat fmt2 (SimpleDateFormat. "MMM dd yyyy")
        #^SimpleDateFormat file-fmt (SimpleDateFormat. "yyyy-MM-dd")
        lines (apply concat (map file-line-seq
                                 ["/home/chouser/commlog/irssi/irc-01.log"
                                  "/home/chouser/commlog/irssi/irc-02.log"]))]
    (loop [[line & lines] lines, #^FileWriter writer nil, date nil]
      (if-not line
        (.close writer)
        (let [[_ date-str] (re-find #"^--- (?:Day changed|Log opened) ... (.*)"
                                    line)
              new-date (when date-str
                         (.format file-fmt (.parse (if (re-find #":" date-str)
                                                     fmt1 fmt2)
                                                   date-str)))]
          (if (and new-date (not= new-date date))
            (do ; rotate logs
              (when writer
                (.close writer))
              (let [writer (FileWriter. (str "logs/" new-date ".log"))]
                (.write writer line)
                (.write writer "\n")
                (recur lines writer new-date)))
            (do ; append to existing log, if any
              (when writer
                (.write writer line)
                (.write writer "\n"))
              (recur lines writer date))))))))


