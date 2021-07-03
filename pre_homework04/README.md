# 预习第四周

### ANT风格语义

- 什么是ANT风格语义？

  - 通配符上来看，有三种

    - | 通配符 | 说明                    |
      | ------ | ----------------------- |
      | ？     | 匹配任何单字符          |
      | *      | 匹配0或者任意数量的字符 |
      | **     | 匹配0或者更多的目录     |

  - 示例

    - | URL路径             | 说明                                                         |
      | ------------------- | ------------------------------------------------------------ |
      | /app/*.x            | 匹配所有在app路径下的.x文件                                  |
      | /app/p?ttern        | 匹配/app/pattern和/app/pXttern，但不包括/app/pttern          |
      | /**/example         | 匹配/app/example，/app/foo/example，和/example               |
      | /app/**/dir/file. * | 匹配/app/dir/file.jsp,/app/foo/dir/file.html,/app/foo/bar/dir/file/pdf,和/app/dir/file.java |
      | /**/ *.jsp          | 匹配任何的.jsp文件                                           |

    - 

## 作业：

扩展org.geektimes.http.server.jdk.servlet.URLPatternsMatcher接口，实现 ANT 风格语义，可以参考 Spring org.springframework.security.web.util.matcher.AntPathRequestMatcher
