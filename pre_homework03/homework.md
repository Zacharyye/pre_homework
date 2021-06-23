### 作业：分析Tomcat容器调用shutdown.sh脚本最终如何关闭Tomcat服务器，参考：`org.apache.catalina.startup.Bootstrap`

> 本地版本 `tomcat-8.5.63`

#### (1) shutdown.sh脚本

```shell
#!/bin/sh
# 检测是否OS/400系统
# Better OS/400 detection: see Bugzilla 31132
os400=false
case "`uname`" in
OS400*) os400=true;;
esac

# 取脚本名称
# resolve links - $0 may be a softlink
PRG="$0"

# 判断是否是软连接
while [ -h "$PRG" ] ; do
	# 查看脚本信息
  ls=`ls -ld "$PRG"`
  # 获取真实脚本路径
  link=`expr "$ls" : '.*-> \(.*\)$'`
  # 是否是绝对路径
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# 取目录名称
PRGDIR=`dirname "$PRG"`
# 定义执行脚本
EXECUTABLE=catalina.sh

# 校验执行脚本是否存在
# Check that target executable exists
if $os400; then
  # -x will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  eval
else
	# 判断是否可执行
  if [ ! -x "$PRGDIR"/"$EXECUTABLE" ]; then
    echo "Cannot find $PRGDIR/$EXECUTABLE"
    echo "The file is absent or does not have execute permission"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# 执行catalina.sh stop 脚本，并附带所有参数
exec "$PRGDIR"/"$EXECUTABLE" stop "$@"

```

#### (2) catalina.sh脚本

```shell
#!/bin/sh

# 操作系统检测
# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
os400=false
hpux=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
HP-UX*) hpux=true;;
esac

# 软连接检测
# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
	# 查看脚本信息
  ls=`ls -ld "$PRG"`
  # 获取真实脚本路径
  link=`expr "$ls" : '.*-> \(.*\)$'`
  # 是否绝对路径
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# 获取目录名称
# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# 设置 CATALINA_HOME 
# Only set CATALINA_HOME if not already set
[ -z "$CATALINA_HOME" ] && CATALINA_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`

# 设置 CATALINA_BASE 
# Copy CATALINA_BASE from CATALINA_HOME if not already set
[ -z "$CATALINA_BASE" ] && CATALINA_BASE="$CATALINA_HOME"

# 设置 CLASSPATH
# Ensure that any user defined CLASSPATH variables are not used on startup,
# but allow them to be specified in setenv.sh, in rare case when it is needed.
CLASSPATH=

if [ -r "$CATALINA_BASE/bin/setenv.sh" ]; then
  . "$CATALINA_BASE/bin/setenv.sh"
elif [ -r "$CATALINA_HOME/bin/setenv.sh" ]; then
  . "$CATALINA_HOME/bin/setenv.sh"
fi

# Cygwin 转换路径
# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JRE_HOME" ] && JRE_HOME=`cygpath --unix "$JRE_HOME"`
  [ -n "$CATALINA_HOME" ] && CATALINA_HOME=`cygpath --unix "$CATALINA_HOME"`
  [ -n "$CATALINA_BASE" ] && CATALINA_BASE=`cygpath --unix "$CATALINA_BASE"`
  [ -n "$CLASSPATH" ] && CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# 检测CATALINA_HOME和CATALINA_BASE中是否含有冒号（:），有即提示错误，退出执行
# Ensure that neither CATALINA_HOME nor CATALINA_BASE contains a colon
# as this is used as the separator in the classpath and Java provides no
# for escaping if the same character appears in the path.
case $CATALINA_HOME in
  *:*) echo "Using CATALINA_HOME:   $CATALINA_HOME";
       echo "Unable to start as CATALINA_HOME contains a colon (:) character";
       exit 1;
esac
case $CATALINA_BASE in
  *:*) echo "Using CATALINA_BASE:   $CATALINA_BASE";
       echo "Unable to start as CATALINA_BASE contains a colon (:) character";
       exit 1;
esac

# OS400 设置优先级、开启多线程支持
# For OS400
if $os400; then
  # Set job priority to standard for interactive (interactive - 6) by using
  # the interactive priority - 6, the helper threads that respond to requests
  # will be running at the same priority as interactive jobs.
  COMMAND='chgjob job('$JOBNAME') runpty(6)'
  system $COMMAND

  # Enable multi threading
  export QIBM_MULTI_THREADED=Y
fi

# 获取Java环境变量
# Get standard Java environment variables
if $os400; then
  # -r will Only work on the os400 if the files are:
  # 1. owned by the user
  # 2. owned by the PRIMARY group of the user
  # this will not work if the user belongs in secondary groups
  . "$CATALINA_HOME"/bin/setclasspath.sh
