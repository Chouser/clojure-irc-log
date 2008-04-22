(set! *warn-on-reflection* true)

(load-file "../clojure-contrib/duck-streams.clj")

(import '(java.text SimpleDateFormat)
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

(defn html-post [line]
  (let [[_ timestr c body] (re-matches #"(..:..) (#\\S+): (.*)" line)]
    (if (= c channel)
      (let [[_ speak emote text] (re-matches #"(?:< (\\S+)> | \\* (\\S+))(.*)"
                                           body)
           htmltext (text-to-html text)]
        (xhtml [:tr
                 [:td {:class "t"} [:a {:name timestr} timestr]]
                 [:td {:class "n"} (or speak "*")]
                 [:td {:class "m"} (if speak
                                     htmltext
                                     [[:span {:class "n"} emote]
                                      " " htmltext])]]))
      "")))

(defn until-next-day [text func]
  (when text
    (let [[#^String line & more] text]
      (if (.startsWith line "--- Day changed ")
        text
        (do (func line)
            (recur more func))))))

(defn write-days [lastdate [dateln & text]]
  (when dateln
    (recur lastdate
      (let [date-in-str  (second (re-find #"changed ... (.*)" dateln))
            date         (.parse date-in-fmt (or date-in-str "Jan 01 1900"))
            date-out-str (.format date-file-fmt date)]
        (if (and lastdate (.before date lastdate))
          (until-next-day text (fn [_]))
          (with-open #^java.io.PrintWriter out
                     (duck-streams/writer (str date-out-str ".html"))
            (.write out #^String (html-header date))
            (let [more (until-next-day text #(.write out #^String (html-post %)))]
              (.write out #^String (html-footer date))
              more)))))))

(def lastdate
  (let [[_ datestr] (some #(re-find #"(....-..-..)\\.html" %)
                          (reverse
                            (sort (map str (.listFiles (new File "."))))))]
    (when datestr (.parse date-file-fmt datestr))))

(time (write-days lastdate (line-seq (duck-streams/reader "irc-01.log"))))
