require 'rake'
require 'fileutils'
include FileUtils

task :prepare do
  system "lein appengine-prepare"
end

task :ds => [:prepare] do
  exec "./appengine-java-sdk-1.3.8/bin/dev_appserver.sh resources/"
end

task :deploy => [:prepare] do
  exec "./appengine-java-sdk-1.3.8/bin/appcfg.sh update resources/"
end

task :rollback => [:prepare] do
  exec "./appengine-java-sdk-1.3.8/bin/appcfg.sh rollback resources/"
end

task :swank do
  exec "lein swank"
end

task :repl do
  exec "lein repl"
end

task :clean do
  exec "lein clean"
end

task :deps do
  rm_f 'appengine-java-sdk-1.3.8.zip'
  rm_rf 'appengine-java-sdk-1.3.8'
  `wget 'http://googleappengine.googlecode.com/files/appengine-java-sdk-1.3.8.zip'`
  `unzip appengine-java-sdk-1.3.8.zip`
  rm 'appengine-java-sdk-1.3.8.zip'
  exec "lein deps"
end

task :destroy do
  rm_f 'appengine-java-sdk-1.3.8.zip'
  rm_rf 'appengine-java-sdk-1.3.8'
  rm_rf 'classes'
  rm_rf 'lib'
  rm_rf 'resources/WEB-INF/lib'
end
