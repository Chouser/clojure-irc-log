(ns irc-log
    (:require (clojure.contrib [duck-streams :as ds]))
    (:use (clojure.contrib [mmap :only (mmap)]
                           [str-utils :only (re-partition)]))
    (:import (java.util Date)
             (java.text SimpleDateFormat)
             (java.nio  ByteBuffer)
             (java.io   File)))

;(set! *warn-on-reflection* true)

(def #^SimpleDateFormat date-in-fmt  (SimpleDateFormat. "MMM dd yyyy"))
(def #^SimpleDateFormat date-file-fmt (SimpleDateFormat. "yyyy-MM-dd"))
(def channel "#clojure")

(defn xhtml [v]
  (let [astr (fn [m] (apply str (mapcat #(list \ (name (key %))
                                               \= \" (val %) \") m)))]
    (cond (and (vector? v) (keyword? (first v)))
            (let [[i1 & v2] v
                  [i2 & v3] v2
                  tag+attrs (str \< (name i1) (when (map? i2) (astr i2)))
                  content   (if (map? i2) v3 v2)]
              (cond (seq content)  (str tag+attrs ">" (xhtml [content])
                                        "</" (name i1) \>)
                    (= :script i1) (str tag+attrs "></script>")
                    :else          (str tag+attrs " />")))
          (or (vector? v) (seq? v))
            (apply str (map xhtml v))
          :else v)))

(defn charseq
  ([#^ByteBuffer buf] (charseq buf 0 (.limit buf)))
  ([#^ByteBuffer buf start end]
      (proxy [CharSequence] []
          (charAt [i]        (char (.get buf #^Integer (+ start i))))
          (length []         (- end start))
          (subSequence [s e] (charseq buf (+ start s) (+ start e)))
          (toString []       (let [len (- end start)
                                   ary #^"[B" (make-array Byte/TYPE len)]
                                (.position buf start)
                                (.get buf ary)
                                (String. ary "ISO-8859-1"))))))

(defmacro hash-syms [& syms]
  (cons 'hash-map (mapcat #(list (keyword (name %)) %) syms)))

(def escape-map {\& "&amp;",  \< "&lt;", \> "&gt;",
                 \" "&quot;", \newline "<br />"})
(def link-re #"(?:https?://|www\.)(?:<[^>]*>|[^<>\s])*(?=(?:&gt;|&lt;|[.()\[\]])*(?:\s|$))")
(def wrap-re #"(?:<[^>]*>|&[^;]*;|[^/&?]){1,50}[/&?]*")

(defn text-to-html [text]
  (let [escaped (apply str (map #(or (escape-map %) %) text))
        linked  (apply str
                       (for [[text url]
                             (partition 2 (lazy-cat
                                            (re-partition link-re escaped)
                                            [nil]))]
                         (str text
                              (when url
                                (let [urltext (reduce #(str %1 "<wbr />" %2)
                                                      (re-seq wrap-re url))]
                                  (xhtml [:a {:href url :class "nm"}
                                          urltext]))))))]
    (str linked "\n")))

(defn html-header [date]
  (let [datestr (.format date-in-fmt date)]
    (str "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
         "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
         "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
         (xhtml
           [:head [:title channel " - " datestr]
                  [:meta {:http-equiv "Content-Type"
                          :content "application/xhtml+xml; charset=UTF-8"}]
                  [:link   {:type "text/css" :href "/irc.css"
                            :rel "stylesheet"}]
                  [:script {:type "text/javascript" :src "/irc.js"}]])
         "<body>"
         (xhtml [:h1 channel " - " datestr])
         "<div id=\"narrow\">"
          (xhtml [
            [:dl
             [:dt
              [:form {:action "http://www.google.com/cse" :id "cse-search-box"}
               [:div
                [:input {:type "hidden" :name "cx"
                         :value "partner-pub-1237864095616304:e7qm3gycp2b"}]
                [:input {:type "hidden" :name "ie" :value "UTF-8"}]
                [:input {:type "text"   :id "q" :name "q"  :size "10"}]
                [:input {:type "submit" :id "sa" :name "sa" :value "Go"}]]]
              [:script {:type "text/javascript"
                        :src "http://www.google.com/coop/cse/brand?form=cse-search-box&amp;lang=en"}]]
             [:dd [:a {:href "http://clojure.org/"} "Main Clojure site"]]
             [:dd [:a {:href "http://groups.google.com/group/clojure"}
                   "Google Group"]]
             [:dd [:a {:href "irc://irc.freenode.net/clojure"} "IRC"]]
             [:dd [:a {:href "http://en.wikibooks.org/wiki/Clojure_Programming"}
                   "Wiki"]]
             [:dd [:a {:href "/date/"} "List of all logged dates"]]]
            [:div {:id "nav-head" :class "nav"}
                  [:noscript "Turn on JavaScript for date navigation."]
                  "&nbsp;"]])
          "<div id=\"main\">")))

(defn html-footer [date]
  (str "</div>"
       (xhtml [
         [:div {:id "nav-foot" :class "nav"} "&nbsp;"]
         [:div {:class "foot"} "Logging service provided by "
               [:a {:class "nm" :href "http://n01se.net/"} "n01se.net"]]])
       "</div></body></html>\n"))

(defn minutes [timestr]
  (Integer/parseInt (second (re-seq #"\d+" timestr))))

(defn html-post [prevpost {:keys [timestr speak emote text imc]}]
  (let [htmltext   (text-to-html text)
        prevminute (if-let ptime (:timestr prevpost) (minutes ptime) 99)]
    (xhtml
      [:p
        [:a (merge {:name (str timestr (when (< 0 imc) (char (+ imc 96))))}
              (if (<= 0 (- (minutes timestr) prevminute) 5) nil {:class "nh"}))
            (second (re-find #"^0?(.*)" timestr))]
        " "
        (when (or emote (not= speak (:speak prevpost)))
          [:b (if emote "*" (str speak ":")) " "])
        (if speak htmltext [[:em emote] " " htmltext])])))

(defn parse-post [prevs line]
  (let [[_ timestr c body] (re-matches #"(..:..) (#\S+): (.*)" line)]
    (if (= c channel)
      (conj prevs
            (let [[_ speak emote text]
                    (re-matches #"(?:< (\S+)> | \* (\S+))(.*)" body)
                  imc (let [p (peek prevs)]
                        (if (= timestr (:timestr p)) (+ 1 (:imc p)) 0))
                  offset (count prevs)]
              ;(println line)
              ;(prn (hash-syms timestr speak emote text offset imc))
              (hash-syms timestr speak emote text offset imc)))
      prevs)))

(defn split-days [cs]
  (let [#^SimpleDateFormat date-in-fmt (SimpleDateFormat. "MMM dd yyyy")]
    (for [[[_ datestr] body]
            (partition 2 (rest (re-partition #"--- Day changed ... (.*)" cs)))]
      [(.parse date-in-fmt datestr) (re-seq #".+" (str body))])))

(defn skip-until [#^Date lastdate days]
  (if lastdate
    (filter (fn [[date lines]] (not (.before date lastdate))) days)
    days))

(defn write-day [date lines]
  (let [datestr (.format date-file-fmt date)
        filename (str "date/" datestr ".html")
        goodposts (reduce parse-post [] lines)]
    (.mkdir (File. "date"))
    (with-open #^java.io.PrintWriter out (ds/writer filename)
      (println "Writing" filename)
      (.write out #^String (html-header date))
      (doseq string (map html-post (cons nil goodposts) goodposts)
        (.write out #^String string))
      (.write out #^String (html-footer date)))
    filename))


(let [lastdate
       (let [[_ datestr] (some #(re-find #"(....-..-..)\.html" %)
                               (-> (map str (.listFiles (File. "date")))
                                   sort reverse))]
         (when datestr (.parse date-file-fmt datestr)))
      days
       (skip-until lastdate (split-days (charseq (mmap "irc-01.log"))))
      lastfile
       (reduce (fn [prev [date lines]] (write-day date lines)) nil days)]
  (.. Runtime getRuntime (exec (str "ln -sf " lastfile " index.html"))))
