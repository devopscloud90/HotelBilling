package com.stayledger.web;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
public class DashboardServlet extends Support {
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {
        try { req.setAttribute("invoices",repo.listInvoices("","")); req.setAttribute("revenue",repo.getRevenueToday()); req.setAttribute("outstanding",repo.getOutstanding()); req.setAttribute("invoiceCount",repo.countInvoices()); req.setAttribute("hotelSettings",repo.getSettings()); req.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(req,res); }
        catch(Exception ex){fail(req,res,ex);}
    }
}
