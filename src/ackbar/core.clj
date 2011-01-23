(ns ackbar.core
  (:use [compojure.core :only (GET POST ANY defroutes)])
  (:require [appengine-magic.core :as ae]
            [appengine-magic.services.datastore :as ds]
            [appengine-magic.services.blobstore :as blobs]
            [appengine-magic.services.memcache :as mc]
            [clojure.contrib.str-utils2 :as su]
            [ackbar.views :as vws]
            [ring.util.response :as rsp])
  (:import (com.google.appengine.api.datastore EntityNotFoundException Text)))

(ds/defentity Post [^:key url, title, body, timestamp, in-feed?, category])
(ds/defentity UploadedFile [^:key filename, blob-key])

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
  (respond (vws/error404-template error) 404))

(defmacro memcache
  "Takes a memcache key and an s-expression that will evaluate to something
   serializable. If the key exists in memcache, returns the value under the
   key. Otherwise, evaluates the s-expression and caches it under the key."
  [memcache-key value]
  `(if (mc/contains? ~memcache-key)
     (do (mc/get ~memcache-key))
     (do
       (println "Memcache miss on key " ~memcache-key)
       (def value# ~value)
       (mc/put! ~memcache-key value#)
       value#)))

(defn single-post
  "Takes a post url and returns a Compojure response with the rendered post."
  [url]
  (def post (memcache url (ds/retrieve Post url)))
  (if (nil? post)
    (error404 (str "Unable to find post: " url))
    (respond (vws/posts-view (:title post) [post]))))

(defn paginated-posts
  "Takes a page number and returns a Compojure response which displays the
   posts that should appear on that page number."
  [number]
  (def pagesize 5)
  (def posts (memcache (str "_home_" number)
                       (doall (ds/query :kind Post :limit pagesize
                                        :offset (* pagesize (dec number))
                                        :filter (= :in-feed? true)
                                        :sort [[:timestamp :dsc]]))))
  (def prev (if (> number 1) (str "/posts/" (dec number)) nil))
  (def nxt (if (= (count posts) 5) (str "/posts/" (inc number)) nil))
  (respond (vws/posts-view "thurn.ca" posts prev nxt)))

(defn feed-posts
  "Takes a number of posts to display and returns XML for an RSS feed that
   contains the indicated number of posts."
  [number]
  (def posts (memcache (str "_rss_" number)
                       (doall (ds/query :kind Post :limit number
                                        :filter (= :in-feed? true)
                                        :sort [[:timestamp :dsc]]))))
  (respond (vws/feed-view posts)))

(defn refresh-cache
  "Invalidates the memcache entry for a specific URL. Also invalidates the home
   page and rss page memcache entries for good measure."
  [url]
  (mc/delete! "_home_1")
  (mc/delete! "_rss_5")
  (mc/delete! url)
  (paginated-posts 1)
  (feed-posts 5)
  (single-post url)
  nil)

(defn add-post
  "Adds a post to the database with the specified title and content"
  [url title body ts in-feed? category]
  (if (ds/exists? Post url)
    (error404 "ERROR: Post already exists!")
    (do (ds/save! (Post. url title (Text. body) ts in-feed? category))
        (refresh-cache url)
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
            (if (params "infeed") true false)
            (params "category")))

(defn admin-page
  [msg]
  (def post-list (ds/query :kind Post))
  (def file-list (ds/query :kind UploadedFile))
  (if msg
    (respond (vws/posts-admin-view post-list file-list msg))
    (respond (vws/posts-admin-view post-list file-list))))

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
  (def post (ds/retrieve Post url))
  (if (nil? post)
    (error404 (str "Invalid post URL: " url))
    (respond (vws/edit-view "Edit Post" post))))

(defn edit-post
  "Edits a post, updating its title and content"
  [url title body ts in-feed? category]
  (if (ds/exists? Post url)
    (ds/delete! (ds/retrieve Post url)))
  (ds/save! (Post. url title (Text. body) ts in-feed? category))
  (refresh-cache url))

(defn edit-post-handler
  [params]
  (edit-post (canonical-title (params "title"))
             (params "title")
             (params "body")
             (if (empty? (params "time"))
               (System/currentTimeMillis)
               (Long/parseLong (params "time")))
             (if (params "infeed") true false)
             (params "category"))
  (rsp/redirect "/admin?msg=> post edited"))

(defn delete-post
  "Deletes the specified post by url"
  [url]
  (def post (ds/retrieve Post url))
  (if (nil? post)
    (error404 (str "Invalid post URL: " url))
    (do (ds/delete! post)
        (refresh-cache url)
        (rsp/redirect "/admin?msg=> post deleted"))))

(defn delete-file
  "Deletes the specified file by name"
  [name]
  (def file (ds/retrieve UploadedFile name))
  (if (nil? file)
    (error404 (str "Invalid file name: " name))
    (do
      (blobs/delete! (:blob-key file))
      (ds/delete! file)
      (rsp/redirect "/admin?msg=> file deleted"))))

(defroutes ackbar-app-handler
  (GET "/" _ (paginated-posts 1))
  (GET "/posts/:number" [number] (paginate-handler number))
  (GET "/feed/" _ (feed-posts 5))
  (GET "/admin" {params :params} (admin-page (params "msg")))
  (GET "/admin/add" [] (respond (vws/edit-view "Add Post")))
  (POST "/admin/add" {params :params} (new-post params))
  (GET "/admin/edit/:url" [url] (edit-view-handler url))
  (POST "/admin/edit" {params :params} (edit-post-handler params))
  (POST "/admin/delete/:url" [url] (delete-post url))
  
  (GET "/admin/upload" [] (respond (vws/upload-view "Upload File")))
  
  ;; success callback
  (POST "/admin/upload-callback" req
        (let [blob-map (blobs/uploaded-blobs req)]
          (def blob-key (.getKeyString (blob-map "file")))
          (blobs/callback-complete req (str "/admin/name-file?key=" blob-key))))

  (GET "/admin/name-file" {params :params}
       (vws/name-view "Name File" (params "key")))

  (POST "/admin/name-file" {params :params}
        (if (ds/exists? UploadedFile (params "name"))
          (do
            (blobs/delete! (params "key"))
            (error404 "ERROR: File already exists!"))
          (do (ds/save! (UploadedFile. (params "name") (params "key")))
              (rsp/redirect "/admin?msg=> file saved"))))
  
  (GET "/files/:name.:ext" {{:strs [name ext]} :params :as req}
       (def file (ds/retrieve UploadedFile (str name "." ext)))
       (if (nil? file)
         (error404 (str "Unable to find file: " (str name "." ext)))
         (blobs/serve req (:blob-key file))))

  (POST "/admin/delete-file/:name.:ext" {params :params}
        (delete-file (str (params "name") "." (params "ext"))))
  
  (GET "/:url" [url] (single-post (canonical-title url)))
  (ANY "*" [] (error404 "Not Found."))
  )

(ae/def-appengine-app ackbar-app #'ackbar-app-handler)

