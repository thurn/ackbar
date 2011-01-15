(ns ackbar.test.core
  (:use [clojure.test :only (deftest use-fixtures is run-tests)])
  (:require [appengine-magic.testing :as ae-testing]
            [net.cgrand.enlive-html :as html]
            [ackbar.views :as vws]
            [ackbar.core :as core])
  (:import (com.google.appengine.api.datastore EntityNotFoundException Text)))

(use-fixtures :each (ae-testing/local-services :all))

(def test-posts
     [(ackbar.core.Post. "post_one" "Post One"
                         (Text. "<p>Some text</p>") 1293087600000 true)
      (ackbar.core.Post. "post_two" "More text"
                         (Text. "<p>Post Two</p>") 1292828400000 false)])

(defn tst-selector [sel resource]
  "Takes a selector and applies it to the template in 'resource'."
  (println (apply str
                  (html/emit*
                   (html/flatten-nodes-coll
                    (html/select resource sel))))))

(defn tst-snippet [snippet & args]
  "Takes a snippet function and the snippet's arguments and returns the
   html the snippet will produce for those arguments"
  (html/emit* (apply snippet args)))

(defn selector-match?
  "Takes a seq of Enlive html and a group of selectors and returns true if
   every selector matches some content in the html, false otherwise."
  [code & selectors]
  (not (some #(= (html/select code %) '()) selectors)))

(defn no-selector-match?
  "Takes a seq of Enlive html and a group of selectors and returns true if
   no selector matches the content in the html, false otherwise."
  [code & selectors]
  (every? #(= (html/select code %) '()) selectors))

(defn tst-html [code]
  "Takes a seq representing Enlive-generated HTML and produces the associated
   html text."
  (html/emit* (html/flatten-nodes-coll code)))

(def tst-posts-view
     (apply str
            (vws/posts-view "Page Title" test-posts "prev" "next")))

(def tst-upload-view
     (apply str (vws/edit-view "title")))

(def tst-posts-admin-view
     (apply str (vws/posts-admin-view test-posts)))

;;;;;;; THE LINE ;;;;;;;;

(deftest to-date
  (is (= (vws/to-date 0) "1970-01-01"))
  (is (= (vws/to-date 1) "1970-01-01"))
  (is (= (vws/to-date -1) "1969-12-31"))
  (is (= (vws/to-date (* Integer/MAX_VALUE 2)) "1970-02-19"))
  (is (= (vws/to-date 1294008964000) "2011-01-02"))
  (is (thrown? NullPointerException (vws/to-date nil)))
  (is (thrown? ClassCastException (vws/to-date ""))))

(deftest post-snippet
  (doseq [post test-posts]
    (is (selector-match?
         (vws/post-snippet post)
         [:.post] [:.title] [:.date] [:.body]))))

(deftest post-admin-snippet
  (doseq [post test-posts]
    (is (selector-match?
         (vws/post-admin-snippet post)
         [:.admin-entry] [:.title] [:.edit-form] [:form] [:.delete-form]))))

(deftest notification-snippet
  (doseq [text ["next" "prev" "" nil]]
    (is (selector-match?
         (vws/notification-snippet text)
         [:.notification] [:div]))))

(deftest page-nav-snippet
  (let [code (vws/page-nav-snippet nil nil)]
    (is (selector-match? code [:#page-nav]))
    (is (no-selector-match? code [:#prev-page] [:#next-page])))
  (let [code (vws/page-nav-snippet "prev" nil)]
    (is (selector-match? code [:#page-nav] [:#prev-page]))
    (is (no-selector-match? code [:#next-page])))
  (let [code (vws/page-nav-snippet nil "next")]
    (is (selector-match? code [:#page-nav] [:#next-page]))
    (is (no-selector-match? code [:#prev-page])))
  (let [code (vws/page-nav-snippet "prev" "next")]
    (is (selector-match? code [:#page-nav] [:#prev-page] [:#next-page]))))

(deftest edit-snippet
  (doseq [post test-posts]
    (is (selector-match?
         (vws/edit-snippet post)
         [:#main] [:#instructions] [:form] [:label] [:input] [:textarea]
         [:#title] [:#time] [:#infeed] [:#body]))))

(deftest base-template
  (is true ""))

(deftest posts-view
  (is true ""))

(deftest posts-admin-view
  (is true ""))

(deftest edit-view
  (is true ""))

(deftest error404-template
  (is true ""))

(defn run [] (run-tests 'ackbar.test.core))