package com.stayledger.db;

import com.stayledger.model.Charge;
import com.stayledger.model.HotelSettings;
import com.stayledger.model.Invoice;
import com.stayledger.model.MenuItem;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class BillingRepository {
    private static final String SUMMARY_SQL =
        "select i.id, i.invoice_number, i.invoice_type, i.guest_name, i.guest_phone, i.guest_email, " +
        "i.room_number, i.room_type, i.check_in, i.check_out, i.guest_count, i.nightly_rate, " +
        "i.nights, i.room_subtotal, i.extras_subtotal, i.subtotal, i.discount_amount, " +
        "i.tax_rate, i.tax_amount, i.total_amount, i.issued_on, i.created_at, " +
        "i.property_name, i.property_address, i.property_phone, i.property_email, " +
        "nvl(p.paid,0) paid, i.total_amount-nvl(p.paid,0) balance, p.last_method payment_method, " +
        "case when i.total_amount-nvl(p.paid,0)<=0 then 'Paid' " +
        "when nvl(p.paid,0)>0 then 'Partially paid' else 'Unpaid' end payment_status " +
        "from hotel_invoices i left join (select invoice_id, sum(amount) paid, " +
        "max(payment_method) last_method from hotel_payments group by invoice_id) p on p.invoice_id=i.id";

    public boolean hasStaffUsers() throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select count(*) from hotel_staff_users"); ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public boolean createFirstStaffUser(String username, byte[] salt, byte[] hash) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("select id from hotel_auth_lock where id=1 for update"); ResultSet rs = ps.executeQuery()) { rs.next(); }
                if (hasStaffUsers(c)) { c.rollback(); return false; }
                try (PreparedStatement ps = c.prepareStatement("insert into hotel_staff_users (username, password_salt, password_hash) values (?, ?, ?)")) {
                    ps.setString(1, username); ps.setString(2, hex(salt)); ps.setString(3, hex(hash)); ps.executeUpdate();
                }
                c.commit();
                return true;
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException) throw (SQLException) ex;
                throw new SQLException("Unable to create the administrator account.", ex);
            }
        }
    }

    private boolean hasStaffUsers(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select count(*) from hotel_staff_users"); ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public boolean verifyStaffUser(String username, byte[] suppliedHash) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select password_hash from hotel_staff_users where username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                byte[] storedHash = unhex(rs.getString(1));
                return java.security.MessageDigest.isEqual(storedHash, suppliedHash);
            }
        }
    }

    public byte[] getStaffPasswordSalt(String username) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select password_salt from hotel_staff_users where username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? unhex(rs.getString(1)) : null; }
        }
    }

    private String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b & 0xff));
        return out.toString();
    }

    private byte[] unhex(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return out;
    }

    public HotelSettings getSettings() throws SQLException {
        HotelSettings settings = new HotelSettings();
        try (Connection c = Database.open();
             PreparedStatement ps = c.prepareStatement("select property_name, property_address, property_phone, property_email from hotel_settings where id=1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                settings.setPropertyName(rs.getString(1));
                settings.setPropertyAddress(rs.getString(2));
                settings.setPropertyPhone(rs.getString(3));
                settings.setPropertyEmail(rs.getString(4));
            }
        }
        return settings;
    }

    public List<MenuItem> listFoodItems(boolean includeInactive) throws SQLException {
        String sql = "select id, item_name, unit_price, active from hotel_food_items " +
            (includeInactive ? "" : "where active='Y' ") + "order by lower(item_name)";
        List<MenuItem> items = new ArrayList<MenuItem>();
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) items.add(new MenuItem(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)));
        }
        return items;
    }

    public MenuItem getFoodItem(long id) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select id, item_name, unit_price, active from hotel_food_items where id=? and active='Y'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? new MenuItem(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4)) : null; }
        }
    }

    public void addFoodItem(String name, BigDecimal price) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("insert into hotel_food_items (id, item_name, unit_price, active) values (food_item_seq.nextval, ?, ?, 'Y')")) {
            ps.setString(1, name); ps.setBigDecimal(2, price); ps.executeUpdate();
        }
    }

    public void updateFoodItem(long id, String name, BigDecimal price, String active) throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("update hotel_food_items set item_name=?, unit_price=?, active=? where id=?")) {
            ps.setString(1, name); ps.setBigDecimal(2, price); ps.setString(3, active); ps.setLong(4, id);
            if (ps.executeUpdate() != 1) throw new SQLException("Menu item was not found.");
        }
    }

    public void saveSettings(HotelSettings settings) throws SQLException {
        String sql = "merge into hotel_settings s using (select 1 id from dual) x on (s.id=x.id) " +
            "when matched then update set property_name=?, property_address=?, property_phone=?, property_email=?, updated_at=systimestamp " +
            "when not matched then insert (id, property_name, property_address, property_phone, property_email) values (1, ?, ?, ?, ?)";
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindSettings(ps, settings, 1);
            bindSettings(ps, settings, 5);
            ps.executeUpdate();
        }
    }

    private void bindSettings(PreparedStatement ps, HotelSettings settings, int start) throws SQLException {
        ps.setString(start, settings.getPropertyName());
        setNullableString(ps, start + 1, settings.getPropertyAddress());
        setNullableString(ps, start + 2, settings.getPropertyPhone());
        setNullableString(ps, start + 3, settings.getPropertyEmail());
    }

    public List<Invoice> listInvoices(String search, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("select * from (").append(SUMMARY_SQL).append(") where 1=1");
        List<String> values = new ArrayList<String>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" and (lower(guest_name) like ? or lower(invoice_number) like ? or lower(nvl(room_number,'')) like ?)");
            String pattern = "%" + search.trim().toLowerCase() + "%";
            values.add(pattern); values.add(pattern); values.add(pattern);
        }
        if ("Paid".equals(status) || "Unpaid".equals(status) || "Partially paid".equals(status)) {
            sql.append(" and payment_status=?");
            values.add(status);
        }
        sql.append(" order by created_at desc");
        List<Invoice> results = new ArrayList<Invoice>();
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) ps.setString(i + 1, values.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(readInvoice(rs));
            }
        }
        return results;
    }

    public Invoice getInvoice(long id) throws SQLException {
        Invoice invoice = null;
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select * from (" + SUMMARY_SQL + ") where id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) invoice = readInvoice(rs);
            }
        }
        if (invoice != null) invoice.setCharges(getCharges(id));
        return invoice;
    }

    public List<Charge> getCharges(long invoiceId) throws SQLException {
        List<Charge> charges = new ArrayList<Charge>();
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select id, description, quantity, unit_price, amount from hotel_invoice_charges where invoice_id=? order by sort_order, id")) {
            ps.setLong(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) charges.add(new Charge(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5)));
            }
        }
        return charges;
    }

    public long createInvoice(Invoice invoice, List<Charge> charges, HotelSettings settings) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("select invoice_seq.nextval from dual")) {
                    rs.next(); id = rs.getLong(1);
                }
                String number = "ST-" + new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) + "-" + String.format("%04d", id);
                invoice.setId(id);
                invoice.setNumber(number);
                insertInvoice(c, invoice, settings);
                int order = 0;
                for (Charge charge : charges) insertCharge(c, id, charge, order++);
                if (invoice.getPaid() != null && invoice.getPaid().signum() > 0) {
                    insertPayment(c, id, invoice.getPaid(), invoice.getPaymentMethod());
                }
                c.commit();
                return id;
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException) throw (SQLException) ex;
                throw new SQLException("Unable to save the invoice.", ex);
            }
        }
    }

    private void insertInvoice(Connection c, Invoice i, HotelSettings settings) throws SQLException {
        String sql = "insert into hotel_invoices (id, invoice_number, invoice_type, guest_name, guest_phone, guest_email, room_number, room_type, " +
            "check_in, check_out, guest_count, nightly_rate, nights, room_subtotal, extras_subtotal, subtotal, discount_amount, " +
            "tax_rate, tax_amount, total_amount, property_name, property_address, property_phone, property_email) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, i.getId()); ps.setString(2, i.getNumber()); ps.setString(3, i.getInvoiceType()); ps.setString(4, i.getGuestName());
            setNullableString(ps, 5, i.getPhone()); setNullableString(ps, 6, i.getEmail()); setNullableString(ps, 7, i.getRoomNumber());
            ps.setString(8, i.getRoomType()); ps.setDate(9, i.getCheckIn()); ps.setDate(10, i.getCheckOut()); ps.setInt(11, i.getGuestCount());
            ps.setBigDecimal(12, i.getNightlyRate()); ps.setInt(13, i.getNights()); ps.setBigDecimal(14, i.getRoomSubtotal());
            ps.setBigDecimal(15, i.getExtrasSubtotal()); ps.setBigDecimal(16, i.getSubtotal()); ps.setBigDecimal(17, i.getDiscount());
            ps.setBigDecimal(18, i.getTaxRate()); ps.setBigDecimal(19, i.getTax()); ps.setBigDecimal(20, i.getTotal());
            ps.setString(21, settings.getPropertyName().trim().isEmpty() ? "Hotel" : settings.getPropertyName());
            setNullableString(ps, 22, settings.getPropertyAddress()); setNullableString(ps, 23, settings.getPropertyPhone()); setNullableString(ps, 24, settings.getPropertyEmail());
            ps.executeUpdate();
        }
    }

    private void insertCharge(Connection c, long invoiceId, Charge charge, int order) throws SQLException {
        String sql = "insert into hotel_invoice_charges (id, invoice_id, menu_item_id, description, quantity, unit_price, amount, sort_order) values (invoice_charge_seq.nextval, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, invoiceId);
            if (charge.getMenuItemId() > 0) ps.setLong(2, charge.getMenuItemId()); else ps.setNull(2, Types.NUMERIC);
            ps.setString(3, charge.getDescription()); ps.setBigDecimal(4, charge.getQuantity()); ps.setBigDecimal(5, charge.getUnitPrice()); ps.setBigDecimal(6, charge.getAmount()); ps.setInt(7, order); ps.executeUpdate();
        }
    }

    public void recordPayment(long invoiceId, BigDecimal amount, String method) throws SQLException {
        try (Connection c = Database.open()) {
            c.setAutoCommit(false);
            try {
                BigDecimal total;
                try (PreparedStatement ps = c.prepareStatement("select total_amount from hotel_invoices where id=? for update")) {
                    ps.setLong(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Invoice was not found.");
                        total = rs.getBigDecimal(1);
                    }
                }
                BigDecimal paid = BigDecimal.ZERO;
                try (PreparedStatement ps = c.prepareStatement("select nvl(sum(amount),0) from hotel_payments where invoice_id=?")) {
                    ps.setLong(1, invoiceId);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) paid = rs.getBigDecimal(1); }
                }
                if (amount.signum() <= 0 || amount.compareTo(total.subtract(paid)) > 0) throw new SQLException("Payment must be greater than zero and no more than the balance due.");
                insertPayment(c, invoiceId, amount, method);
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                if (ex instanceof SQLException) throw (SQLException) ex;
                throw new SQLException("Unable to record the payment.", ex);
            }
        }
    }

    private void insertPayment(Connection c, long invoiceId, BigDecimal amount, String method) throws SQLException {
        String sql = "insert into hotel_payments (id, invoice_id, amount, payment_method) values (payment_seq.nextval, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, invoiceId); ps.setBigDecimal(2, amount); ps.setString(3, method); ps.executeUpdate();
        }
    }

    public BigDecimal getRevenueToday() throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select nvl(sum(amount),0) from hotel_payments where trunc(paid_on)=trunc(sysdate)"); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    public BigDecimal getOutstanding() throws SQLException {
        String sql = "select nvl(sum(greatest(i.total_amount-nvl(p.paid,0),0)),0) from hotel_invoices i left join " +
            "(select invoice_id, sum(amount) paid from hotel_payments group by invoice_id) p on p.invoice_id=i.id";
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    public int countInvoices() throws SQLException {
        try (Connection c = Database.open(); PreparedStatement ps = c.prepareStatement("select count(*) from hotel_invoices"); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Invoice readInvoice(ResultSet rs) throws SQLException {
        Invoice i = new Invoice();
        i.setId(rs.getLong("id")); i.setNumber(rs.getString("invoice_number")); i.setInvoiceType(rs.getString("invoice_type")); i.setGuestName(rs.getString("guest_name"));
        i.setPhone(rs.getString("guest_phone")); i.setEmail(rs.getString("guest_email")); i.setRoomNumber(rs.getString("room_number"));
        i.setRoomType(rs.getString("room_type")); i.setCheckIn(rs.getDate("check_in")); i.setCheckOut(rs.getDate("check_out"));
        i.setGuestCount(rs.getInt("guest_count")); i.setNightlyRate(rs.getBigDecimal("nightly_rate")); i.setNights(rs.getInt("nights"));
        i.setRoomSubtotal(rs.getBigDecimal("room_subtotal")); i.setExtrasSubtotal(rs.getBigDecimal("extras_subtotal"));
        i.setSubtotal(rs.getBigDecimal("subtotal")); i.setDiscount(rs.getBigDecimal("discount_amount")); i.setTaxRate(rs.getBigDecimal("tax_rate"));
        i.setTax(rs.getBigDecimal("tax_amount")); i.setTotal(rs.getBigDecimal("total_amount")); i.setPaid(rs.getBigDecimal("paid"));
        i.setBalance(rs.getBigDecimal("balance")); i.setPaymentMethod(rs.getString("payment_method")); i.setIssuedOn(rs.getDate("issued_on"));
        i.setCreatedAt(rs.getTimestamp("created_at")); i.setPropertyName(rs.getString("property_name")); i.setPropertyAddress(rs.getString("property_address"));
        i.setPropertyPhone(rs.getString("property_phone")); i.setPropertyEmail(rs.getString("property_email"));
        return i;
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) ps.setNull(index, Types.VARCHAR); else ps.setString(index, value.trim());
    }
}
