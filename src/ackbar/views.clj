(ns ackbar.views
  (:require [net.cgrand.enlive-html :as html]
            [appengine-magic.core :as ae]))

(defn to-date
  "Converts a UNIX timstamp to a date string."
  [timestamp]
  (def date-format (java.text.SimpleDateFormat. "yyyy-MM-dd"))
  (.setTimeZone date-format (java.util.TimeZone/getTimeZone "GMT"))
  (.format date-format (java.util.Date. (long timestamp))))

(html/defsnippet
  ^{:doc "A snippet which takes a post definition (ackbar.core.Post object) and
    returns the enlive html seq for that post."}
  post-snippet "posts.html"
  [:.post]
  [post]
  [:.title :> :a] (html/do->              
                   (html/content (:title post))
                   (html/set-attr :href (str "/" (:url post))))
  [:.date] (html/content (to-date (:timestamp post))) 
  [:.body] (html/html-content (.getValue (:body post))))

(html/defsnippet
  ^{:doc "A snippet which takes a post defintion (ackbar.core.Post object) and
    returns the enlive html seq for an administrator view of that post, with
    edit and delete links."}
  post-admin-snippet "admin.html"
  [:.admin-entry]
  [post]
  [:.title :> :a] (html/do->              
                   (html/content (:title post))
                   (html/set-attr :href (str "/" (:url post))))
  [:.edit-form] (html/set-attr :action (str "/admin/edit/" (:url post)))
  [:.delete-form] (html/set-attr :action (str "/admin/delete/" (:url post))))

(html/defsnippet
  ^{:doc "Takes a string of text and returns enlive html for a notification div
    for that text."}
  notification-snippet "admin.html"
  [:.notification]
  [text]
  [:.notification] (html/content text))

(html/defsnippet
  ^{:doc "A snippet which takes a previous page url and a next page url. For the
    urls which are not nil, renders a link to them."}
  page-nav-snippet "posts.html"
  [:#page-nav]
  [prev-link next-link]
  [:#next-page] (if next-link
                  (html/set-attr :href next-link)
                  (html/substitute ""))
  [:#prev-page] (if prev-link
                  (html/set-attr :href prev-link)
                  (html/substitute "")))

(html/defsnippet
  ^{:doc "A snippet which populates the edit/upload form with the values from a
    specified ackbar.ui.Post object and changes the form action to /admin/edit"}
  edit-snippet "upload.html"
  [:#main]
  [post]
  [:h1] (html/content "Edit Post")
  [:form] (html/set-attr :action "/admin/edit")
  [:#title] (html/set-attr :value (:title post))
  [:#time] (html/set-attr :value (:timestamp post))
  [:#infeed] (if (:in-feed? post)
               (html/set-attr :checked "checked")
               (html/remove-attr :checked))
  [:#body] (html/content (.getValue (:body post))))

(html/deftemplate
  ^{:doc "A template which takes a title and one or more body functions. The
    body functions are called in order on the main div of the base template, and
    the title is set as the page title."}
  base-template "base.html"
  [title & body-fns]
  [:title] (html/content title)
  [:#main] (apply html/do-> body-fns))

(defn posts-view
  "Takes a title and a seq of Post objects and returns an enlive-style HTML seq
   for a page displaying the specified Post objects. Optionally accepts a pair
   of 'prev' and 'next' links for pagination at the bottom."
  ([title posts] (posts-view title posts nil nil))
  ([title posts prev nxt]
     (def main-content (html/content (map post-snippet posts)))
     (def page-nav (html/append (page-nav-snippet prev nxt)))
     (base-template title main-content page-nav)))

(defn posts-admin-view
  "Takes a seq of Post objects and returns an enlive HTML seq for a page
   displaying the titles of the post objects along with edit/delete links.
   Optionally takes a notification message to be displayed to the user."
  ([posts] (posts-admin-view posts nil))
  ([posts notification]
     (def post-list (html/content (map post-admin-snippet posts)))
     (def notice (html/prepend (notification-snippet notification)))
     (base-template "Admin" post-list notice)))

(defn edit-view
  "Takes a page title and optionally an ackbar.ui.Post object. Returns
   enlive html for an edit/upload page by that title. If a post object was
   specified, populates the form with its content and makes the form's action
   /admin/edit instead of /admin/add."
  ([title] (edit-view title nil))
  ([title post]
     (def form (if post
                 (edit-snippet post)
                 ((html/snippet "upload.html" [:#main] []))))
     (base-template title (html/substitute form))))

(html/deftemplate
  ^{:doc "The 404 error page template"}
  error404-template "404.html"
  [error-message]
  [:h1] (html/content error-message))
