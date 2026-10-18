package com.stayledger.web;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
public class InvoicesServlet extends Support {
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {
        try { String q=value(req,"q"), status=value(req,"status"); req.setAttribute("invoices",repo.listInvoices(q,status)); req.setAttribute("q",q); req.setAttribute("status",status); req.getRequestDispatcher("/WEB-INF/views/invoices.jsp").forward(req,res); }
        catch(Exception ex){fail(req,res,ex);}
    }
}
