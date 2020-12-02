DROP TABLE inventory CASCADE;
CREATE TABLE inventory (
    ProductID          integer not null, 
    ProductDesc        varchar(30) not null,
    ProductPrice       numeric(8,2) not null,
    ProductStockAmount integer,
    primary key (ProductID)
);

CREATE OR REPLACE FUNCTION sufficientStock(id INTEGER, q INTEGER)
    RETURNS BOOLEAN LANGUAGE plpgsql AS
    $$
    DECLARE
    isSufficient BOOLEAN;
    BEGIN
    SELECT ProductStockAmount >= q INTO isSufficient FROM inventory WHERE ProductID = id; 
    RETURN isSufficient;
    END;
    $$;

CREATE OR REPLACE PROCEDURE reduceStock(id INTEGER, soldQuantity INTEGER)
    LANGUAGE plpgsql AS
    $$
    BEGIN
    UPDATE inventory SET ProductStockAmount = ProductStockAmount - soldQuantity WHERE ProductID = id;     
    END;
    $$;

--INSERT INTO inventory(ProductID, ProductDesc, ProductPrice, ProductStockAmount) VALUES (1, "Cap", 10.5, 5);
INSERT INTO inventory VALUES (1, 'testing1', 1, 1);
INSERT INTO inventory VALUES (2, 'testing2', 2, 2);
INSERT INTO inventory VALUES (3, 'testing3', 3, 3);
INSERT INTO inventory VALUES (4, 'testing4', 4, 4);

DROP TABLE orders CASCADE;
CREATE TABLE orders (
    OrderID        integer not null, 
    OrderType      varchar(30) not null, -- 'InStore', 'Collection' or 'Delivery' - should be an enum ideally
    OrderCompleted integer not null, -- 0 or 1 
    OrderPlaced    date, 
    primary key (OrderID)
);

DROP TABLE order_products CASCADE;
CREATE TABLE order_products (
    OrderID         integer not null,
    ProductID       integer not null, 
    ProductQuantity integer not null, 
    foreign key (OrderID) references orders(OrderID),
    foreign key (ProductID) references inventory(ProductID)
);

DROP TABLE deliveries CASCADE;
CREATE TABLE deliveries (
    OrderID       integer not null,
    FName         varchar(30) not null,
    LName         varchar(30) not null,
    House         varchar(30) not null,
    Street        varchar(30) not null,
    City          varchar(30) not null,
    DeliveryDate  date,
    foreign key (OrderID) references orders(OrderID)
);

DROP TABLE collections CASCADE;
CREATE TABLE collections (
    OrderID         integer not null,
    FName           varchar(30) not null,
    LName           varchar(30) not null,
    CollectionDate  date,
    foreign key (OrderID) references orders(OrderID)
);

DROP TABLE staff CASCADE;
CREATE TABLE staff (
    StaffID         integer not null,
    FName           varchar(30) not null,
    LName           varchar(30) not null,
    primary key (StaffID)
);

DROP TABLE staff_orders CASCADE;
CREATE TABLE staff_orders (
    StaffID         integer not null,
    OrderID         integer not null,
    foreign key (StaffID) references staff(StaffID),
    foreign key (OrderID) references orders(OrderID) 
);
