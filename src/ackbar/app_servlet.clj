(ns ackbar.app_servlet
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use ackbar.core)
  (:use [appengine-magic.servlet :only [make-servlet-service-method]]))


(defn -service [this request response]
  ((make-servlet-service-method ackbar-app) this request response))
