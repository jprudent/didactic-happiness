(ns tetsuo.client
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defn posology-descriptors [vmp-id body handler]
  (go (handler (<! (http/post
                     (str "http://lo:8088/rest/api/vmp/" vmp-id "/posology-descriptors")
                     {:content-type "text/xml"
                      :body         body})))))