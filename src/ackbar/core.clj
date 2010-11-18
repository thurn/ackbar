(ns ackbar.core
  (:use compojure.core hiccup.core hiccup.form-helpers hiccup.page-helpers)
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clojure.contrib.str-utils2 :as s])
  (:import com.google.appengine.api.datastore.EntityNotFoundException))

(ds/defentity Page [^:key name, body])

(defn canonical-name
  "Converts a given page name to a standard format, suitable for use in a URL."
  [str]
  (s/replace (s/replace (.toLowerCase str) #"\s+" "_") #"\W+" ""))


(defn wrap-result [result]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html [:html [:head [:title (:title result)]]
                [:body (:body result)]])})

(defn result [str] (wrap-result {:title str :body [:h1 str]}))

(defn response-wrapper
  ([title body] (response-wrapper title body 200))
  ([title body status]
   {:status status
    :headers {"Content-Type" "text/html"}
    :body (xhtml [:head [:title title]]
                 [:body body])}))

(defn render-page
  "Retrieve a page from the datastore and render it for display to the user"
  [name]
  (try
    (response-wrapper name (:body (ds/retrieve Page name)))
    (catch EntityNotFoundException _
        (response-wrapper "404" (str "Unable to find page: " name) 404))))

(defn admin-add-page
  "Displays the Ackbar Admin 'Add Page' Page"
  []
  (response-wrapper "Add Page"
   (form-to [:post "/admin/add_page"]
            (label "title" "Title:")
            [:br]
            (text-field "title")
            [:br]
            (label "body" "Body:")
            [:br]
            (text-area {:cols 80 :rows 40} "body")
            [:br]
            (submit-button "Submit")
            )))

(defn add-page
  "Adds a page to the database with the specified title and content"
  [title body]
  (ds/save! (Page. (canonical-name title) body))
  (result "Page saved!")
  )

(defn admin-edit-page
  "Displays the Ackbar Admin 'Edit Page' Page"
  []
  (result "Admin Edit Page"))

(defn admin-delete-page
  "Displays the Ackbar Admin 'Delete Page' Page"
  []
  (result "Admin Delete Page"))

(defn combined-js
  "A link to the minified and combined javascript for the project"
  []
  (result "Combined Javascript"))

(defn combined-css
  "A link to the combined CSS for the project."
  []
  (result "Combined CSS"))

(defroutes ackbar-app-handler
  (GET "/" [] (render-page "home"))
  (GET "/admin/add_page" [] (admin-add-page))
  (POST "/admin/add_page" {params :params}
        (add-page (params "title") (params "body")))
  (GET "/admin/edit_page" [] (admin-edit-page))
  (POST "/admin/edit_page" {params :params} (render-page "edit page post"))
  (GET "/admin/delete_page" [] (admin-delete-page))
  (POST "/admin/delete_page" {params :params} (render-page "delete page post"))
  (GET "/static/combined.js" [] (combined-js))
  (GET "/static/combined.css" []  (combined-css))
  (GET "/:name" [name] (render-page (canonical-name name)))
  (ANY "*" [] (response-wrapper "404" "NOT FOUND!" 404)))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)
