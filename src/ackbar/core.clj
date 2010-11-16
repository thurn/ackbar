(ns ackbar.core
  (:use compojure.core hiccup.core)
  (:require [appengine-magic.core :as ae]))

(defn wrap-result [result]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html [:html [:head [:title (:title result)]]
                [:body (:body result)]])})

(defn result [str] (wrap-result {:title str :body [:h1 str]}))

(defn render-page
  "Retrieve a page from the datastore and render it for display to the user"
  [page]
  (result (str "Rendered Page: " page)))

(defn admin-page
  "Displays the Ackbar Admin Page"
  []
  (result "Admin Page"))

(defn admin-login-page
  "Displays the Ackbar Admin Login Page"
  []
  (result "Admin Login Page"))

(defn admin-logout-page
  "Displays the Ackbar Admin Logout Page"
  []
  (result "Admin Logout Page"))

(defn admin-add-page
  "Displays the Ackbar Admin 'Add Page' Page"
  []
  (result "Admin Page"))

(defn admin-edit-page
  "Displays the Ackbar Admin 'Edit Page' Page"
  []
  (result "Admin Page"))

(defn admin-delete-page
  "Displays the Ackbar Admin 'Delete Page' Page"
  []
  (result "Admin Page"))

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
  str
  )

(defroutes ackbar-app-handler
  (GET "/" [] (render-page "home"))
  (GET "/admin/home" [] (admin-page))
  (GET "/admin/login" [] (admin-login-page))
  (GET "/admin/logout" [] (admin-logout-page))
  (GET "/admin/add-page" [] (admin-add-page))
  (GET "/admin/edit-page" [] (admin-edit-page))
  (GET "/admin/delete-page" [] (admin-delete-page))
  (GET "/static/combined.js" [] (combined-js))
  (GET "/static/combined.css" []  (combined-css))
  (GET "/:name" [name] (render-page (canonical-name name))))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)