else
  if [ -r "$CATALINA_HOME"/bin/setclasspath.sh ]; then
    . "$CATALINA_HOME"/bin/setclasspath.sh
  else
    echo "Cannot find $CATALINA_HOME/bin/setclasspath.sh"
    echo "This file is needed to run this program"
    exit 1
  fi
fi

# 添加另外jar包
# Add on extra jar files to CLASSPATH
if [ ! -z "$CLASSPATH" ] ; then
  CLASSPATH="$CLASSPATH":
fi
CLASSPATH="$CLASSPATH""$CATALINA_HOME"/bin/bootstrap.jar

# 日志输出
if [ -z "$CATALINA_OUT" ] ; then
  CATALINA_OUT="$CATALINA_BASE"/logs/catalina.out
fi

# 临时目录
if [ -z "$CATALINA_TMPDIR" ] ; then
  # Define the java.io.tmpdir to use for Catalina
  CATALINA_TMPDIR="$CATALINA_BASE"/temp
fi

# 添加 tomcat-juli.jar
# Add tomcat-juli.jar to classpath
# tomcat-juli.jar can be over-ridden per instance
if [ -r "$CATALINA_BASE/bin/tomcat-juli.jar" ] ; then
  CLASSPATH=$CLASSPATH:$CATALINA_BASE/bin/tomcat-juli.jar
else
  CLASSPATH=$CLASSPATH:$CATALINA_HOME/bin/tomcat-juli.jar
fi

# Bugzilla 37848: When no TTY is available, don't output to console
have_tty=0
if [ -t 0 ]; then
    have_tty=1
fi

