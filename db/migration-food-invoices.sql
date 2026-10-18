-- Run once against an existing HotelBilling schema to support food-only bills.
ALTER TABLE hotel_invoices ADD (invoice_type VARCHAR2(10) DEFAULT 'ROOM' NOT NULL);
ALTER TABLE hotel_invoice_charges ADD (quantity NUMBER(8,2) DEFAULT 1 NOT NULL);
ALTER TABLE hotel_invoice_charges ADD (unit_price NUMBER(12,2) DEFAULT 0 NOT NULL);
UPDATE hotel_invoice_charges SET unit_price = amount;
ALTER TABLE hotel_invoice_charges ADD CONSTRAINT hotel_charge_qty_ck CHECK (quantity > 0);
ALTER TABLE hotel_invoice_charges ADD CONSTRAINT hotel_charge_price_ck CHECK (unit_price >= 0);
COMMIT;
