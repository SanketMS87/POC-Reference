package com.rukamini.mail.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public final class ResponseCorsFilter implements Filter {

    static final Pattern HOST_ALLOWED_PATTERN =
            Pattern.compile(
                    "(.*\\.swissre\\.com)|(.*\\.swissreapps(-np)?\\.com)|.*localhost",
                    Pattern.CASE_INSENSITIVE);
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseCorsFilter.class);

    @Override
    public void doFilter(
            final ServletRequest servletRequest,
            final ServletResponse servletResponse,
            final FilterChain filterChain)
            throws IOException, ServletException {
        final String methodName = "doFilter";
        LOGGER.debug("entering {}", methodName);

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        // LOGGER.info("Origin : "+origin);

        if (null != origin && shouldAddHeaders(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Request-Method", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
        }

        filterChain.doFilter(servletRequest, servletResponse);
        LOGGER.debug("exiting {}", methodName);
    }

    boolean shouldAddHeaders(String origin) {
        URL originUrl = getOriginUrl(origin);
        return (null != originUrl) && HOST_ALLOWED_PATTERN.matcher(originUrl.getHost()).matches();
    }

    private URL getOriginUrl(String origin) {
        URL originUrl = null;
        try {
            originUrl = new URL(origin);
        } catch (MalformedURLException ex) {
            LOGGER.warn("Invalid origin URL: {}", origin, ex);
        }
        return originUrl;
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        // init filter logic
    }

    @Override
    public void destroy() {
        // NOOP
    }
}
