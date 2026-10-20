-- Run once against an existing HotelBilling schema to enable staff login.
CREATE TABLE hotel_auth_lock (
  id NUMBER(1) PRIMARY KEY CHECK (id = 1)
);
INSERT INTO hotel_auth_lock (id) VALUES (1);

CREATE TABLE hotel_staff_users (
  username VARCHAR2(80) PRIMARY KEY,
  password_salt VARCHAR2(32) NOT NULL,
  password_hash VARCHAR2(64) NOT NULL,
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
COMMIT;
