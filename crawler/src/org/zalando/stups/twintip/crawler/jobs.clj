(ns org.zalando.stups.twintip.crawler.jobs
  (:require [org.zalando.stups.friboo.system.cron :refer [def-cron-component]]
            [org.zalando.stups.friboo.log :as log]
            [overtone.at-at :refer [every]]
            [clj-http.lite.client :as client]
            [clojure.data.json :as json]))

(def default-configuration
  {:cpu-count 1})

(defn fetch-apps [kio-url]
  (json/read-str (:body (client/get (str kio-url "/apps") {:insecure? true}))))

(defn update-app [app]
  nil)

(defn crawl [configuration]
  (let [kio-url (:kio-url configuration)]
    (log/info "Starting new crawl run with %s..." kio-url)
    (try

      (let [apps (fetch-apps kio-url)]
        (doseq [app apps]
          (log/info "Fetching update for %s from %s." app app)
          (update-app app)))
      (catch Exception e
        (log/error e "Could not fetch apps %s." (str e))))))

(def-cron-component
  Jobs []

  (every 5000 (partial crawl configuration) pool :initial-delay 1000 :desc "API crawling"))
