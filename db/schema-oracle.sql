-- Run this once in the Oracle XE database used by HotelBilling.
CREATE SEQUENCE invoice_seq START WITH 1001 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE invoice_charge_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE payment_seq START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE food_item_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE hotel_settings (
  id NUMBER(1) PRIMARY KEY CHECK (id = 1),
  property_name VARCHAR2(120) NOT NULL,
  property_address VARCHAR2(400),
  property_phone VARCHAR2(30),
  property_email VARCHAR2(160),
  updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE hotel_food_items (
  id NUMBER(12) PRIMARY KEY,
  item_name VARCHAR2(80) NOT NULL UNIQUE,
  unit_price NUMBER(12,2) NOT NULL CHECK (unit_price > 0),
  active CHAR(1) DEFAULT 'Y' NOT NULL CHECK (active IN ('Y','N')),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE TABLE hotel_invoices (
  id NUMBER(12) PRIMARY KEY,
  invoice_number VARCHAR2(24) NOT NULL UNIQUE,
  invoice_type VARCHAR2(10) DEFAULT 'ROOM' NOT NULL,
  guest_name VARCHAR2(120) NOT NULL,
  guest_phone VARCHAR2(30),
  guest_email VARCHAR2(160),
  room_number VARCHAR2(20),
  room_type VARCHAR2(30) NOT NULL,
  check_in DATE NOT NULL,
  check_out DATE NOT NULL,
  guest_count NUMBER(3) DEFAULT 1 NOT NULL,
  nightly_rate NUMBER(12,2) NOT NULL,
  nights NUMBER(5) NOT NULL,
  room_subtotal NUMBER(12,2) NOT NULL,
  extras_subtotal NUMBER(12,2) NOT NULL,
  subtotal NUMBER(12,2) NOT NULL,
  discount_amount NUMBER(12,2) DEFAULT 0 NOT NULL,
  tax_rate NUMBER(5,2) DEFAULT 0 NOT NULL,
  tax_amount NUMBER(12,2) DEFAULT 0 NOT NULL,
  total_amount NUMBER(12,2) NOT NULL,
  issued_on DATE DEFAULT SYSDATE NOT NULL,
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  property_name VARCHAR2(120) NOT NULL,
  property_address VARCHAR2(400),
  property_phone VARCHAR2(30),
  property_email VARCHAR2(160),
  CONSTRAINT hotel_invoice_dates_ck CHECK (check_out > check_in),
  CONSTRAINT hotel_invoice_amounts_ck CHECK (nightly_rate >= 0 AND discount_amount >= 0 AND tax_rate >= 0 AND total_amount >= 0)
);

CREATE TABLE hotel_invoice_charges (
  id NUMBER(12) PRIMARY KEY,
  invoice_id NUMBER(12) NOT NULL REFERENCES hotel_invoices(id) ON DELETE CASCADE,
  menu_item_id NUMBER(12) REFERENCES hotel_food_items(id),
  description VARCHAR2(80) NOT NULL,
  quantity NUMBER(8,2) DEFAULT 1 NOT NULL CHECK (quantity > 0),
  unit_price NUMBER(12,2) DEFAULT 0 NOT NULL CHECK (unit_price >= 0),
  amount NUMBER(12,2) NOT NULL CHECK (amount > 0),
  sort_order NUMBER(5) DEFAULT 0 NOT NULL
);

CREATE TABLE hotel_payments (
  id NUMBER(12) PRIMARY KEY,
  invoice_id NUMBER(12) NOT NULL REFERENCES hotel_invoices(id) ON DELETE CASCADE,
  amount NUMBER(12,2) NOT NULL CHECK (amount > 0),
  payment_method VARCHAR2(30) NOT NULL,
  paid_on DATE DEFAULT SYSDATE NOT NULL,
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE INDEX hotel_invoice_created_ix ON hotel_invoices(created_at);
CREATE INDEX hotel_invoice_guest_ix ON hotel_invoices(guest_name);
CREATE INDEX hotel_payment_invoice_ix ON hotel_payments(invoice_id);
CREATE INDEX hotel_food_item_active_ix ON hotel_food_items(active);

-- Settings are optional at first; users are prompted to add the property details.
