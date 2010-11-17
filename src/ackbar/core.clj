;; It's a Trap!

(ns ackbar.core
  (:use compojure.core hiccup.core hiccup.form-helpers hiccup.page-helpers)
  (:require [appengine-magic.core :as ae]))

(defn wrap-result [result]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html [:html [:head [:title (:title result)]]
                [:body (:body result)]])})

(defn result [str] (wrap-result {:title str :body [:h1 str]}))

(defn response-wrapper [title body]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (xhtml [:head [:title title]]
                [:body body])})

(defn render-page
  "Retrieve a page from the datastore and render it for display to the user"
  [page]
  (result (str "Rendered Page: " page)))

(defn admin-add-page
  "Displays the Ackbar Admin 'Add Page' Page"
  []
  (response-wrapper "Add Page"
   (form-to [:post "/admin/add-page"]
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

(defn canonical-name
  "Converts a given page name to a standard format, suitable for use in a URL."
  [str]
  str)

                                        ; Special Pages: home, admin, 404, navbar
(defroutes ackbar-app-handler
  (GET "/" [] (render-page "home"))
  (GET "/admin/add-page" [] (admin-add-page))
  (POST "/admin/add-page" {params :params} (render-page "add page post"))
  (GET "/admin/edit-page" [] (admin-edit-page))
  (POST "/admin/edit-page" {params :params} (render-page "edit page post"))
  (GET "/admin/delete-page" [] (admin-delete-page))
  (POST "/admin/delete-page" {params :params} (render-page "delete page post"))
  (GET "/static/combined.js" [] (combined-js))
  (GET "/static/combined.css" []  (combined-css))
  (GET "/:name" [name] (render-page (canonical-name name)))
  (ANY "*" [] (render-page "404")))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)
