package com.stayledger.web;
import com.stayledger.model.HotelSettings;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
public class SettingsServlet extends Support {
    protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {try{req.setAttribute("settings",repo.getSettings());req.getRequestDispatcher("/WEB-INF/views/settings.jsp").forward(req,res);}catch(Exception ex){fail(req,res,ex);}}
    protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException {try{HotelSettings s=new HotelSettings();s.setPropertyName(value(req,"propertyName"));s.setPropertyAddress(value(req,"propertyAddress"));s.setPropertyPhone(value(req,"propertyPhone"));s.setPropertyEmail(value(req,"propertyEmail"));if(s.getPropertyName().isEmpty())throw new IllegalArgumentException("Hotel name is required.");repo.saveSettings(s);res.sendRedirect(req.getContextPath()+"/settings?saved=1");}catch(Exception ex){fail(req,res,ex);}}
}
