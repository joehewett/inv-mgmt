DROP SEQUENCE IF EXISTS ProductIDSequence;
CREATE SEQUENCE ProductIDSequence START 1 INCREMENT BY 1;

--- ############################ ---         
--- #########  TABLES  ######### ---
--- ###'######################## --- 

DROP TABLE inventory CASCADE;
CREATE TABLE inventory (
    ProductID          INTEGER NOT NULL, 
    ProductDesc        VARCHAR(30) NOT NULL,
    ProductPrice       NUMERIC(8,2) NOT NULL,
    ProductStockAmount INTEGER,
    PRIMARY KEY (ProductID)
);

DROP TABLE orders CASCADE;
CREATE TABLE orders (
    OrderID        INTEGER NOT NULL, 
    OrderType      VARCHAR(30) NOT NULL , -- 'InStore', 'Collection' or 'Delivery' - should be an enum ideally
    OrderCompleted INTEGER NOT NULL, -- 0 or 1 
    OrderPlaced    DATE, 
    PRIMARY KEY (OrderID), 
    CONSTRAINT order_type_cons CHECK (OrderTYPE='InStore' OR OrderType='Collection' OR OrderType='Delivery'),
    CONSTRAINT order_comp_cons CHECK (OrderCompleted = 0 OR OrderCompleted = 1)
);

DROP TABLE order_products CASCADE;
CREATE TABLE order_products (
    OrderID         INTEGER NOT NULL,
    ProductID       INTEGER NOT NULL, 
    ProductQuantity INTEGER NOT NULL, 
    FOREIGN KEY (OrderID) REFERENCES orders(OrderID) ON DELETE CASCADE, 
    FOREIGN KEY (ProductID) REFERENCES inventory(ProductID) ON DELETE CASCADE
);

DROP TABLE deliveries CASCADE;
CREATE TABLE deliveries (
    OrderID       INTEGER NOT NULL,
    FName         VARCHAR(30) NOT NULL,
    LName         VARCHAR(30) NOT NULL,
    House         VARCHAR(30) NOT NULL,
    Street        VARCHAR(30) NOT NULL,
    City          VARCHAR(30) NOT NULL,
    DeliveryDate  date,
    FOREIGN KEY (OrderID) REFERENCES orders(OrderID)
);

DROP TABLE collections CASCADE;
CREATE TABLE collections (
    OrderID         INTEGER NOT NULL,
    FName           VARCHAR(30) NOT NULL,
    LName           VARCHAR(30) NOT NULL,
    CollectionDate  date,
    FOREIGN KEY (OrderID) REFERENCES orders(OrderID) ON DELETE CASCADE
);

DROP TABLE staff CASCADE;
CREATE TABLE staff (
    StaffID         INTEGER NOT NULL,
    FName           VARCHAR(30) NOT NULL,
    LName           VARCHAR(30) NOT NULL,
    PRIMARY KEY (StaffID)
);

DROP TABLE staff_orders CASCADE;
CREATE TABLE staff_orders (
    StaffID         INTEGER NOT NULL,
    OrderID         INTEGER NOT NULL,
    FOREIGN KEY (StaffID) REFERENCES staff(StaffID) ON DELETE CASCADE,
    FOREIGN KEY (OrderID) REFERENCES orders(OrderID) ON DELETE CASCADE 
);

--- ############################################ ---         
--- #########  FUNCTIONS & PROCEDURES  ######### ---
--- ############################################ --- 

-- Create a new order in orders and return its ID created by sequence
CREATE OR REPLACE FUNCTION insertOrder(orderType VARCHAR, orderPlaced DATE, orderCompleted INTEGER, staffID INTEGER)
    RETURNS INTEGER LANGUAGE plpgsql AS
    $$ 
    DECLARE
        newID INTEGER; 
    BEGIN 
        INSERT INTO orders (OrderID, OrderType, OrderCompleted, orderPlaced) VALUES (nextval('ProductIDSequence'), orderType, orderCompleted, orderPlaced) RETURNING OrderID INTO newID;
        CALL insertStaffOrder(staffID, newID);
        RETURN newID; 
    END; 
    $$; 

