-- TO DO 
-- Think about ID sequence overlap and what we can do about it - we need to be able to be able to check if the new sequence id exists already and if so, skip until its not in use
-- Add a boolean function that checks if the order date is < the delivery date

DROP SEQUENCE IF EXISTS ProductIDSequence;
CREATE SEQUENCE ProductIDSequence START 1 INCREMENT BY 1;


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
    OrderType      VARCHAR(30) NOT NULL, -- 'InStore', 'Collection' or 'Delivery' - should be an enum ideally
    OrderCompleted INTEGER NOT NULL, -- 0 or 1 
    OrderPlaced    DATE, 
    PRIMARY KEY (OrderID)
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

-- Create a new order in orders and return it's ID created by sequence
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

-- Ensure ProductStockAmount is sufficient to process this order for this product
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

CREATE OR REPLACE PROCEDURE removeOldOrders(removeFromDate DATE)
    LANGUAGE plpgsql AS
    $$
    BEGIN
        DELETE FROM orders o 
        USING collections c
        WHERE o.OrderID = c.OrderID
        AND removeFromDate - INTERVAL '8 days' > c.CollectionDate;
    END;
    $$;

CREATE OR REPLACE FUNCTION addUncollectedStock() RETURNS TRIGGER AS $removeOrders$
    BEGIN
        UPDATE inventory SET ProductStockAmount = ProductStockAmount + OLD.ProductQuantity WHERE ProductID = OLD.ProductID;     
        RETURN OLD;
    END; 
    $removeOrders$
    LANGUAGE plpgsql;

-- Delete all orders from 
CREATE TRIGGER removeOrders BEFORE DELETE
    ON order_products 
    FOR EACH ROW EXECUTE FUNCTION addUncollectedStock();  



--INSERT INTO inventory(ProductID, ProductDesc, ProductPrice, ProductStockAmount) VALUES (1, "Cap", 10.5, 5);
INSERT INTO inventory VALUES (1, 'testing1', 1, 1);
INSERT INTO inventory VALUES (2, 'testing2', 2, 2);
INSERT INTO inventory VALUES (3, 'testing3', 3, 3);
INSERT INTO inventory VALUES (4, 'testing4', 4, 4);
INSERT INTO staff VALUES (4, 'James', 'Smith');

INSERT INTO orders VALUES (10, 'Collection', 0, NOW());
INSERT INTO collections VALUES (10, 'James', 'Smith', NOW() - INTERVAL '1 year');
INSERT INTO order_products VALUES (10, 1, 1);
INSERT INTO staff_orders VALUES (4, 10)
