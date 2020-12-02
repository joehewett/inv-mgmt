DROP TABLE inventory CASCADE;
CREATE TABLE inventory (
    ProductID          integer not null, 
    ProductDesc        varchar(30) not null,
    ProductPrice       numeric(8,2) not null,
    ProductStockAmount integer,
    primary key (ProductID)
);

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
    foreign key (OrderID) references orders(orderID),
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
    foreign key (OrderID) references orders(orderID)
);

DROP TABLE collections CASCADE;
CREATE TABLE collections (
    OrderID         integer not null,
    FName           varchar(30) not null,
    LName           varchar(30) not null,
    CollectionDate  date,
    foreign key (OrderID) references orders(orderID)
);

DROP TABLE staff CASCADE;
CREATE TABLE staff (
    StaffID         integer not null,
    FName           varchar(30) not null,
    LName           varchar(30) not null,
    primary key (OrderID)
);

DROP TABLE staff_orders CASCADE;
CREATE TABLE staff_orders (
    StaffID         integer not null,
    OrderID         integer not null,
    foreign key (StaffID) references staff(StaffID),
    foreign key (OrderID) references orders(OrderID) 
);
