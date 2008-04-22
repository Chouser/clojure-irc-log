(set! *warn-on-reflection* true)

(load-file "../clojure-contrib/duck-streams.clj")

(import '(java.util Date)
        '(java.text SimpleDateFormat)
        '(java.nio  ByteBuffer)
        '(java.io   File))

(def #^SimpleDateFormat date-in-fmt  (new SimpleDateFormat "MMM dd yyyy"))
(def #^SimpleDateFormat date-file-fmt (new SimpleDateFormat "yyyy-MM-dd"))
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

(defn take-ns [n xs]
  (when (seq xs)
    (lazy-cons (take n xs) (take-ns n (drop n xs)))))

(defn re-split
  [#^java.util.regex.Pattern re #^CharSequence cs]
    (let [m (re-matcher re cs)]
      ((fn step [prevend]
           (if (.find m)
             (lazy-cons (.subSequence cs prevend (.start m))
                        (lazy-cons (re-groups m)
                                   (step (+ (.start m) (count (.group m))))))
             (when (< prevend (.length cs))
               (list (.subSequence cs prevend (.length cs))))))
       0)))

(defn charseq
  ([#^ByteBuffer buf] (charseq buf 0 (.limit buf)))
  ([#^ByteBuffer buf start end]
      (proxy [CharSequence] []
          (charAt [i]        (char (.get buf #^Integer (+ start i))))
          (length []         (- end start))
          (subSequence [s e] (charseq buf (+ start s) (+ start e)))
          (toString []       (let [len (- end start)
                                   ary #^"[B" (make-array (Byte.TYPE) len)] ;"
                                (.position buf start)
                                (.get buf ary)
                                (new String ary "ISO-8859-1"))))))

(defn mmap [f]
  (let [READ_ONLY (java.nio.channels.FileChannel$MapMode.READ_ONLY)
        channel (.getChannel (new java.io.FileInputStream f))]
    (.map channel READ_ONLY 0 (.size channel))))

(defmacro hash-syms [& syms]
  (cons 'hash-map (mapcat #(list (keyword (name %)) %) syms)))

(def escape-map {\& "&amp;",  \< "&lt;", \> "&gt;",
                 \" "&quot;", \newline "<br />"})
(def link-re #"(?:https?://|www\\.)(?:<[^>]*>|[^<>\\s])*(?=(?:&gt;|&lt;|[.\\(\\)\\[\\]])*(?:\\s|$))")

(defn text-to-html [text]
  (let [escaped (apply str (map #(or (escape-map %) %) text))
        linked  (apply str
                       (for [[text url] (take-ns 2 (re-split link-re escaped))]
                            (str text
                                 (when url
                                   (xhtml [:a {:href url} url])))))]
    (str linked "\n")))

(defn html-header [date]
  (let [datestr (date-in-fmt.format date)]
  (str "<html>"
    (xhtml
      [:head [:title channel " - " datestr]
             [:link   {:type "text/css" :href "irc.css" :rel "stylesheet"}]
             [:script {:type "text/javascript" :src "irc.js"}]])
    "<body>"
    (xhtml [[:h1 channel " - " datestr]
            [:div {:id "nav-head"} "&nbsp;"]])
    "<table><tbody valign=\"top\">\n")))

(defn html-footer [date]
  (str "</tbody></table>"
       (xhtml [:div {:id "nav-foot"} "&nbsp;"])
       "</body></html>\n"))

(defn html-post [prevpost post]
  (let [{timestr :timestr speak :speak emote :emote text :text} post
        htmltext (text-to-html text)]
    (xhtml [:tr
              [:td {:class "t"} [:a {:name timestr} timestr]]
              [:td {:class "n"} (or speak "*")]
              [:td {:class "m"} (if speak
                                  htmltext
                                  [[:span {:class "n"} emote]
                                  " " htmltext])]])))

(defn parse-post [line]
  (let [[_ timestr c body] (re-matches #"(..:..) (#\\S+): (.*)" line)]
    (when (= c channel)
      (let [[_ speak emote text]
              (re-matches #"(?:< (\\S+)> | \\* (\\S+))(.*)" body)]
        (hash-syms timestr speak emote text)))))

(defn split-days [cs]
  (let [#^SimpleDateFormat date-in-fmt (new SimpleDateFormat "MMM dd yyyy")]
    (for [[[_ datestr] body]
            (take-ns 2 (rest (re-split #"--- Day changed ... (.*)" cs)))]
      [(.parse date-in-fmt datestr) (re-seq #".+" body)])))

(defn skip-until [#^Date lastdate days]
  (if lastdate
    (filter (fn [[date lines]] (when-not (.before lastdate date))) days)
    days))

(defn write-days [days]
  (doseq [date lines] days
    (let [datestr (.format date-file-fmt date)
          filename (str datestr ".html")]
      (with-open #^java.io.PrintWriter out (duck-streams/writer filename)
        (.write out #^String (html-header date))
        (let [goodposts (filter identity (map parse-post lines))]
          (doseq string (map html-post (cons nil goodposts) goodposts)
            (.write out #^String string)))
        (.write out #^String (html-footer date))))))

(def lastdate
  (let [[_ datestr] (some #(re-find #"(....-..-..)\\.html" %)
                          (reverse
                            (sort (map str (.listFiles (new File "."))))))]
    (when datestr (.parse date-file-fmt datestr))))

(time
  (write-days
    (skip-until lastdate (split-days (charseq (mmap "irc-01.log"))))))