# 路径转换成WINDOWS格式
# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  JAVA_HOME=`cygpath --absolute --windows "$JAVA_HOME"`
  JRE_HOME=`cygpath --absolute --windows "$JRE_HOME"`
  CATALINA_HOME=`cygpath --absolute --windows "$CATALINA_HOME"`
  CATALINA_BASE=`cygpath --absolute --windows "$CATALINA_BASE"`
  CATALINA_TMPDIR=`cygpath --absolute --windows "$CATALINA_TMPDIR"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  [ -n "$JAVA_ENDORSED_DIRS" ] && JAVA_ENDORSED_DIRS=`cygpath --path --windows "$JAVA_ENDORSED_DIRS"`
fi

if [ -z "$JSSE_OPTS" ] ; then
  JSSE_OPTS="-Djdk.tls.ephemeralDHKeySize=2048"
fi
JAVA_OPTS="$JAVA_OPTS $JSSE_OPTS"

# 注册自定义URL处理器
# Register custom URL handlers
# Do this here so custom URL handles (specifically 'war:...') can be used in the security policy
JAVA_OPTS="$JAVA_OPTS -Djava.protocol.handler.pkgs=org.apache.catalina.webresources"

# 检验 LOGGING_CONFIG 配置
# Check for the deprecated LOGGING_CONFIG
# Only use it if CATALINA_LOGGING_CONFIG is not set and LOGGING_CONFIG starts with "-D..."
if [ -z "$CATALINA_LOGGING_CONFIG" ]; then
  case $LOGGING_CONFIG in
    -D*) CATALINA_LOGGING_CONFIG="$LOGGING_CONFIG"
  esac
fi


# Set juli LogManager config file if it is present and an override has not been issued
if [ -z "$CATALINA_LOGGING_CONFIG" ]; then
  if [ -r "$CATALINA_BASE"/conf/logging.properties ]; then
    CATALINA_LOGGING_CONFIG="-Djava.util.logging.config.file=$CATALINA_BASE/conf/logging.properties"
  else
    # Bugzilla 45585
    CATALINA_LOGGING_CONFIG="-Dnop"
  fi
fi

if [ -z "$LOGGING_MANAGER" ]; then
  LOGGING_MANAGER="-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager"
fi

# Set UMASK unless it has been overridden
if [ -z "$UMASK" ]; then
    UMASK="0027"
fi
umask $UMASK


# Java 9 no longer supports the java.endorsed.dirs
# system property. Only try to use it if
# JAVA_ENDORSED_DIRS was explicitly set
# or CATALINA_HOME/endorsed exists.
ENDORSED_PROP=ignore.endorsed.dirs
if [ -n "$JAVA_ENDORSED_DIRS" ]; then
    ENDORSED_PROP=java.endorsed.dirs
fi
if [ -d "$CATALINA_HOME/endorsed" ]; then
    ENDORSED_PROP=java.endorsed.dirs
fi


# Make the umask available when using the org.apache.catalina.security.SecurityListener
JAVA_OPTS="$JAVA_OPTS -Dorg.apache.catalina.security.SecurityListener.UMASK=`umask`"

if [ -z "$USE_NOHUP" ]; then
    if $hpux; then
        USE_NOHUP="true"
    else
        USE_NOHUP="false"
    fi
fi
unset _NOHUP
if [ "$USE_NOHUP" = "true" ]; then
    _NOHUP="nohup"
fi

# 添加Tomcat需要的参数
# Add the JAVA 9 specific start-up parameters required by Tomcat
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.lang=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.io=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.util=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
JDK_JAVA_OPTIONS="$JDK_JAVA_OPTIONS --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED"
export JDK_JAVA_OPTIONS

# ----- Execute The Requested Command -----------------------------------------
# 执行相关指令
# Bugzilla 37848: only output this if we have a TTY

elif [ "$1" = "stop" ] ; then
	
	# 把参数stop去掉
  shift

  SLEEP=5
  if [ ! -z "$1" ]; then
    echo $1 | grep "[^0-9]" >/dev/null 2>&1
    if [ $? -gt 0 ]; then
      SLEEP=$1
      shift
    fi
  fi
  
  # 以上主要是判断停止语句执行到此时，设定几秒后再执行停止语句
  # 用来配合stop n
  

  FORCE=0
  if [ "$1" = "-force" ]; then
    shift
    FORCE=1
  fi
  # 如果参数中使用了-force，则FORCE=1

  if [ ! -z "$CATALINA_PID" ]; then
  # $CATALINA_PID 文件不是非空
    if [ -f "$CATALINA_PID" ]; then
      if [ -s "$CATALINA_PID" ]; then
        kill -0 `cat "$CATALINA_PID"` >/dev/null 2>&1
        # kill -0 pid 不发送任何信号，但是系统会进行错误检查
        if [ $? -gt 0 ]; then
          echo "PID file found but either no matching process was found or the current user does not have permission to stop the process. Stop aborted."
          exit 1
        fi
      else
        echo "PID file is empty and has been ignored."
      fi
    else
      echo "\$CATALINA_PID was set but the specified file does not exist. Is Tomcat running? Stop aborted."
      exit 1
    fi
  fi
	# 以上脚本是进行停止命令检错的，如果可能停止不了，则可以直接报错
	
  eval "\"$_RUNJAVA\"" $LOGGING_MANAGER "$JAVA_OPTS" \
    -D$ENDORSED_PROP="\"$JAVA_ENDORSED_DIRS\"" \
    -classpath "\"$CLASSPATH\"" \
    -Dcatalina.base="\"$CATALINA_BASE\"" \
    -Dcatalina.home="\"$CATALINA_HOME\"" \
    -Djava.io.tmpdir="\"$CATALINA_TMPDIR\"" \
    org.apache.catalina.startup.Bootstrap "$@" stop
	
  # stop failed. Shutdown port disabled? Try a normal kill.
  if [ $? != 0 ]; then
    if [ ! -z "$CATALINA_PID" ]; then
      echo "The stop command failed. Attempting to signal the process to stop through OS signal."
      kill -15 `cat "$CATALINA_PID"` >/dev/null 2>&1
    fi
  fi
	
	# 这个就是停止脚本的核心命令了
  if [ ! -z "$CATALINA_PID" ]; then
    if [ -f "$CATALINA_PID" ]; then
      while [ $SLEEP -ge 0 ]; do
        kill -0 `cat "$CATALINA_PID"` >/dev/null 2>&1
        if [ $? -gt 0 ]; then
          rm -f "$CATALINA_PID" >/dev/null 2>&1
          if [ $? != 0 ]; then
            if [ -w "$CATALINA_PID" ]; then
              cat /dev/null > "$CATALINA_PID"
              # If Tomcat has stopped don't try and force a stop with an empty PID file
              FORCE=0
            else
              echo "The PID file could not be removed or cleared."
            fi
          fi
          echo "Tomcat stopped."
          break
        fi
        if [ $SLEEP -gt 0 ]; then
          sleep 1
        fi
        if [ $SLEEP -eq 0 ]; then
          echo "Tomcat did not stop in time."
          if [ $FORCE -eq 0 ]; then
            echo "PID file was not removed."
          fi
          echo "To aid diagnostics a thread dump has been written to standard out."
          kill -3 `cat "$CATALINA_PID"`
        fi
        SLEEP=`expr $SLEEP - 1 `
      done
    fi
  fi
	# 上段语句主要是清空$CATALINA_PID
	
	# 值得注意的是，生产环境偶尔不加-force选项，tomcat有时无法停止下来
	# 如果参数带“-force”，则强制kill掉tomcat
  KILL_SLEEP_INTERVAL=5
  if [ $FORCE -eq 1 ]; then
    if [ -z "$CATALINA_PID" ]; then
      echo "Kill failed: \$CATALINA_PID not set"
    else
      if [ -f "$CATALINA_PID" ]; then
        PID=`cat "$CATALINA_PID"`
        echo "Killing Tomcat with the PID: $PID"
        kill -9 $PID
        while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
            kill -0 `cat "$CATALINA_PID"` >/dev/null 2>&1
            if [ $? -gt 0 ]; then
            		# 强制执行的核心命令
                rm -f "$CATALINA_PID" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ -w "$CATALINA_PID" ]; then
                        cat /dev/null > "$CATALINA_PID"
                    else
                        echo "The PID file could not be removed."
                    fi
                fi
                echo "The Tomcat process has been killed."
                break
            fi
            if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                sleep 1
            fi
            KILL_SLEEP_INTERVAL=`expr $KILL_SLEEP_INTERVAL - 1 `
        done
        if [ $KILL_SLEEP_INTERVAL -lt 0 ]; then
            echo "Tomcat has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
        fi
      fi
    fi
  fi
```

