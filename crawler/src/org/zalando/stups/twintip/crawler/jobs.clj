(ns org.zalando.stups.twintip.crawler.jobs
  (:require [org.zalando.stups.friboo.system.cron :refer [def-cron-component]]
            [org.zalando.stups.friboo.log :as log]
            [overtone.at-at :refer [every]]
            [clj-http.lite.client :as client]
            [clojure.data.json :as json]))

(def default-configuration
  {:cpu-count        1
   :every-ms         5000
   :initial-delay-ms 1000})

(defn- add-path
  "Concatenates path elements to an URL."
  [url & path]
  (let [[x & xs] path]
    (if x
      (let [url (if (or
                      (.endsWith url "/")
                      (.startsWith x "/"))
                  (str url x)
                  (str url "/" x))]
        (recur url xs))
      url)))

(defn- fetch-url
  "GETs a JSON document and returns the parsed result."
  ([url]
   (-> (client/get url)
       :body
       json/read-str))
  ([url path]
   (fetch-url (add-path url path))))

(defn fetch-apps
  "Fetches list of all applications."
  [kio-url]
  ; TODO filter only active
  (fetch-url kio-url "/apps"))

(defn- get-app-api-info
  "Fetch information about one application."
  [app-id app-service-url]
  (try
    ; TODO make discovery endpoint configurable
    (let [discovery (fetch-url app-service-url "/.discovery")]
      (try
        (let [definition (fetch-url (:url discovery))]
          (if (= (:swagger definition) "2.0")
            {:status     "SUCCESS"
             :type       "swagger-2.0"
             :name       (-> definition :info :title)
             :version    (-> definition :info :version)
             :url        (:url discovery)
             :ui         (:ui discovery)
             :definition definition}

            ; incompatible definition
            {:status     "INCOMPATIBLE"
             :type       nil
             :name       nil
             :version    nil
             :url        (:url discovery)
             :ui         (:ui discovery)
             :definition nil}))

        (catch Exception e
          ; cannot fetch definition of discovery document
          {:status     "UNAVAILABLE"
           :type       nil
           :name       nil
           :version    nil
           :url        (:url discovery)
           :ui         (:ui discovery)
           :definition nil})))

    (catch Exception e
      ; cannot even load discovery document
      {; TODO make explicit status code for "NOT_REACHABLE" (no connection) and "NO_DISCOVERY" (404)
       :status     (.getName e)
       :type       nil
       :name       nil
       :version    nil
       :url        nil
       :ui         nil
       :definition nil})))

(defn- crawl
  "One run to get API definitions of all applications."
  [configuration]
  (let [{:keys [kio-url storage-url]} (:kio-url configuration)]
    (log/info "Starting new crawl run with %s..." kio-url)
    (try
      (let [apps (fetch-apps kio-url)]
        (doseq [{:keys [id service_url]} apps]
          (log/info "Fetching update for %s from %s." id service_url)
          (let [api-info (get-app-api-info id service_url)]
            (log/debug "Storing result for %s..." id)
            (try
              (client/put (add-path storage-url "/apis/" id) :body api-info)
              (log/info "Updated %s." id)
              (catch Exception e
                (log/warn "Could not store result for %s in %s." id storage-url)))))
        (catch Exception e
          (log/error e "Could not fetch apps %s." (str e)))))))

(def-cron-component
  Jobs []

  (let [{:keys [every-ms initial-delay-ms]} configuration]

    (every every-ms (partial crawl configuration) pool :initial-delay initial-delay-ms :desc "API crawling")))
