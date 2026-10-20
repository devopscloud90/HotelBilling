package com.stayledger.web;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginServlet extends Support {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            if (!repo.hasStaffUsers()) { res.sendRedirect(req.getContextPath() + "/setup"); return; }
            if (req.getSession(false) != null && req.getSession(false).getAttribute("staffUsername") != null) {
                res.sendRedirect(req.getContextPath() + "/dashboard"); return;
            }
            req.setAttribute("pageTitle", "Staff login");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
        } catch (Exception ex) { fail(req, res, ex); }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            String username = value(req, "username").toLowerCase(Locale.ENGLISH);
            char[] password = req.getParameter("password") == null ? new char[0] : req.getParameter("password").toCharArray();
            byte[] salt = repo.getStaffPasswordSalt(username);
            byte[] hash = PasswordHasher.hash(password, salt == null ? new byte[16] : salt);
            boolean valid = salt != null && repo.verifyStaffUser(username, hash);
            java.util.Arrays.fill(password, '\0');
            if (!valid) {
                req.setAttribute("pageTitle", "Staff login");
                req.setAttribute("errorMessage", "Username or password is incorrect.");
                req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, res);
                return;
            }
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession session = req.getSession(true);
            session.setAttribute("staffUsername", username);
            session.setAttribute("csrfToken", Support.newCsrfToken());
            res.sendRedirect(req.getContextPath() + "/dashboard");
        } catch (Exception ex) { fail(req, res, ex); }
    }
}