#### (3) 执行stop 指令 `org.apache.catalina.startup.Bootstrap "$@" stop`

```java
# main 方法中根据参数进行方法调用
...
else if (command.equals("stop")) {
        daemon.stopServer(args);
} 
...
  
# stopServer 方法
# this.catalinaDaemon.getClass() => org.apache.catalina.startup.Catalina
 public void stopServer(String[] arguments) throws Exception {
  Object[] param;
  Class[] paramTypes;
  if (arguments != null && arguments.length != 0) {
    paramTypes = new Class[]{arguments.getClass()};
    param = new Object[]{arguments};
  } else {
    paramTypes = null;
    param = null;
  }

  Method method = this.catalinaDaemon.getClass().getMethod("stopServer", paramTypes);
  method.invoke(this.catalinaDaemon, param);
}

# org.apache.catalina.startup.Catalina#stopServer()
public void stopServer(String[] arguments) {
    # 入参赋值
    if (arguments != null) {
      this.arguments(arguments);
    }
		
  	# 获取服务实例
    Server s = this.getServer();
    if (s == null) {
      # 创建汇编器
      Digester digester = this.createStopDigester();
      
      # 获取配置文件
      File file = this.configFile();

      Throwable var6;
      try {
        FileInputStream fis = new FileInputStream(file);
        var6 = null;

        try {
          InputSource is = new InputSource(file.toURI().toURL().toString());
          is.setByteStream(fis);
          digester.push(this);
          digester.parse(is);
        } catch (Throwable var62) {
          var6 = var62;
          throw var62;
        } finally {
          if (fis != null) {
            if (var6 != null) {
              try {
                fis.close();
              } catch (Throwable var61) {
                var6.addSuppressed(var61);
              }
            } else {
              fis.close();
            }
          }

        }
      } catch (Exception var71) {
        log.error("Catalina.stop: ", var71);
        System.exit(1);
      }
		
      s = this.getServer();
      if (s.getPort() > 0) {
        try {
          # 连接端口
          Socket socket = new Socket(s.getAddress(), s.getPort());
          Throwable var73 = null;

          try {
            # 获取输出流
            OutputStream stream = socket.getOutputStream();
            var6 = null;

            try {
              # 获取 SHUTDOWN 指令
              String shutdown = s.getShutdown();
							
              # 写指令
              for(int i = 0; i < shutdown.length(); ++i) {
                stream.write(shutdown.charAt(i));
              }

              stream.flush();
            } catch (Throwable var64) {
              var6 = var64;
              throw var64;
            } finally {
              if (stream != null) {
                if (var6 != null) {
                  try {
                    stream.close();
                  } catch (Throwable var60) {
                    var6.addSuppressed(var60);
                  }
                } else {
                  stream.close();
                }
              }

            }
          } catch (Throwable var66) {
            var73 = var66;
            throw var66;
          } finally {
            if (socket != null) {
              if (var73 != null) {
                try {
                  socket.close();
                } catch (Throwable var59) {
                  var73.addSuppressed(var59);
                }
              } else {
                socket.close();
              }
            }

          }
        } catch (ConnectException var68) {
          log.error(sm.getString("catalina.stopServer.connectException", new Object[]{s.getAddress(), String.valueOf(s.getPort())}));
          log.error("Catalina.stop: ", var68);
          System.exit(1);
        } catch (IOException var69) {
          log.error("Catalina.stop: ", var69);
          System.exit(1);
        }
      } else {
        log.error(sm.getString("catalina.stopServer"));
        System.exit(1);
      }

    } else {
      try {
        # 服务停止 + 销毁
        s.stop();
        s.destroy();
      } catch (LifecycleException var63) {
        log.error("Catalina.stop: ", var63);
      }

    }
  }
```



