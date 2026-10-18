package com.stayledger.web;

import com.stayledger.model.Charge;
import com.stayledger.model.HotelSettings;
import com.stayledger.model.Invoice;
import com.stayledger.model.MenuItem;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InvoiceServlet extends Support {
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            String raw = value(req, "id");
            if (raw.isEmpty()) {
                req.setAttribute("menuItems", repo.listFoodItems(false));
                req.getRequestDispatcher("/WEB-INF/views/invoice-form.jsp").forward(req, res);
                return;
            }
            Invoice invoice = repo.getInvoice(id(req));
            if (invoice == null) { res.sendError(404); return; }
            req.setAttribute("invoice", invoice);
            req.getRequestDispatcher("/WEB-INF/views/invoice-detail.jsp").forward(req, res);
        } catch (Exception ex) { fail(req, res, ex); }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            Invoice invoice = new Invoice();
            invoice.setInvoiceType("FOOD");
            invoice.setGuestName("Walk-in");
            invoice.setPhone("");
            invoice.setEmail("");
            invoice.setRoomNumber(value(req, "tableNumber"));
            invoice.setRoomType("Food");
            invoice.setGuestCount(1);
            Date today = new Date(System.currentTimeMillis());
            invoice.setCheckIn(today);
            invoice.setCheckOut(new Date(today.getTime() + 86400000L));
            invoice.setNights(1);
            invoice.setNightlyRate(BigDecimal.ZERO);
            invoice.setRoomSubtotal(BigDecimal.ZERO);

            String[] itemIds = req.getParameterValues("itemId");
            String[] quantities = req.getParameterValues("itemQuantity");
            if (itemIds == null || quantities == null) throw new IllegalArgumentException("Add at least one available menu item.");
            if (itemIds.length > 50) throw new IllegalArgumentException("An invoice can contain up to 50 food items.");
            Map<Long, MenuItem> menu = new HashMap<Long, MenuItem>();
            for (MenuItem item : repo.listFoodItems(false)) menu.put(item.getId(), item);
            List<Charge> items = new ArrayList<Charge>();
            BigDecimal foodTotal = BigDecimal.ZERO;
            for (int n = 0; n < itemIds.length; n++) {
                String idText = itemIds[n].trim();
                String qtyText = n < quantities.length ? quantities[n].trim() : "";
                if (idText.isEmpty()) continue;
                long itemId = Long.parseLong(idText);
                MenuItem menuItem = menu.get(itemId);
                if (menuItem == null) throw new IllegalArgumentException("A selected menu item is no longer available. Refresh and try again.");
                int qty = Integer.parseInt(qtyText);
                BigDecimal unitPrice = menuItem.getPrice();
                if (qty < 1 || qty > 999 || unitPrice.signum() <= 0) throw new IllegalArgumentException("Food quantity must be 1–999 and menu price must be greater than zero.");
                BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
                items.add(new Charge(0, menuItem.getName(), BigDecimal.valueOf(qty), unitPrice, amount, menuItem.getId()));
                foodTotal = foodTotal.add(amount);
            }
            if (items.isEmpty()) throw new IllegalArgumentException("Add at least one food item.");
            invoice.setExtrasSubtotal(foodTotal);
            invoice.setSubtotal(foodTotal);
            invoice.setDiscount(decimal(req, "discount"));
            invoice.setTaxRate(decimal(req, "taxRate"));
            if (invoice.getDiscount().signum() < 0 || invoice.getDiscount().compareTo(foodTotal) > 0 || invoice.getTaxRate().signum() < 0 || invoice.getTaxRate().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Check the discount and tax values.");
            }
            BigDecimal taxable = foodTotal.subtract(invoice.getDiscount());
            invoice.setTax(taxable.multiply(invoice.getTaxRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            invoice.setTotal(taxable.add(invoice.getTax()));
            invoice.setPaid(decimal(req, "amountPaid"));
            if (invoice.getPaid().signum() < 0 || invoice.getPaid().compareTo(invoice.getTotal()) > 0) throw new IllegalArgumentException("Amount paid must be between zero and the invoice total.");
            invoice.setPaymentMethod(value(req, "paymentMethod"));

            long newId = repo.createInvoice(invoice, items, repo.getSettings());
            res.sendRedirect(req.getContextPath() + "/invoice?id=" + newId);
        } catch (Exception ex) { fail(req, res, ex); }
    }
}
