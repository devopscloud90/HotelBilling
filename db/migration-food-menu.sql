-- Run once after migration-food-invoices.sql on an existing HotelBilling schema.
CREATE SEQUENCE food_item_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE hotel_food_items (
  id NUMBER(12) PRIMARY KEY,
  item_name VARCHAR2(80) NOT NULL UNIQUE,
  unit_price NUMBER(12,2) NOT NULL CHECK (unit_price > 0),
  active CHAR(1) DEFAULT 'Y' NOT NULL CHECK (active IN ('Y','N')),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

ALTER TABLE hotel_invoice_charges ADD (menu_item_id NUMBER(12));
ALTER TABLE hotel_invoice_charges ADD CONSTRAINT hotel_charge_menu_fk FOREIGN KEY (menu_item_id) REFERENCES hotel_food_items(id);
CREATE INDEX hotel_food_item_active_ix ON hotel_food_items(active);