-- Create a new row in order products
-- Triggers checkValidOrderProduct call
CREATE OR REPLACE PROCEDURE insertOrderProduct(ordID INTEGER, prodID INTEGER, prodQuantity INTEGER)
    LANGUAGE plpgsql AS
    $$
    BEGIN
        INSERT INTO order_products (OrderID, ProductID, ProductQuantity) VALUES (ordID, prodID, prodQuantity); 
    END;
    $$;

-- Check that we have sufficient stock to process order for this item
-- If so, reduce stock. If not, throw and exception to trigger rollback
CREATE OR REPLACE FUNCTION checkValidOrderProduct() RETURNS TRIGGER AS $orderProductsTrigger$
    DECLARE
    isSufficient BOOLEAN;
    BEGIN
        SELECT sufficientStock(NEW.ProductID, NEW.ProductQuantity) INTO isSufficient;
        IF NOT (isSufficient) THEN
            RAISE EXCEPTION 'Insufficient stock to execute order';
        ELSE 
            CALL reduceStock(NEW.ProductID, NEW.ProductQuantity);
        END IF;
        RETURN NEW;
    END; 
    $orderProductsTrigger$
    LANGUAGE plpgsql;

-- Trigger to ensure new order_product row is valid, and if so reduce stock in inventory
CREATE TRIGGER orderProductsTrigger AFTER INSERT OR UPDATE
    ON order_products
    FOR EACH ROW EXECUTE FUNCTION checkValidOrderProduct();


-- Ensure ProductStockAmount is sufficient to process this order for this product
CREATE OR REPLACE FUNCTION sufficientStock(id INTEGER, quantity INTEGER)
    RETURNS BOOLEAN LANGUAGE plpgsql AS
    $$
    DECLARE
        isSufficient BOOLEAN;
    BEGIN
        SELECT ProductStockAmount >= quantity INTO isSufficient FROM inventory WHERE ProductID = id; 
        RETURN isSufficient;
    END;
    $$;

-- Called in display inventory to show the stock amount for the required product
CREATE OR REPLACE FUNCTION getQuantity(id INTEGER)
    RETURNS INTEGER LANGUAGE plpgsql AS
    $$
    DECLARE
        quantity INTEGER;
    BEGIN
        SELECT ProductStockAmount INTO quantity FROM inventory WHERE ProductID = id; 
        RETURN quantity;
    END;
    $$;

-- Reduce the stock of the given product in the inventory by given amount
CREATE OR REPLACE PROCEDURE reduceStock(id INTEGER, soldQuantity INTEGER)
    LANGUAGE plpgsql AS
    $$
    BEGIN
        UPDATE inventory SET ProductStockAmount = ProductStockAmount - soldQuantity WHERE ProductID = id;     
    END;
    $$;

-- Add a row to the staff orders table with the new order ID 
CREATE OR REPLACE PROCEDURE insertStaffOrder(staffID INTEGER, orderID INTEGER)
    LANGUAGE plpgsql AS
    $$
    BEGIN
        INSERT INTO staff_orders (StaffID, OrderID) VALUES (staffID, orderID); 
    END;
    $$;

-- Create a new order in orders and return it's ID created by sequence
CREATE OR REPLACE PROCEDURE insertCollection(orderID INTEGER, fName VARCHAR, lName VARCHAR, collectionDate DATE)
    LANGUAGE plpgsql AS
    $$ 
    BEGIN 
        INSERT INTO collections (orderID, FName, LName, CollectionDate) VALUES (orderID, fName, lName, collectionDate);
    END; 
    $$; 

