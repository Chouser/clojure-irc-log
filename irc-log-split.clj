(ns irc-log-split
    (:require (clojure.contrib [duck-streams :as ds]))
    (:use (clojure.contrib [mmap :only (mmap)]
                           [str-utils :only (re-partition)]))
    (:import (java.util Date)
             (java.text SimpleDateFormat)
             (java.nio  ByteBuffer)
             (java.io   FileReader FileWriter BufferedReader)))

(defn split-days []
  (let [#^SimpleDateFormat fmt1 (SimpleDateFormat. "MMM dd hh:mm:ss yyyy")
        #^SimpleDateFormat fmt2 (SimpleDateFormat. "MMM dd yyyy")
        #^SimpleDateFormat file-fmt (SimpleDateFormat. "yyyy-MM-dd")
        lines (mapcat ds/read-lines
                      ["/home/chouser/commlog/irssi/irc-01.log"
                       "/home/chouser/commlog/irssi/irc-02.log"])]
    (loop [[line & lines] lines, writer nil, date nil, date-line nil]
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
              (recur lines nil new-date line))
            (if (re-matches #"..:.. (-[^-]*:#clojure-|#clojure:).*" line)
              (let [#^FileWriter new-writer
                    (or writer (FileWriter. (str "logs/" date ".log")))]
                (when-not writer
                  (.write new-writer #^String date-line)
                  (.write new-writer "\n"))
                (.write new-writer #^String line)
                (.write new-writer "\n")
                (recur lines new-writer date nil))
              (recur lines writer date date-line))))))))
