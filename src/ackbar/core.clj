(ns ackbar.core
  (:use [compojure.core :only (GET POST ANY defroutes)])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [clojure.contrib.str-utils2 :as su]
            [ackbar.views :as vws]
            [ring.util.response :as rsp])
  (:import (com.google.appengine.api.datastore EntityNotFoundException Text)))

(ds/defentity Post [^:key url, title, body, timestamp, in-feed?])

(defn canonical-title
  "Converts a given post title to a standard format, suitable for use in a URL."
  [str]
  (su/replace (su/replace (.toLowerCase str) #"\s+" "_") #"\W+" ""))

(defn respond
  "Takes an html post body string and optional response stauts, and returns a
   Compojure response map. If status is not specified, defaults to 200."
  ([code] (respond code 200))
  ([code status]
     {:status status
      :headers {"Content-Type" "text/html"}
      :body code}))

(defn error404
  "Takes an error string and returns a Compojure error response with status 404
   displaying the error message."
  [error]
  (respond (vws/error404-template error)))

(defn single-post
  "Takes a post url and returns a Compojure response with the rendered post."
  [url]
  (try
    (def post (ds/retrieve Post url))
    (def post-response (vws/posts-view (:title post) [post]))
    (respond post-response)
    (catch EntityNotFoundException _
      (error404 (str "Unable to find post: " url)))))

(defn paginated-posts
  "Takes a page number and returns a Compojure response which displays the
   posts that should appear on that page number."
  [number]
  (def pagesize 5)
  (def posts (ds/query :kind Post :limit pagesize
                       :offset (* pagesize (dec number))
                       :filter (= :in-feed? true)
                       :sort [[:timestamp :dsc]]))
  (def prev (if (> number 1) (str "/posts/" (dec number)) nil))
  (def nxt (if (= (count posts) 5) (str "/posts/" (inc number)) nil))
  (respond (vws/posts-view "thurn.ca" posts prev nxt)))

(defn add-post
  "Adds a post to the database with the specified title and content"
  [url title body ts in-feed?]
  (if (ds/exists? Post url)
    (error404 "ERROR: Post already exists!")
    (do (ds/save! (Post. url title (Text. body) ts in-feed?))
        (rsp/redirect "/admin?msg=> post added"))))

(defn new-post
  "Takes a map holding HTTP post parameters and adds a new Post to the
   datastore with properties corresponding to those parameters."
  [params]
  (println (params "time"))
  (add-post (canonical-title (params "title"))
            (params "title")
            (params "body")
            (if (empty? (params "time"))
              (System/currentTimeMillis)
              (Long/parseLong (params "time")))
            (if (params "infeed") true false)))

(defn admin-page
  [msg]
  (def post-list (ds/query :kind Post))
  (if msg
    (respond (vws/posts-admin-view post-list msg))
    (respond (vws/posts-admin-view post-list))))

(defn paginate-handler
  [number]
  (try
    (def value (Integer/parseInt number))
    (if (< value 1)
      (error404 (str "Invalid page number " number))
      (paginated-posts value))
    (catch NumberFormatException _
      (error404 (str "Invalid page number " number)))))

(defn edit-view-handler
  [url]
  (try (respond (vws/edit-view "Edit Post" (ds/retrieve Post url)))
       (catch EntityNotFoundException _
         (error404 (str "Invalid post URL: " url)))))

(defn edit-post
  "Edits a post, updating its title and content"
  [url title body ts in-feed?]
  (if (ds/exists? Post url)
    (ds/delete! (ds/retrieve Post url)))
  (ds/save! (Post. url title (Text. body) ts in-feed?)))

(defn edit-post-handler
  [params]
  (edit-post (canonical-title (params "title"))
             (params "title")
             (params "body")
             (if (empty? (params "time"))
               (System/currentTimeMillis)
               (Long/parseLong (params "time")))
             (if (params "infeed") true false))
  (rsp/redirect "/admin?msg=> post edited"))

(defn delete-post
  "Deletes the specified post by url"
  [url]
  (try
    (do (ds/delete! (ds/retrieve Post url))
        (rsp/redirect "/admin?msg=> post deleted"))
    (catch EntityNotFoundException _
      (error404 (str "Invalid post URL: " url)))))

(defroutes ackbar-app-handler
  (GET "/" [] (paginated-posts 1))
  (GET "/posts/:number" [number] (paginate-handler number))
  (GET "/admin" {params :params} (admin-page (params "msg")))
  (GET "/admin/add" [] (respond (vws/edit-view "Add Post")))
  (POST "/admin/add" {params :params} (new-post params))
  (GET "/admin/edit/:url" [url] (edit-view-handler url))
  (POST "/admin/edit" {params :params} (edit-post-handler params))
  (POST "/admin/delete/:url" [url] (delete-post url))
  (GET "/:url" [url] (single-post (canonical-title url)))
  (ANY "*" [] (error404 "Not Found.")))

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)

