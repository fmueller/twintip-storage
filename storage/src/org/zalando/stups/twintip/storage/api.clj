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

(ns org.zalando.stups.twintip.storage.api
  (:require [org.zalando.stups.friboo.system.http :refer [def-http-component]]
            [org.zalando.stups.twintip.storage.sql :as sql]
            [ring.util.response :refer :all]
            [org.zalando.stups.friboo.ring :refer :all]
            [org.zalando.stups.friboo.log :as log]))

; define the API component and its dependencies
(def-http-component API "api/twintip-api.yaml" [db])

(def default-http-configuration
  {:http-port 8080})

;; applications

(defn read-apis [{:keys [search]} _ db]
  (if (nil? search)
    (do
      (log/debug "Read all APIs.")
      (-> (sql/read-apis {} {:connection db})
          (response)
          (content-type-json)))
    (do
      (log/debug "Search in APIs with query: %s" search)
      (-> (sql/search-apis {:searchquery search} {:connection db})
          (response)
          (content-type-json)))))

(defn read-api [{:keys [application_id]} _ db]
  (log/debug "Read API %s." application_id)
  (-> (sql/read-api
        {:application_id application_id}
        {:connection db})
      (single-response)
      (content-type-json)))

(defn create-or-update-api! [{:keys [apidef application_id]} _ db]
  (sql/create-or-update-api!
    (merge apidef {:application_id application_id})
    {:connection db})
  (log/audit "Created/updated API %s using data %s." application_id apidef)
  (response nil))
