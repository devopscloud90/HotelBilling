# Stayledger Hotel Billing — Java / JSP edition

This version runs as a Java 8 web application on Apache Tomcat 9. Servlets handle requests, JSP renders the interface, and Oracle XE stores hotel settings, food menu items, invoices, and payments.

## Requirements

- JDK 8
- Apache Tomcat 9
- Oracle XE with the Oracle JDBC driver in `WEB-INF/lib/ojdbc14.jar`

The supplied build script uses the Tomcat and JDK locations from this workstation by default. Set `CATALINA_HOME` and `JAVA_HOME` first if your installation paths differ.

## Database setup

1. Create an Oracle schema/user for the application and grant it permission to create tables, indexes, and sequences. The local development connection defaults are `jdbc:oracle:thin:@localhost:1521:xe`, username `hotelbilling`, password `hotelbilling`.
2. For a fresh database, connect as that user and run `db/schema-oracle.sql` once. For an existing deployment, run `db/migration-food-invoices.sql`, then `db/migration-food-menu.sql`; these preserve existing invoices while adding food menu support.
3. Configure connection values before starting Tomcat using environment variables or Java system properties: `HOTEL_DB_URL`, `HOTEL_DB_USER`, and `HOTEL_DB_PASSWORD`.

The app currently has no staff login or role management.

## Build and deploy

Run `build.cmd`. It compiles the Java 8 sources against Tomcat's Servlet API and packages `HotelBilling.war` without bundling the Servlet API. Copy the WAR to Tomcat's `webapps` folder after configuring the Oracle connection, then start or restart Tomcat. Open `http://localhost:8080/HotelBilling/`.

## Included features

- Hotel dashboard with invoice count, payments received today, and outstanding balance
- Searchable invoice list with payment status filtering
- Walk-in food bills without customer details, optional table numbers, menu item selection, quantity-based live totals, discount, and tax
- Food menu management for adding items, changing INR rates, and marking items unavailable
- Existing room invoices remain readable after the food-invoice database migrations
- Invoice detail and print layout
- Payment recording with server-side balance validation
- Property settings saved in Oracle and copied onto new invoices

The menu starts empty. Open `/HotelBilling/menu` to add food names and INR rates; unavailable items are hidden from new bills. Create a food bill at `/HotelBilling/invoice`. Invoice totals update in the browser as quantities change and are recalculated from saved menu rates on the server.

The original browser-only frontend files (`index.html` and `app.js`) are retained as a prototype; Tomcat serves the Java/JSP application in the WAR.
