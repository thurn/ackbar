(ns ackbar.core
  (:use compojure.core hiccup.core hiccup.form-helpers hiccup.page-helpers)
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clojure.contrib.str-utils2 :as s])
  (:import com.google.appengine.api.datastore.EntityNotFoundException
           java.text.SimpleDateFormat
           java.util.Date))

(ds/defentity Page [^:key url, title, body, timestamp])

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
  [url]
  (try
    (response-wrapper url (:body (ds/retrieve Page url)))
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " url) 404))))

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
  [url title body]
  (if (ds/exists? Page url)
    (result "ERROR: Page already exists!")
    (do (ds/save! (Page. url title body (System/currentTimeMillis)))
        (result "Page saved!"))))

(defn page-link
  "Builds a link to the specified page object"
  [page]
  (link-to (str "/" (:url page)) (:title page)))

(defn edit-link
  "Builds a link to edit the specified page"
  [page]
  (link-to (str "/admin/edit/" (:url page)) "edit"))

(defn delete-link
  "Builds a link to delete the specified page"
  [page]
  (form-to [:post (str "/admin/delete/" (:url page))]
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
  "Deletes the specified page by url"
  [url]
  (try
    (do (ds/delete! (ds/retrieve Page url))
        (response-wrapper "Deleted"
                          (str "Page deleted: " url)))
    
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " url) 404))))

(defn admin-edit-page
  "Displays the Ackbar Admin 'Edit Page' Page"
  [url]
  (try
    (def page (ds/retrieve Page url))
    (response-wrapper "Edit Page"
                      (add-or-edit-form "/admin/edit"
                                        (:title page)
                                        (:body page)))
    (catch EntityNotFoundException _
      (response-wrapper "404" (str "Unable to find page: " url) 404))))

(defn edit-page
  "Edits a page, updating its title and content"
  [url title body]
  (if (ds/exists? Page url)
    (ds/delete! (ds/retrieve Page url)))
  (ds/save! (Page. url title body (System/currentTimeMillis)))
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

(def date-format (SimpleDateFormat. "yyyy-MM-dd"))

(defn to-date
  "Converts a UNIX timstamp to a date string"
  [timestamp]
  (.format date-format (Date. timestamp)))

(defn render-paginate
  "Renders a fixed number of pages"
  [number]
  (def pagesize 10)
  (def pages (ds/query :kind Page :limit pagesize
                       :offset (* pagesize number)
                       :sort [[:timestamp :dsc]]))
  (response-wrapper
   "Paginated"
   (html (for [page pages]
           [:div {:class "post"} [:h1 {:class "title"} (page-link page)]
            [:div {:class "date"} (to-date (:timestamp page))]
            (:body page)])
         (if (> number 0)
           (html (link-to (str "/pages/" (dec number)) "prev") [:br]))
         (if (= pagesize (count pages))
           (link-to (str "/pages/" (inc number)) "next")))))

(defroutes ackbar-app-handler
  (GET "/" [] (render-paginate 0))
  (GET "/pages/:number" [number] (render-paginate (Integer/parseInt number)))
  (GET "/admin/add" [] (admin-add-page))
  (GET "/admin/pages" [] (admin-page-list))
  (POST "/admin/add" {params :params}
        (add-page (canonical-title (params "title"))
                  (params "title")
                  (params "body")))
  (GET "/admin/edit/:title" [title] (admin-edit-page title))
  (POST "/admin/edit" {params :params}
        (edit-page (canonical-title (params "title"))
                  (params "title")
                  (params "body")))
  (POST "/admin/delete/:title" [title] (delete-page title))
  (GET "/pages" [] (all-pages))
  (GET "/:title" [title] (render-page (canonical-title title)))
  (ANY "*" [] (response-wrapper "404" "NOT FOUND!" 404)))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)
