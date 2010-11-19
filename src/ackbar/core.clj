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
                    (form-to [:post "/admin/add"]
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

(defn page-link
  "Builds a link to the specified page object"
  [page]
  (link-to (str "/" (:name page)) (:name page)))

(defn edit-link
  "Builds a link to edit the specified page"
  [page]
  (link-to (str "/admin/edit/" (:name page)) "edit"))

(defn delete-link
  "Builds a link to delete the specified page"
  [page]
  (form-to [:post (str "/admin/delete/" (:name page))]
           (submit-button "Delete")))

(defn admin-page-list
  "The admin page listing all pages along with edit and delete links"
  []
  (def page-list (ds/query :kind Page))
  (if (empty? page-list)
    (response-wrapper "No Pages Yet!" "No pages.")
    (response-wrapper "Page List"
                      [:ul (for [page page-list]
                             [:li (page-link page) [:br]
                              (edit-link page) (delete-link page)])])))

(defn delete-page
  "Deletes the specified page by name."
  [name]
    (try
      (do (ds/delete! (ds/retrieve Page name))
          (response-wrapper "Deleted"
                            (str "Page deleted: " name)))
      
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " name) 404))))

(defn admin-edit-page
  "Displays the Ackbar Admin 'Edit Page' Page"
  []
  (result "Admin Edit Page"))

(defroutes ackbar-app-handler
  (GET "/" [] (render-page "home"))
  (GET "/admin/add" [] (admin-add-page))
  (GET "/admin/pages" [] (admin-page-list))
  (POST "/admin/add" {params :params}
        (add-page (params "title") (params "body")))
  (GET "/admin/edit/:name" [name] (admin-edit-page name))
  (POST "/admin/edit/:name" [name params] (render-page "edit page post"))
  (POST "/admin/delete/:name" [name] (delete-page name))
  (GET "/:name" [name] (render-page (canonical-name name)))
  (ANY "*" [] (response-wrapper "404" "NOT FOUND!" 404)))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)