-- Create a new order in orders and return it's ID created by sequence
CREATE OR REPLACE PROCEDURE insertDelivery(orderID INTEGER, fName VARCHAR, lName VARCHAR, house VARCHAR, street VARCHAR, city VARCHAR, deliveryDate DATE)
    LANGUAGE plpgsql AS
    $$ 
    BEGIN 
        INSERT INTO deliveries (orderID, FName, LName, House, Street, City, DeliveryDate) VALUES (orderID, fName, lName, house, street, city, deliveryDate);
    END; 
    $$; 

-- Remove all orders that are 8 days older than the date provided
CREATE OR REPLACE PROCEDURE removeOldOrders(removeFromDate DATE)
    LANGUAGE plpgsql AS
    $$
    BEGIN
        DELETE FROM orders o 
        USING collections c
        WHERE o.OrderID = c.OrderID
        AND removeFromDate - INTERVAL '8 days' >= c.CollectionDate
        AND o.OrderCompleted = 0;
    END;
    $$;

-- Triggered when we delete from order_products in opt 5 - adds items back to inventory
CREATE OR REPLACE FUNCTION addUncollectedStock() RETURNS TRIGGER AS $removeOrders$
    BEGIN
        UPDATE inventory SET ProductStockAmount = ProductStockAmount + OLD.ProductQuantity WHERE ProductID = OLD.ProductID;     
        RETURN OLD;
    END; 
    $removeOrders$
    LANGUAGE plpgsql;

-- Before we delete a row from order_products, add the stock back to the inventory
CREATE TRIGGER removeOrders BEFORE DELETE
    ON order_products 
    FOR EACH ROW EXECUTE FUNCTION addUncollectedStock();  


--- ########################### ---         
--- #########  VIEWS  ######### ---
--- ########################### --- 

-- View - gets a table of product IDs with the price * quantity sold for that product in the order_product row 
-- used in opt6 as a componenet part to simplify the query
CREATE OR REPLACE VIEW allSaleValues AS 
    SELECT so.StaffID, o.OrderPlaced, op.productID, op.OrderID AS id, op.ProductQuantity * i.ProductPrice AS saleValue 
    FROM order_products op 
    INNER JOIN inventory i ON op.ProductID = i.ProductID
    INNER JOIN orders o ON op.OrderID = o.OrderID
    INNER JOIN staff_orders so ON o.OrderID = so.OrderID;

-- View all collections that have not been collected, regardless of date. We can then filter this view further later
CREATE OR REPLACE VIEW uncollectedCollectionsView AS 
    SELECT o.OrderID AS OrderID, c.CollectionDate FROM orders o
    INNER JOIN collections c
    ON o.OrderID = c.OrderID
    WHERE o.OrderCompleted = 0;

-- View - gets the highest selling products in descending order of total value
-- Uses in opt4 and opt7
CREATE OR REPLACE VIEW profitableProductsView AS 
    SELECT i.ProductID, i.ProductDesc, COALESCE(i.ProductPrice * sales,0) AS totalValue FROM inventory i 
    LEFT OUTER JOIN (
        SELECT ProductID, SUM(ProductQuantity) AS sales 
        FROM order_products 
        GROUP BY ProductID
    ) p ON i.ProductID = p.ProductID
    ORDER BY totalValue DESC; 

-- View - Get the lifetime sales of all members of staff over 50,000
-- Works by getting the total value of each order, then linking with the staff table
-- It then groups by staffID, summing the order values for that staff member. A final HAVING checks that this total is >= 50k 
CREATE OR REPLACE VIEW lifetimeSalesView AS
    SELECT s.fName || ' ' || s.lName AS fullName, so.StaffID, SUM(totalOrderValue) AS lifetimeSales 
    FROM staff_orders so 
    INNER JOIN (
        SELECT w.id as id, SUM(saleValue) As totalOrderValue 
        FROM allSaleValues w 
        GROUP BY w.id
    ) x 
    ON x.id = so.OrderID 
    INNER JOIN staff s ON s.StaffID = so.StaffID
    GROUP BY so.StaffID, fullName
    HAVING SUM(totalOrderValue) >= 50000
    ORDER BY lifetimeSales DESC;
    

