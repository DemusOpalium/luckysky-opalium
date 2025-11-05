#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || {
        echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
        exit 1
    }
fi

# Increase the maximum file descriptors if we can.
MAX_FD="maximum"
case "`uname`" in
  Darwin* )
    GRADLE_OPTS="$GRADLE_OPTS -Xdock:name=$APP_NAME -Xdock:icon=$APP_HOME/media/gradle.icns"
    ;;
esac

# For Cygwin or MSYS, switch paths to Windows format before running java
cygwin=false
msys=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  MSYS* | MINGW* )
    msys=true
    ;;
esac

if $cygwin || $msys ; then
  APP_HOME=`cygpath --path --mixed "$APP_HOME"`
  CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
  JAVACMD=`cygpath --unix "$JAVACMD"`
fi

# Collect all arguments for the java command.
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"

exec "$JAVACMD" "$@"
