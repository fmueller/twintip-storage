(ns org.zalando.stups.twintip.crawler.jobs
  (:require [org.zalando.stups.friboo.system.cron :refer [def-cron-component]]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.friboo.config :refer [require-config]]
            [overtone.at-at :refer [every]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [org.zalando.stups.friboo.system.oauth2 :as oauth2]))

(def default-configuration
  {:jobs-cpu-count        1
   :jobs-every-ms         60000
   :jobs-initial-delay-ms 1000})

(defn- conpath
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

(defn- get-app-api-info
  "Fetch information about one application."
  [app-service-url]
  (try
    ; TODO make discovery endpoint configurable
    (let [discovery (:body (client/get (conpath app-service-url "/.well-known/schema-discovery")
                                       {:as :json}))
          schema-url (:schema_url discovery)
          schema-url (if (.startsWith schema-url "/")
                       (conpath app-service-url schema-url)
                       schema-url)
          schema-type (:schema_type discovery)
          ui-url (:ui_url discovery)]
      (try
        (let [definition (:body (client/get schema-url
                                            {:as :json-string-keys}))
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
  [configuration tokens]
  (try
    (let [kio-url (require-config configuration :kio-url)
          twintip-storage-url (require-config configuration :twintip-storage-url)]
      (log/debug "Starting new crawl run with %s..." kio-url)

      (let [apps (:body (client/get (conpath kio-url "/apps")
                                    {:oauth-token (oauth2/access-token :kio-ro-api tokens)
                                     :as          :json}))]
        (log/info "Found %s apps in Kio; fetching their APIs..." (count apps))
        (log/debug "Fetching APIs of the following apps: %s" apps)

        (doseq [{:keys [id service_url] :as app} apps]
          (log/debug "Fetching update for %s from %s..." id service_url)
          (let [api-info (get-app-api-info service_url)]
            (log/debug "Storing result for %s: %s" id api-info)
            (try
              (client/put (conpath twintip-storage-url "/apps/" id)
                          {:oauth-token  (oauth2/access-token :twintip-rw-api tokens)
                           :content-type :application/json
                           :form-params  api-info})
              (log/info "Updated %s." id)
              (catch Exception e
                (log/error e "Could not store result for %s in %s: %s" id twintip-storage-url (str e))))))))
    (catch Throwable e
      (log/error e "Could not fetch apps %s." (str e)))))

(def-cron-component
  Jobs [tokens]

  (let [{:keys [every-ms initial-delay-ms]} configuration]

    (every every-ms #(crawl configuration tokens) pool :initial-delay initial-delay-ms :desc "API crawling")))
