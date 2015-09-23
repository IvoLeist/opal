#!/bin/bash

getPidFile() {
   while getopts ":p:" opt; do
     case $opt in
       p)
         echo $OPTARG
         return 0
         ;;
     esac
   done

   return 1
}

# OS specific support.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if [ -z "$JAVA_OPTS" ]
then
  if [ ! -z "$JAVA_ARGS" ]
  then
    JAVA_OPTS=$JAVA_ARGS
  else
    # Set default JAVA_OPTS
    JAVA_OPTS="-Xmx2G -XX:MaxPermSize=128M"
  fi

  export JAVA_OPTS
fi

# The directory containing the opal shell script
OPAL_BIN_DIR=`dirname $0`
# resolve links - $0 may be a softlink
OPAL_DIST=$(readlink -f $OPAL_BIN_DIR/..)

export OPAL_DIST

echo "JAVA_OPTS=$JAVA_OPTS"
echo "OPAL_HOME=$OPAL_HOME"
echo "OPAL_DIST=$OPAL_DIST"
echo "OPAL_LOG=$OPAL_LOG"

if [ -z "$OPAL_HOME" ]
then
  echo "OPAL_HOME not set."
  exit 2;
fi

if $cygwin; then
  # For Cygwin, ensure paths are in UNIX format before anything is touched
  [ -n "$OPAL_DIST" ] && OPAL_BIN=`cygpath --unix "$OPAL_DIST"`
  [ -n "$OPAL_HOME" ] && OPAL_HOME=`cygpath --unix "$OPAL_HOME"`

  # For Cygwin, switch paths to Windows format before running java
  export OPAL_DIST=`cygpath --absolute --windows "$OPAL_DIST"`
  export OPAL_HOME=`cygpath --absolute --windows "$OPAL_HOME"`
fi

# Java 6 supports wildcard classpath entries
# http://download.oracle.com/javase/6/docs/technotes/tools/solaris/classpath.html
CLASSPATH=$OPAL_HOME/conf:"$OPAL_DIST/lib/*"
if $cygwin; then
  CLASSPATH=$OPAL_HOME/conf;"$OPAL_DIST/lib/*"
fi

[ -e "$OPAL_HOME/logs" ] || mkdir "$OPAL_HOME/logs"

JAVA_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n

# Add $JAVA_DEBUG to this line to enable remote JVM debugging (for developers)
exec java $JAVA_OPTS -cp "$CLASSPATH" -DOPAL_HOME="${OPAL_HOME}" \
  -DOPAL_DIST=${OPAL_DIST} -DOPAL_LOG=${OPAL_LOG}  org.obiba.opal.server.OpalServer "$@" >$OPAL_LOG/stdout.log 2>&1 &

# On CentOS 'daemon' function does not initialize the pidfile
pidfile=$(getPidFile $@)

if [ ! -z "$pidfile" ]; then
  echo $! > $pidfile
fi