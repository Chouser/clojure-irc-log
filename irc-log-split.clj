(set! *warn-on-reflection* true)

(load-file "../clojure-contrib/trunk/duck-streams.clj")

(import '(java.text SimpleDateFormat)
        '(java.io   File))

(def #^SimpleDateFormat date-in-fmt  (new SimpleDateFormat "MMM dd yyyy"))
(def #^SimpleDateFormat date-out-fmt (new SimpleDateFormat "yyyy-MM-dd"))
(def channel "#clojure")

(defn xhtml [v]
  (let [astr (fn [m] (apply str (mapcat #(list \ (name (key %))
                                               \= \" (val %) \") m)))]
    (cond (and (vector? v) (keyword? (first v)))
            (let [[i1 & v2] v
                  [i2 & v3] v2
                  tag+attrs (str \< (name i1) (when (map? i2) (astr i2)))
                  content   (if (map? i2) v3 v2)]
              (cond (seq content) (str tag+attrs ">" (xhtml [content])
                                       "</" (name i1) \>)
                    :else         (str tag+attrs " />")))
          (or (vector? v) (seq? v))
            (apply str (map xhtml v))
          :else v)))

(defn re-split
  "Returns a lazy sequence of pairs, [ss m]. Each ss is a substring as
  split up by the pattern.  Each m is the match (as processed by
  re-groups) that followed ss in the original string.  If the original
  string does not end in match, the final m is nil."
  [#^java.util.regex.Pattern re #^String s]
    (let [m (re-matcher re s)]
      ((fn step [prevend]
           (if (.find m)
             (lazy-cons [(.substring s prevend (.start m)) (re-groups m)]
                        (step (+ (.start m) (count (.group m)))))
             (when (< prevend (count s))
               (list [(.substring s prevend) nil]))))
       0)))

(def escape-map {\& "&amp;",  \< "&lt;", \> "&gt;",
                 \" "&quot;", \newline "<br />"})
(def link-re #"(?:https?://|www\\.)(?:<[^>]*>|[^<>\\s])*(?=(?:&gt;|&lt;|[.\\(\\)\\[\\]])*(?:\\s|$))")

(defn text-to-html [text]
  (let [escaped   (apply str (map #(or (escape-map %) %) text))
        linked    (apply str (for [[text url] (re-split link-re escaped)]
                                  (str text
                                       (when url
                                         (xhtml [:a {:href url} url])))))]
    linked))

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
    "<table><tbody valign=\"top\">")))

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
      (if (line.startsWith "--- Day changed ")
        text
        (do (func line)
            (recur more func))))))

(defn write-days [lastdate [dateln & text]]
  (when dateln
    (recur lastdate
      (let [date-in-str  (second (re-find #"changed ... (.*)" dateln))
            date         (date-in-fmt.parse (or date-in-str "Jan 01 1900"))
            date-out-str (date-out-fmt.format date)]
        (if (and lastdate (date.before lastdate))
          (until-next-day text (fn [_]))
          (with-open out (duck-streams/writer (str date-out-str ".html"))
            (out.write (html-header date))
            (let [more (until-next-day text #(out.write (html-post %)))]
              (out.write (html-footer date))
              more)))))))

(def lastdate
  (let [[_ datestr] (some #(re-find #"(....-..-..)\\.html" %)
                          (reverse
                            (sort (map str (.listFiles (new File "."))))))]
    (when datestr (date-out-fmt.parse datestr))))

(write-days lastdate (line-seq (duck-streams/reader "/home/chouser/irc.log")))