-- OPTION 7
-- View -- Uses opt4's view to get the profitableProducts > 20k and then joins with inventory, order_products, staff_orders and staff
-- to return a table with staff info, products sold, units of said product sold, and value of said product sold
-- we then do a minor amount of formatting in the calling Java to render it in the correct way
CREATE OR REPLACE VIEW highestSellingProductSellersView AS
    SELECT staffID, fName, lName, ProductID, SUM(ProductQuantity) AS unitsSold, SUM(salePrice) AS valOfProductSold FROM (
        SELECT s.StaffID, s.fName as fName, s.lName AS lName, i.ProductID AS ProductID, op.ProductQuantity AS ProductQuantity, op.ProductQuantity * i.ProductPrice AS salePrice
        FROM order_products op 
        INNER JOIN staff_orders so ON op.OrderID = so.OrderID
        INNER JOIN inventory i ON i.ProductID = op.ProductID
        INNER JOIN staff s ON so.StaffID = s.StaffID
        INNER JOIN (
            SELECT ProductID FROM profitableProductsView 
            WHERE totalValue > 20000
        ) x ON i.ProductID = x.ProductID
    ) AS x 
    GROUP BY staffID, fName, lName, ProductID;

-- Get the staff in order of value of items they've sold that have sold > 20k 
CREATE OR REPLACE VIEW mostValuableStaff AS
    SELECT staffID FROM (
        SELECT staffID, SUM(valOfProductSold) FROM highestSellingProductSellersView 
        GROUP BY staffID 
        ORDER BY SUM(valOfProductSold) DESC
    ) AS pv; 
    
-- OPTION 8
-- Get all items that have done 20k this year
CREATE OR REPLACE VIEW year20kItems AS 
    SELECT DATE_PART('year',orderPlaced) AS yr, ProductID, SUM(saleValue) 
    FROM allSaleValues 
    GROUP BY yr, ProductID
    HAVING SUM(saleValue) > 20000;

-- Get all staff sales of products that have done 20k this year
CREATE OR REPLACE VIEW staff20kItems AS 
    SELECT y.yr, so.StaffID, op.ProductID FROM order_products op 
    INNER JOIN staff_orders so ON op.OrderID = so.OrderID
    INNER JOIN year20kItems y ON y.ProductID = op.ProductID
    GROUP BY so.staffID, op.productID, y.yr;

-- Get staff that have sold 30k this year
CREATE OR REPLACE VIEW yearlySales30k AS
    SELECT s.StaffID, DATE_PART('year', o.OrderPlaced) AS yr
    FROM staff_orders s 
    INNER JOIN (
        SELECT w.id as id, SUM(saleValue) As totalOrderValue 
        FROM allSaleValues w 
        GROUP BY w.id
    ) x 
    ON x.id = s.OrderID 
    INNER JOIN orders o ON o.OrderID = s.OrderID
    GROUP BY s.StaffID, yr
    HAVING SUM(totalOrderValue) >= 30000
    ORDER BY SUM(totalOrderValue) DESC;

-- All staff that have sold 30k stock and count > 0 of products they've sold that have sold > 20k 
CREATE OR REPLACE VIEW staffYearly20kProductSales AS 
    SELECT DISTINCT s.fName || ' ' || s.lName AS fullName, q.staffid, q.yr, q.count FROM (
        SELECT StaffID, yr, COUNT(ProductID)
        FROM staff20kItems d
        GROUP BY StaffID, yr
    ) q 
    INNER JOIN yearlySales30k y ON y.StaffID = q.StaffID
    INNER JOIN yearlySales30k x ON x.yr = q.yr
    INNER JOIN staff s ON s.StaffID = q.StaffID; 
