package com.stayledger.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class AuthenticationFilter implements Filter {
    public void init(FilterConfig config) { }
    public void destroy() { }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if ("/login".equals(path) || "/setup".equals(path) || path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".ico")) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("staffUsername") == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        String csrfToken = (String) session.getAttribute("csrfToken");
        if (csrfToken == null) {
            csrfToken = Support.newCsrfToken();
            session.setAttribute("csrfToken", csrfToken);
        }
        req.setAttribute("csrfToken", csrfToken);
        if ("POST".equalsIgnoreCase(req.getMethod())) {
            String submitted = req.getParameter("_csrf");
            if (submitted == null || !java.security.MessageDigest.isEqual(csrfToken.getBytes(java.nio.charset.StandardCharsets.UTF_8), submitted.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        chain.doFilter(request, response);
    }
}
