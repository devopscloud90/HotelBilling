package com.stayledger.web;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
public class PaymentServlet extends Support {
    protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {
        try {long id=id(req); String method=value(req,"paymentMethod"); if(!"Cash".equals(method)&&!"Card".equals(method)&&!"Bank transfer".equals(method))throw new IllegalArgumentException("Choose a valid payment method."); repo.recordPayment(id,decimal(req,"amount"),method); res.sendRedirect(req.getContextPath()+"/invoice?id="+id);}
        catch(Exception ex){fail(req,res,ex);}
    }
}
