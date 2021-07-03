package com.zachary.pre_homework02_02;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 全手打 抄过来的 非复制
 */
public class ExtendURLPatternsMatcher implements URLPatternsMatcher{
    private static final Log logger = LogFactory.getLog(ExtendURLPatternsMatcher.class);
    private static final String MATCH_ALL = "/**";
    private final Matcher matcher;
    private final String pattern;
    private final HttpMethod httpMethod;
    private final boolean caseSensitive;
    private final UrlPathHelper urlPathHelper;

    @Override
    public boolean matches(Collection<String> urlPatterns, String requestURI) {
        return false;
    }

    public ExtendURLPatternsMatcher (String pattern, String httpMethod, boolean caseSensitive, UrlPathHelper urlPathHelper) {
        Assert.hasText(pattern , "Pattern cannot be null or empty");
        this.caseSensitive = caseSensitive;
        if (!pattern.equals("/**") && !pattern.equals("**")) {
            if (pattern.endsWith("/**") && pattern.indexOf(63) == -1  && pattern.indexOf(123) == -1 && pattern.indexOf(125) == -1 && pattern.indexOf("*") == pattern.length() - 2) {
                this.matcher = new SubpathMatcher(pattern.substring(0, pattern.length() - 3), caseSensitive);
            } else {
                this.matcher = new SpringAntMatcher(caseSensitive, pattern);
            }
        } else {
            pattern = "/**";
            this.matcher = null;
        }
        this.pattern = pattern;
        this.httpMethod = StringUtils.hasText(httpMethod) ? HttpMethod.valueOf(httpMethod) : null;
        this.urlPathHelper = urlPathHelper;
    }

    public boolean matches (HttpServletRequest request) {
        if(this.httpMethod != null && StringUtils.hasText(request.getMethod()) && this.httpMethod != valueOf(request.getMethod())) {
            return false;
        } else if (this.pattern.equals("/**")) {
            return true;
        } else {
            String url = this.getRequestPath(request);
            return this.matcher.matches(url);
        }
    }

    public RequestMatcher.MatchResult matcher (HttpServletRequest request) {
        if (this.matcher != null && this.matches(request)) {
            String url = this.getRequestPath(request);
            return RequestMatcher.MatchResult.match(this.matcher.extractUrlTemplateVariables(url));
        } else {
            return RequestMatcher.MatchResult.notMatch();
        }
    }

    private String getRequestPath (HttpServletRequest request) {
        if(this.urlPathHelper != null) {
            return this.urlPathHelper.getPathWithinApplication(request);
        } else {
            String url = request.getServletPath();
            String pathInfo = request.getPathInfo();
            if (pathInfo != null) {
                url = StringUtils.hasLength(url) ? url + pathInfo : pathInfo;
            }
            return url;
        }
    }

    public String getPattern() {
        return this.pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtendURLPatternsMatcher that = (ExtendURLPatternsMatcher) o;
        return caseSensitive == that.caseSensitive && Objects.equals(matcher, that.matcher) && Objects.equals(pattern, that.pattern) && httpMethod == that.httpMethod && Objects.equals(urlPathHelper, that.urlPathHelper);
    }

    @Override
    public int hashCode () {
        int result = this.pattern != null ? this.hashCode() : 0;
        result = 31 * result + (this.httpMethod != null ? this.httpMethod.hashCode() : 0);
        result = 31 * result + (this.caseSensitive ? 1231 : 1237);
        return result;
    }

    @Override
    public String toString () {
        StringBuilder sb = new StringBuilder();
        sb.append("Ant [patter=']").append((this.pattern)).append("'");
        if (this.httpMethod != null) {
            sb.append(", ").append(this.httpMethod);
        }
        sb.append("]");
        return sb.toString();
    }

    private static HttpMethod valueOf (String method) {
        try {
            return HttpMethod.valueOf(method);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    private static class SubpathMatcher implements Matcher {
        private final String subpath;
        private final int length;
        private final boolean caseSensitive;

        public SubpathMatcher(String subpath,  boolean caseSensitive) {
            assert !subpath.contains("*");

            this.subpath = caseSensitive ? subpath : subpath.toLowerCase();
            this.length = subpath.length();
            this.caseSensitive = caseSensitive;
        }

        @Override
        public boolean matches(String path) {
            if(!this.caseSensitive) {
                path = path.toLowerCase();
            }
            return path.startsWith(this.subpath) && (path.length() == this.length || path.charAt(this.length) == '/');
        }

        @Override
        public Map<String, String> extractUrlTemplateVariables(String var) {
            return Collections.emptyMap();
        }
    }

    private static class SpringAntMatcher implements Matcher {
        private final AntPathMatcher antPathMatcher;
        private final String pattern;

        public SpringAntMatcher(boolean caseSensitive, String pattern) {
            this.antPathMatcher = createMatcher(caseSensitive);
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String path) {
            return this.antPathMatcher.match(this.pattern, path);
        }

        @Override
        public Map<String, String> extractUrlTemplateVariables(String path) {
            return this.antPathMatcher.extractUriTemplateVariables(this.pattern, path);
        }

        private static AntPathMatcher createMatcher (boolean caseSensitive) {
            AntPathMatcher matcher = new AntPathMatcher();
            matcher.setTrimTokens(false);
            matcher.setCaseSensitive(caseSensitive);
            return matcher;
        }
    }

    private interface Matcher {
        boolean matches(String var);

        Map<String, String> extractUrlTemplateVariables(String var);
    }
}
