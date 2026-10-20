package com.stayledger.web;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SetupServlet extends Support {
    private boolean isLocalRequest(HttpServletRequest req) {
        String address = req.getRemoteAddr();
        return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address) || "::ffff:127.0.0.1".equals(address);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!isLocalRequest(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        try {
            if (repo.hasStaffUsers()) { res.sendRedirect(req.getContextPath() + "/login"); return; }
            req.setAttribute("pageTitle", "Create administrator login");
            req.getRequestDispatcher("/WEB-INF/views/setup.jsp").forward(req, res);
        } catch (Exception ex) { fail(req, res, ex); }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!isLocalRequest(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }
        char[] password = req.getParameter("password") == null ? new char[0] : req.getParameter("password").toCharArray();
        char[] confirmation = req.getParameter("confirmPassword") == null ? new char[0] : req.getParameter("confirmPassword").toCharArray();
        try {
            String username = value(req, "username").toLowerCase(Locale.ENGLISH);
            if (!username.matches("[a-z0-9._@-]{3,80}")) throw new IllegalArgumentException("Username must be 3–80 characters and use letters, numbers, dot, underscore, @, or hyphen.");
            if (password.length < 12) throw new IllegalArgumentException("Use a password with at least 12 characters.");
            if (!samePassword(password, confirmation)) {
                throw new IllegalArgumentException("The passwords do not match.");
            }
            byte[] salt = PasswordHasher.newSalt();
            byte[] hash = PasswordHasher.hash(password, salt);
            if (!repo.createFirstStaffUser(username, salt, hash)) { res.sendRedirect(req.getContextPath() + "/login"); return; }
            javax.servlet.http.HttpSession session = req.getSession(true);
            session.setAttribute("staffUsername", username);
            session.setAttribute("csrfToken", Support.newCsrfToken());
            res.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (IllegalArgumentException ex) {
            req.setAttribute("pageTitle", "Create administrator login");
            req.setAttribute("errorMessage", ex.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/setup.jsp").forward(req, res);
        } catch (Exception ex) { fail(req, res, ex); }
        finally {
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
        }
    }

    private boolean samePassword(char[] first, char[] second) {
        int difference = first.length ^ second.length;
        for (int i = 0; i < Math.max(first.length, second.length); i++) {
            char a = i < first.length ? first[i] : 0;
            char b = i < second.length ? second[i] : 0;
            difference |= a ^ b;
        }
        return difference == 0;
    }
}
