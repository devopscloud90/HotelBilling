package com.stayledger.web;

import com.stayledger.db.BillingRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class Support extends HttpServlet {
    protected final BillingRepository repo = new BillingRepository();
    public static String newCsrfToken() {
        byte[] token = new byte[32];
        new java.security.SecureRandom().nextBytes(token);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
    protected String value(HttpServletRequest req, String name) { String v=req.getParameter(name); return v==null?"":v.trim(); }
    protected BigDecimal decimal(HttpServletRequest req, String name) {
        String v=value(req,name); if(v.isEmpty()) return BigDecimal.ZERO;
        try { return new BigDecimal(v); } catch(NumberFormatException ex) { throw new IllegalArgumentException("Enter a valid number for " + name.replace('_',' ') + "."); }
    }
    protected long id(HttpServletRequest req) { try { return Long.parseLong(value(req,"id")); } catch(Exception ex) { throw new IllegalArgumentException("A valid invoice ID is required."); } }
    protected void fail(HttpServletRequest req,HttpServletResponse res,Exception ex) throws ServletException,IOException {
        getServletContext().log("Hotel billing request failed",ex);
        req.setAttribute("errorMessage", ex instanceof IllegalArgumentException ? ex.getMessage() : "The request could not be completed. Check the database connection and try again.");
        try { req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req,res); } catch(ServletException forwardError) { throw forwardError; }
    }
    protected void dbError(HttpServletRequest req,HttpServletResponse res,SQLException ex) throws ServletException,IOException { fail(req,res,ex); }
}
