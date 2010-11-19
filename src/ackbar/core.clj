(ns ackbar.core
  (:use compojure.core hiccup.core hiccup.form-helpers hiccup.page-helpers)
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clojure.contrib.str-utils2 :as s])
  (:import com.google.appengine.api.datastore.EntityNotFoundException))

(ds/defentity Page [^:key title, body])

(defn canonical-title
  "Converts a given page title to a standard format, suitable for use in a URL."
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
  [title]
  (try
    (response-wrapper title (:body (ds/retrieve Page title)))
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " title) 404))))

(defn add-or-edit-form
  "Returns a form to add or edit a page."
  ([url] (add-or-edit-form url "" ""))
  ([url title body]
     (form-to [:post url]
              (label "title" "Title:") [:br]
              (text-field "title" title) [:br]
              (label "body" "Body:") [:br]
              (text-area {:cols 80 :rows 40} "body" body) [:br]
              (submit-button "Submit"))))
                                 
(defn admin-add-page
  "Displays the Ackbar Admin 'Add Page' Page"
  []
  (response-wrapper "Add Page"
                    (add-or-edit-form "/admin/add")))

(defn add-page
  "Adds a page to the database with the specified title and content"
  [title body]
  (if (ds/exists? Page title)
    (result "ERROR: Page already exists!")
    (do (ds/save! (Page. (canonical-title title) body))
        (result "Page saved!"))))

(defn page-link
  "Builds a link to the specified page object"
  [page]
  (link-to (str "/" (:title page)) (:title page)))

(defn edit-link
  "Builds a link to edit the specified page"
  [page]
  (link-to (str "/admin/edit/" (:title page)) "edit"))

(defn delete-link
  "Builds a link to delete the specified page"
  [page]
  (form-to [:post (str "/admin/delete/" (:title page))]
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
  "Deletes the specified page by title."
  [title]
  (try
    (do (ds/delete! (ds/retrieve Page title))
        (response-wrapper "Deleted"
                          (str "Page deleted: " title)))
      
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " title) 404))))

(defn admin-edit-page
  "Displays the Ackbar Admin 'Edit Page' Page"
  [title]
  (try
    (def page (ds/retrieve Page title))
    (response-wrapper "Edit Page"
                      (add-or-edit-form "/admin/edit"
                                        (:title page)
                                        (:body page)))
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " title) 404))))

(defn edit-page
  "Edits a page, updating its title and content"
  [title body]
  (if (ds/exists? Page (canonical-title title))
    (ds/delete! (ds/retrieve Page (canonical-title title))))
  (ds/save! (Page. (canonical-title title) body))
  (result "Page updated!"))

(defn all-pages
  "Renders a page with all of the pages inserted in the datastore sorted
   chronologically"
  []
  (def pages (ds/query :kind Page))
  (response-wrapper
   "All Pages"
   (for [page pages]
     [:div [:h1 (:title page)]
     [:div (:body page)]])))
                    
(defroutes ackbar-app-handler
  (GET "/" [] (render-page "home"))
  (GET "/admin/add" [] (admin-add-page))
  (GET "/admin/pages" [] (admin-page-list))
  (POST "/admin/add" {params :params}
        (add-page (params "title") (params "body")))
  (GET "/admin/edit/:title" [title] (admin-edit-page title))
  (POST "/admin/edit" {params :params}
        (edit-page (params "title") (params "body")))
  (POST "/admin/delete/:title" [title] (delete-page title))
  (GET "/all" [] (all-pages))
  (GET "/:title" [title] (render-page (canonical-title title)))
  (ANY "*" [] (response-wrapper "404" "NOT FOUND!" 404)))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)
