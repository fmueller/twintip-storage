; Copyright 2015 Zalando SE
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns org.zalando.stups.twintip.storage.core
  (:require [com.stuartsierra.component :refer [using system-map]]
            [org.zalando.stups.friboo.config :as config]
            [org.zalando.stups.friboo.system :as system]
            [org.zalando.stups.friboo.log :as log]
            [org.zalando.stups.twintip.storage.sql :as sql]
            [org.zalando.stups.twintip.storage.api :as api])
  (:gen-class))

(defn run
  "Initializes and starts the whole system."
  [default-configuration]
  (let [configuration (config/load-configuration
                        (system/default-http-namespaces-and :db)
                        [sql/default-db-configuration
                         api/default-http-configuration
                         default-configuration])

        system (system/http-system-map configuration
                                       api/map->API [:db]
                                       :db (sql/map->DB {:configuration (:db configuration)}))]

    (system/run configuration system)))

(defn -main
  "The actual main for our uberjar."
  [& args]
  (try
    (run {})
    (catch Exception e
      (log/error e "Could not start system because of %s." (str e))
      (System/exit 1))))
