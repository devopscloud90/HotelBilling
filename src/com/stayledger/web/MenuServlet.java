package com.stayledger.web;

import com.stayledger.model.MenuItem;
import java.io.IOException;
import java.math.BigDecimal;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MenuServlet extends Support {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            req.setAttribute("menuItems", repo.listFoodItems(true));
            req.getRequestDispatcher("/WEB-INF/views/menu.jsp").forward(req, res);
        } catch (Exception ex) { fail(req, res, ex); }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            String action = value(req, "action");
            String name = value(req, "name");
            BigDecimal price = decimal(req, "price");
            if (name.isEmpty() || name.length() > 80) throw new IllegalArgumentException("Item name must be between 1 and 80 characters.");
            if (price.signum() <= 0 || price.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("Enter a price greater than zero with up to two decimal places.");
            if ("add".equals(action)) {
                repo.addFoodItem(name, price);
            } else if ("update".equals(action)) {
                long id = Long.parseLong(value(req, "itemId"));
                String active = "N".equals(value(req, "active")) ? "N" : "Y";
                repo.updateFoodItem(id, name, price, active);
            } else {
                throw new IllegalArgumentException("Choose a valid menu action.");
            }
            res.sendRedirect(req.getContextPath() + "/menu?saved=1");
        } catch (Exception ex) { fail(req, res, ex); }
    }
}
