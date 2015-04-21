(ns puppetlabs.config.typesafe
  (:import (java.util Map List)
           (com.typesafe.config ConfigFactory ConfigParseOptions ConfigSyntax
                                Config))
  (:require [clojure.java.io :as io]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Private

(declare java-config->clj)

(defn strip-quotes
  "Given a string read from a config file, check to see if it begins and ends with
  double-quotes, and if so, remove them."
  [s]
  {:pre [(string? s)]
   :post [(string? %)
          (not (and (.startsWith % "\"")
                    (.endsWith % "\"")))]}
  (if (and (.startsWith s "\"")
           (.endsWith s "\""))
    (.substring s 1 (dec (.length s)))
    s))

(defn- parse-int
  "Parse a string `s` as an integer, returning nil if the string doesn't
  contain an integer."
  [s]
  {:pre  [(string? s)]
   :post [(or (integer? %) (nil? %))]}
  (try (Integer/parseInt s)
    (catch java.lang.NumberFormatException e
      nil)))

(defn string->val
  "Given a string read from a config file, convert it to the corresponding value
  that we will use for our internal configuration data.  This includes removing
  surrounding double-quotes and casting to an integer when possible."
  [s]
  {:pre [(string? s)]
   :post [((some-fn string? integer?) %)]}
  (let [v (strip-quotes s)]
    (or (parse-int v) v)))

(defn nested-java-map->map
  "Given a (potentially nested) java Map object read from a config file,
  convert it (potentially recursively) to a clojure map with keywordized keys."
  [m]
  {:pre [(instance? Map m)]
   :post [(map? %)
          (every? keyword? (keys %))]}
  (reduce
    (fn [acc [k v]]
      (assoc acc (keyword k)
                 (java-config->clj v)))
    {}
    (.entrySet m)))

(defn java-list->vec
  "Given a java List object read from a config file, convert it to a clojure
  vector for use in our internal configuration representation."
  [l]
  {:pre [(instance? List l)]
   :post [(vector? %)]}
  (mapv java-config->clj l))

(defn java-config->clj
  "Given a java configuration object read from a config file, convert it to a
  clojure object suitable for use in our internal configuration representation."
  [v]
  (cond
    (instance? Map v)   (nested-java-map->map v)
    (instance? List v)  (java-list->vec v)
    (string? v)         (string->val v)
    ;; Any other types we need to treat specially here?
    :else v))

(defn config->map
  "Converts an instance of `Config` to a more user-friendly clojure map"
  [config]
  {:pre [(instance? Config config)]
   :post [(map? %)]}
  (-> config
      .root
      .unwrapped
      nested-java-map->map))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn config-file->map
  "Given the path to a configuration file (of type .conf, .json, or .properties),
  parse the file and return a clojure map representation of the data."
  [file-path]
  {:pre [(string? file-path)]
   :post [(map? %)]}
  (-> (io/file file-path)
      (ConfigFactory/parseFileAnySyntax)
      config->map))

(defn reader->map
  "Given any clojure object that is suitable for use with `reader`, parse the
  configuration data and return a clojure map representation of the data.

  Optional `format` arg may be one of `:conf`, `:json`, or `:properties`, to
  specify the configuration format.  Defaults to `:conf`."
  ([input]
   (reader->map input :conf))
  ([input format]
   {:pre [(satisfies? io/IOFactory input)
          (contains? #{:conf :json :properties} format)]
    :post [(map? %)]}
   (let [parse-options (ConfigParseOptions/defaults)]
     (.setSyntax
       parse-options
       (condp = format
         :conf ConfigSyntax/CONF
         :json ConfigSyntax/JSON
         :properties ConfigSyntax/PROPERTIES))
     (-> (io/reader input)
         (ConfigFactory/parseReader parse-options)
         config->map))))

