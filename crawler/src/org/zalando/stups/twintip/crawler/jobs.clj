(ns org.zalando.stups.twintip.crawler.jobs
  (:require [org.zalando.stups.friboo.system.cron :refer [def-cron-component]]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.config :refer [require-config]]
            [overtone.at-at :refer [every]]
            [clj-http.lite.client :as client]
            [clojure.data.json :as json]))

(def default-configuration
  {:jobs-cpu-count        1
   :jobs-every-ms         60000
   :jobs-initial-delay-ms 1000})

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
  [app-service-url]
  (try
    ; TODO make discovery endpoint configurable
    (let [discovery (fetch-url app-service-url "/.well-known/schema-discovery")
          schema-url (get discovery "schema_url")
          schema-type (get discovery "schema_type")
          ui-url (get discovery "ui_url")]
      (try
        (let [definition (fetch-url (add-path app-service-url schema-url))
              swagger-2-0? (and (= schema-type "swagger-2.0")
                                (= (get definition "swagger") "2.0"))
              name (when swagger-2-0? (get-in definition ["info" "title"]))
              version (when swagger-2-0? (get-in definition ["info" "version"]))]
          {:status     "SUCCESS"
           :type       schema-type
           :name       name
           :version    version
           :url        schema-url
           :ui         ui-url
           :definition (json/write-str definition)})

        (catch Exception e
          (log/debug "Definition unavailable %s: %s" schema-url (str e))
          ; cannot fetch definition of discovery document
          {:status     (str "UNAVAILABLE")
           :type       nil
           :name       nil
           :version    nil
           :url        (get discovery "definition")
           :ui         (get discovery "ui")
           :definition nil})))

    (catch Exception e
      (log/warn "Undiscoverable service %s: %s" app-service-url (str e))
      ; cannot even load discovery document
      {; TODO make explicit status code for "NOT_REACHABLE" (no connection) and "NO_DISCOVERY" (404)
       :status     (str "UNDISCOVERABLE")
       :type       nil
       :name       nil
       :version    nil
       :url        nil
       :ui         nil
       :definition nil})))

(defn- crawl
  "One run to get API definitions of all applications."
  [configuration]
  (try
    (let [kio-url (require-config configuration :kio-url)
          storage-url (require-config configuration :storage-url)]
      (log/info "Starting new crawl run with %s..." kio-url)

      (doseq [app (fetch-apps kio-url)]
        (let [app-id (get app "id")
              app-service-url (get app "service_url")]
          (log/debug "Fetching update for %s from %s..." app-id app-service-url)
          (let [api-info (get-app-api-info app-service-url)]
            (log/debug "Storing result for %s: %s" app-id api-info)
            (try
              (client/put (add-path storage-url "/apps/" app-id)
                          {:content-type :json
                           :body         (json/write-str api-info)})
              (log/info "Updated %s." app-id)
              (catch Exception e
                (log/error e "Could not store result for %s in %s: %s" app-id storage-url (str e))))))))
    (catch Exception e
      (log/error e "Could not fetch apps %s." (str e)))))

(def-cron-component
  Jobs []

  (let [{:keys [every-ms initial-delay-ms]} configuration]

    (every every-ms (partial crawl configuration) pool :initial-delay initial-delay-ms :desc "API crawling")))
