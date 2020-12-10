import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;

class Assignment {

    public static String readEntry(String prompt) {
        try {
            StringBuffer buffer = new StringBuffer();
            System.out.print(prompt);
            System.out.flush();
            int c = System.in.read();
            while (c != '\n' && c != -1) {
                buffer.append((char) c);
                c = System.in.read();
            }
            return buffer.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    public static void main(String args[]) throws SQLException, IOException {
        // You should only need to fetch the connection details once
        Connection conn = getConnection();
        String opt = "";

        while (true) {
            printMenu();
            opt = readEntry("Enter your choice: ");
            System.out.println("");

            switch (opt) {
                case "0":
                    conn.close();
                    System.out.println("Exiting Inventory Management...");
                    return;
                case "1":
                    Order order1 = new Order("InStore");
                    if (order1.handleOption()) {
                        option1(conn, order1.productIDsArray, order1.quantitiesArray, order1.saleDate, order1.staffID);
                    }
                    break;
                case "2":
                    Order order2 = new Order("Collection");
                    if (order2.handleOption()) {
                        option2(conn, order2.productIDsArray, order2.quantitiesArray, order2.saleDate,
                            order2.collectionDate, order2.firstName, order2.lastName, order2.staffID);
                    }
                    break;
                case "3":
                    Order order3 = new Order("Delivery");
                    if (order3.handleOption()) {
                    option3(conn, order3.productIDsArray, order3.quantitiesArray, order3.saleDate,
                            order3.collectionDate, order3.firstName, order3.lastName, order3.house, order3.street,
                            order3.city, order3.staffID);
                    }
                    break;
                case "4":
                    option4(conn);
                    break;
                case "5":
                    String userDate = readEntry("Enter the date: ");
                    option5(conn, userDate);
                    break;
                case "6":
                    option6(conn);
                    break;
                case "7":
                    option7(conn);
                    break;
                case "8":
                    Integer myYear = Integer.valueOf(readEntry("Enter a year: "));
                    option8(conn, myYear);

                    break;
                default:
                    System.out.println("That's not a valid option. Press a number between 0 and 8.");
            }
        }
    }

    /**
     * @param conn         An open database connection
     * @param orderType    One of InHouse, Delivery, Collection so we can process
     *                     the order correctly
     * @param productIDs   An array of productIDs associated with an order
     * @param quantities   An array of quantities of a product. The index of a
     *                     quantity correspeonds with an index in productIDs
     * @param orderDate    A string in the form of 'DD-Mon-YY' that represents the
     *                     date the order was made
     * @param deliveryDate A string in the form of 'DD-Mon-YY' that represents the
     *                     date the order will be delivered
     * @param fName        The first name of the customer who will receive the order
     * @param LName        The last name of the customer who will receive the order
     * @param house        The house name or number of the delivery address
     * @param street       The street name of the delivery address
     * @param city         The city name of the delivery address
     * @param staffID      The id of the staff member who sold the order
     */
    public static void executeOrder(Connection conn, String orderType, int[] productIDs, int[] quantities,
            String orderDate, String deliveryOrCollectionDate, String fName, String LName, String house, String street,
            String city, int staffID) {

        Boolean successfulOrder = false;
        Integer orderCompleted = 0;

        // Check the order type and determine the completion status. If not a valid
        // status, return.
        if (orderType.equals("InStore")) {
            orderCompleted = 1;
        } else if (orderType.equals("Delivery") || orderType.equals("Collection")) {
            orderCompleted = 0;
        } else {
            System.out.print("That is not a valid order type.\n");
            return;
        }

        // If we couldn't process the date, return. getSQLDate will handle the exception
        java.sql.Date sqlDate;
        try {
            sqlDate = getSQLDate(orderDate);
            if (sqlDate == null) {return;}
        } catch (Exception e) {
            System.out.println("That doesn't look like a valid date format - usage: dd-MMM-yy e.g. 17-Nov-20\n");
            return; 
        }

        try {
            // Turn off auto-commiting in case the order fails
            conn.setAutoCommit(false);

            // Create a new order
            int id = insertOrder(conn, "InStore", orderCompleted, sqlDate, staffID);
            if (id < 1) {
                System.out.println("Something went wrong while creating a new order. Please try again.\n");
                return;
            }

            // For each product in the order, create an order_products row
            for (int i = 0; i < productIDs.length; i++) {
                insertOrderProduct(conn, id, productIDs[i], quantities[i]);
            }

            // If this is a collection or delivery then we have an extra step to do
            if (orderType.equals("Collection")) {
                insertCollection(conn, id, fName, LName, getSQLDate(deliveryOrCollectionDate));
            } else if (orderType.equals("Delivery")) {
                insertDelivery(conn, id, fName, LName, house, street, city, getSQLDate(deliveryOrCollectionDate));
            }

            // Now that insertOrderProduct has handled the stock reductions, print the new
            // stock amounts
            displayInventory(conn, productIDs);

            // If we've made it this far without an SQLException then we know the order has
            // completed successfully so commit changes to db
            conn.setAutoCommit(true);
            successfulOrder = true;

        } catch (SQLException e) {
            System.out.println(
                    "\nAn error occurred while trying to add the order:\n- Ensure there is enough stock in the inventory\n- Ensure the staff ID number exists\n- Ensure all the products are valid\n");
            // System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            System.out.print("Exception while creating order...");
            e.printStackTrace();
        } finally {
            // If an exception is thrown we want to roll back so check successfulOrder
            // boolean in a finally block
            if (!successfulOrder) {
                try {
                    System.out.println("Order unsuccessful - rolling back...\n");
                    conn.rollback();
                } catch (SQLException e) {
                    System.err.format(
                            "CRITICAL ERROR - Could not roll back to safe state. Database might be offline. \nSQL State: %s\n%s",
                            e.getSQLState(), e.getMessage());
                }
            }
        }
    }

    /**
     * @param conn       An open database connection
     * @param productIDs An array of productIDs associated with an order
     * @param quantities An array of quantities of a product. The index of a
     *                   quantity correspeonds with an index in productIDs
     * @param orderDate  A string in the form of 'DD-Mon-YY' that represents the
     *                   date the order was made
     * @param staffID    The id of the staff member who sold the order
     */
    public static void option1(Connection conn, int[] productIDs, int[] quantities, String orderDate, int staffID) {
        executeOrder(conn, "InStore", productIDs, quantities, orderDate, null, null, null, null, null, null, staffID);
    }

    /**
     * @param conn           An open database connection
     * @param productIDs     An array of productIDs associated with an order
     * @param quantities     An array of quantities of a product. The index of a
     *                       quantity correspeonds with an index in productIDs
     * @param orderDate      A string in the form of 'DD-Mon-YY' that represents the
     *                       date the order was made
     * @param collectionDate A string in the form of 'DD-Mon-YY' that represents the
     *                       date the order will be collected
     * @param fName          The first name of the customer who will collect the
     *                       order
     * @param LName          The last name of the customer who will collect the
     *                       order
     * @param staffID        The id of the staff member who sold the order
     */
    public static void option2(Connection conn, int[] productIDs, int[] quantities, String orderDate,
            String collectionDate, String fName, String LName, int staffID) {
        executeOrder(conn, "Collection", productIDs, quantities, orderDate, collectionDate, fName, LName, null, null,
                null, staffID);
    }

    /**
     * @param conn         An open database connection
     * @param productIDs   An array of productIDs associated with an order
     * @param quantities   An array of quantities of a product. The index of a
     *                     quantity correspeonds with an index in productIDs
     * @param orderDate    A string in the form of 'DD-Mon-YY' that represents the
     *                     date the order was made
     * @param deliveryDate A string in the form of 'DD-Mon-YY' that represents the
     *                     date the order will be delivered
     * @param fName        The first name of the customer who will receive the order
     * @param LName        The last name of the customer who will receive the order
     * @param house        The house name or number of the delivery address
     * @param street       The street name of the delivery address
     * @param city         The city name of the delivery address
     * @param staffID      The id of the staff member who sold the order
     */
    public static void option3(Connection conn, int[] productIDs, int[] quantities, String orderDate,
            String deliveryDate, String fName, String LName, String house, String street, String city, int staffID) {
        executeOrder(conn, "Delivery", productIDs, quantities, orderDate, deliveryDate, fName, LName, house, street,
                city, staffID);
    }

    /**
     * @param conn An open database connection
     * @throws SQLException
     */
    public static void option4(Connection conn) throws SQLException {
        // Select a view that we have predefined in the shchema
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM profitableProductsView");

        // Call a custom formatting function that prints out table nicely
        formatTable(rs);

        rs.close();
        st.close();
    }

    /**
     * @param conn An open database connection
     * @param date The target date to test collection deliveries against
     */
    public static void option5(Connection conn, String date) {
        // Identify all orders that are 8 days older than the provided date
        // Call a procedure that DELETES all orders WHERE id IN (SELECT id FROM ORDERS
        // WHERE date too old)
        // We have a trigger that deletes all order_products when an order is deleted
        // We have another trigger trigger that re adds all inventory when order product
        // gets deleted
        java.sql.Date sqlDate = getSQLDate(date);
        if (sqlDate == null) { return; }
        Boolean deletionsMade = false;
        
        try {
            // Get the result set of ID's that we're going to delete. We'll need them so we
            // can display after the delete has happened
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT * FROM uncollectedCollectionsView WHERE ? >= collectionDate + INTERVAL '8 Days'");
            pst.setDate(1, sqlDate);
            ResultSet rs = pst.executeQuery();

            // Call a procedure that identifies and deletes all old orders.
            // The relevant tables have triggers on DELETE to perform necessary updates e.g.
            // add old stock back to inventory
            CallableStatement stmt = conn.prepareCall("call removeOldOrders(?)");
            stmt.setDate(1, getSQLDate(date));
            stmt.execute();
            stmt.close();

            // Use the dataset we created earlier to print out the orders that we cancelled
            while (rs.next()) {
                deletionsMade = true;
                System.out.println("Order " + rs.getInt(1) + " has been cancelled");
            }

            if (deletionsMade) {
                System.out.println("Removed orders 8 days older than " + date + " and updated inventory.\n");
            } else {
                System.out.println("No orders were found that were 8 days older than " + date + "\n");
            }

            pst.close();
            rs.close();

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param conn An open database connection
     */
    public static void option6(Connection conn) {
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT fullName, lifetimeSales FROM lifetimeSalesView");

            formatTable(rs);

            st.close();
            rs.close();

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param conn An open database connection
     */
    public static void option7(Connection conn) {

        // Hashmap linking ID's to their profiles. Profiles contain name and all the
        // orders/quantities that the staff member has
        HashMap<Integer, Profile> people = new HashMap<Integer, Profile>();
        // Keep track of what products we see so that we can list them off later
        ArrayList<Integer> ProductIDs = new ArrayList<Integer>();

        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM highestSellingProductSellersView");

            // Parse the data into a structure that we can navigate later to display the
            // data in the desired way
            // Note that this is just to store the data, and no sorting or ordering is done
            while (rs.next()) {
                Profile person;
                Integer staffID = rs.getInt(1);
                Integer productID = rs.getInt(4);

                // Lets keep a list of product IDs
                if (!ProductIDs.contains(productID)) {
                    ProductIDs.add(productID);
                }

                // If this staffID already exists as key then we don't want to overwrite it - we
                // want to fetch the list of objects and add to it
                if (people.containsKey(staffID)) {
                    person = (Profile) people.get(staffID);
                } else {
                    // otherwise, we want to create a new list and put it in the Hashmap with the
                    // staffID as Key
                    person = new Profile(staffID, rs.getString(2) + " " + rs.getString(3));
                }
                person.addProduct(productID, rs.getInt(5), rs.getInt(6));
                people.put(staffID, person);

            }

            rs.close();

            // This is how we know what order to display the staff in
            // Uses a view that manipulates the main highestSellingProductSellersView to get
            // the ID's in order of descending value
            ResultSet sl = st.executeQuery("SELECT * FROM mostValuableStaff");

            System.out.print("Staff Name");
            for (int i = 0; i < ProductIDs.size(); i++) {
                // header += ", Product " + ProductIDs.get(i);
                System.out.print(", Product " + ProductIDs.get(i));
            }
            System.out.println("");

            // Loop over the staff list from mostValuableStaff and retrieve their
            // information from the profiles we stored
            while (sl.next()) {
                Integer staffID = sl.getInt(1);
                Profile person = people.get(staffID);

                System.out.print(person.fullName);
                for (int i = 0; i < ProductIDs.size(); i++) {
                    System.out.print(", " + person.getProduct(ProductIDs.get(i)));
                }
                System.out.println("");
            }
            System.out.println("");

            sl.close();
            st.close();

        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param conn An open database connection
     * @param year The target year we match employee and product sales against
     */
    public static void option8(Connection conn, int year) {
        // Incomplete - Code for option 8 goes here

        PreparedStatement pst;
        try {
            // We filter a view staffYearly20kProductSales
            // Here we check how many "20k products" a person has sold for a given year, named s.count
            // We compared this to the total number of items that sold 20k in a year given year. 
            // If these numbers are equal for a given year, we know that the person has sold at least one of each product
            // The 30k filtering happens prior to this step - see yearlySales30k.  
            pst = conn.prepareStatement(
                "SELECT fullName FROM staffYearly20kProductSales s " + 
                "WHERE s.yr = ? " + 
                "AND s.count = (" + 
                "   SELECT COUNT(*) " + 
                "   FROM year20kItems i " + 
                "   WHERE i.yr = ?)"
            );
            pst.setInt(1, year);
            pst.setInt(2, year);

            ResultSet rs = pst.executeQuery();

            System.out.println("\n-- Employees Of The Year " + year + " --");
            while (rs.next()) {
                System.out.println(rs.getString(1)); 
            }
            System.out.println("");

        } catch (SQLException e) {
            e.printStackTrace();
        }
	}

    public static Connection getConnection(){
        Properties props = new Properties();
        props.setProperty("socketFactory", "org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg");

        props.setProperty("socketFactoryArg",System.getenv("PGHOST") + "/.s.PGSQL.5432");
        Connection conn;
        try{
          conn = DriverManager.getConnection("jdbc:postgresql://localhost/deptstore", props);
          return conn;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param conn      An open database connection
     * @param productID A productID associated with an order
     */
    public static void displayInventory(Connection conn, int[] productIDs) {
        int quantity;
        try {
            // Create a callable statement that calls getQuantity
            // Iterate over the products and get the quantity remaining for each id
            CallableStatement stmt = conn.prepareCall("{ call getQuantity(?) }");
            System.out.println("");
            for (int i = 0; i < productIDs.length; i++) {
                stmt.registerOutParameter(1, Types.INTEGER);
                stmt.setInt(1, productIDs[i]);
                stmt.execute();
                quantity = stmt.getInt(1);
                System.out.println("Product ID " + productIDs[i] + " stock is now at " + quantity);
            }
            System.out.println("");
            stmt.close();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param conn           An open database connection
     * @param orderType      The type of order that was made, InHouse, Delivery etc
     * @param orderCompleted An int representing a boolean, that tells us if the
     *                       order is complete yet
     * @param orderPlaced    An SQL date that tells us when the order was made,
     *                       determined by user
     * @param staffID        Id of the staff member that made the order
     * @throws SQLException  Will generate an SQL exception usually in the case of an FK violation
     */
    public static int insertOrder(Connection conn, String orderType, Integer orderCompleted, java.sql.Date orderPlaced, Integer staffID)
            throws SQLException {
        int newOrderID = -1;

        CallableStatement stmt = conn.prepareCall("{ call insertOrder(?, ?, ?, ?) }");
        stmt.registerOutParameter(1, Types.INTEGER);
        stmt.setString(1, orderType);
        stmt.setDate(2, orderPlaced);
        stmt.setInt(3, orderCompleted);
        stmt.setInt(4, staffID);
        stmt.execute();
        newOrderID = stmt.getInt(1);
        stmt.close();

        return newOrderID;
    }

    /**
     * @param conn      An open database connection
     * @param orderID   The ID of the order
     * @param productID The ID of the product that we're going to add to the order
     * @param quantity  Amount of said product that has been ordered in this order
     * @throws SQLException If there is insufficient stock then this method will
     *                      throw and SQLException which we can catch in the calling
     *                      method
     */
    public static void insertOrderProduct(Connection conn, Integer orderID, Integer productID, Integer quantity)
            throws SQLException {
        CallableStatement stmt = conn.prepareCall("call insertOrderProduct(?, ?, ?)");
        stmt.setInt(1, orderID);
        stmt.setInt(2, productID);
        stmt.setInt(3, quantity);
        stmt.execute();
        stmt.close();
    }

    /**
     * @param conn      An open database connection
     * @param fName Client who is collecting orders first name 
     * @param lName Last name of the client who is picking up the order
     * @param collectionDate The day on which the delivery will be made
     */
    public static void insertCollection(Connection conn, Integer orderID, String fName, String lName, java.sql.Date collectionDate)
            throws SQLException {
        CallableStatement stmt = conn.prepareCall("call insertCollection(?, ?, ?, ?)");
        stmt.setInt(1, orderID);
        stmt.setString(2, fName);
        stmt.setString(3, lName);
        stmt.setDate(4, collectionDate);
        stmt.execute();
        stmt.close();
    }

    /**
     * @param conn      An open database connection
     * @param fName Client who is collecting orders first name 
     * @param lName Last name of the client who is picking up the order
     * @param house House name of delivery 
     * @param street Street of house to deliver to  
     * @param city The city within which the street resides upon which one shall find the house to which thou shalt deliver thine order 
     * @param collectionDate The day on which the delivery will be made
     */
    public static void insertDelivery(Connection conn, Integer orderID, String fName, String lName, String house, String street, String city, java.sql.Date deliveryDate)
            throws SQLException {
        CallableStatement stmt = conn.prepareCall("call insertDelivery(?, ?, ?, ?, ?, ?, ?)");
        stmt.setInt(1, orderID);
        stmt.setString(2, fName);
        stmt.setString(3, lName);
        stmt.setString(4, house);
        stmt.setString(5, street);
        stmt.setString(6, city);
        stmt.setDate(7, deliveryDate);
        stmt.execute();
        stmt.close();
    }

    /**
     * @param userInput Date input by the user in string format that we need to convert 
     */
    public static java.sql.Date getSQLDate(String userInput) {
        // Create the date formats that we're going to need to convert
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MMM-yy");
        SimpleDateFormat sqlFormat = new SimpleDateFormat("yyyy-MM-dd");
        // Create an actual date object from the users string
        java.util.Date formattedUserInput;
        try {
            // Try to parse the user input - try catch to catch anything unusual
            formattedUserInput = inputFormat.parse(userInput);
            if (formattedUserInput != null) {
                String newDate = sqlFormat.format(formattedUserInput); 
                java.sql.Date finalDate = java.sql.Date.valueOf(newDate); 
                return finalDate;
            }
        } catch (Exception e) {
            System.out.println("Unable to parse date input - usage: dd-MMM-yy e.g. 17-Nov-18");
        }
        return null;
    }
    
	/**
     * @param rs The dataset we want to print out
     */
    public static void formatTable(ResultSet rs) throws SQLException {
        String dividerLine = "------------------------------";
        String columnSpacer = " ";
        String formatString = ""; 

        ResultSetMetaData md = rs.getMetaData(); 
        Integer colCount = md.getColumnCount();

        if (colCount == 2) {
            formatString = "%-30s%-1s%-30s";
        } else if (colCount == 3) {
            formatString = "%-30s%-1s%-30s%-1s%-30s";
        } else {
            System.out.println("I don't know how to format this table\n"); 
        }

        // Title bar with surrounding dashes
        System.out.println("");
        if (colCount == 2) {
            System.out.format(formatString, "EmployeeName", columnSpacer, "TotalValueSold"); 
            System.out.println();
            System.out.format(formatString, dividerLine, columnSpacer, dividerLine);
            System.out.println("");
        } else if (colCount == 3) {
            System.out.format(formatString, "Product ID", columnSpacer, "ProductDesc", columnSpacer, "TotalValueSold");
            System.out.println();
            System.out.format(formatString, dividerLine, columnSpacer, dividerLine, columnSpacer, dividerLine);
            System.out.println("");
        }

        // Rows of the table, starting with | 
        while (rs.next()) {
            if (colCount == 2) {
                System.out.format(formatString, rs.getString(1) + ",", columnSpacer, "£" + rs.getInt(2));
            } else if (colCount == 3) {
                System.out.format(formatString, rs.getInt(1) + ",", columnSpacer, rs.getString(2) + ",", columnSpacer, "£" + rs.getInt(3));
            }
            System.out.println("");
        }

        // Bottom line to finish off table
        if (colCount == 2) {
            System.out.format(formatString, dividerLine, columnSpacer, dividerLine);
        } else if (colCount == 3) {
            System.out.format(formatString, dividerLine, columnSpacer, dividerLine, columnSpacer, dividerLine);
        }
        System.out.println("\n");
    }

    public static void printMenu() {
        System.out.print("(1) In-Store Purchase\n");
        System.out.print("(2) Collection\n");
        System.out.print("(3) Delivery\n");
        System.out.print("(4) Biggest Sellers\n");
        System.out.print("(5) Reserved Stock\n");
        System.out.print("(6) Staff Life-Time Success\n");
        System.out.print("(7) Staff Contribution\n");
        System.out.print("(8) Employee of the Year\n");
        System.out.print("(0) Quit\n");
        System.out.println("");
    }

}

// Custom class for storing staff and their sales for option 7
class Profile {

    public String fullName; 
    public Integer productID; 
    public HashMap<Integer, Integer> unitsSold; 
    public Integer valOfProductSold; 

    public Profile(Integer id, String name) {
        productID = id; 
        fullName = name;
        unitsSold = new HashMap<Integer, Integer>(); 
        valOfProductSold = 0; 
    }

    // Add a product to the store of products that this staff member has sold
    // First checks if that item exists in the HashMap - it should never since the SQL groups by StaffID & ProductID 
    public void addProduct(Integer productID, Integer units, Integer value) {
        if (unitsSold.containsKey(productID)) {
            System.out.println("Something has gone wrong. It looks like this item " + productID + " already exists for this person\n"); 
        } else {
            unitsSold.put(productID, units);
            valOfProductSold += value;
        }
    }

    // When outputting the table, check if this user has any units sold for the current product we're looking at
    // If the productID key doesn't exist then we can be sure this person has sold 0 units of this product
    public Integer getProduct(Integer productID) {
        if (unitsSold.containsKey(productID)) {
            return unitsSold.get(productID);
        } 
        return 0; 
    }

}

class Order {
    
    public String orderType; 
    private ArrayList<Integer> productIDs, quantities; 
    public int[] productIDsArray, quantitiesArray; 
    public String saleDate, collectionDate = "";
    public String firstName, lastName = "";
    public String house, street, city = ""; 
    public Integer staffID = 0;

    public Order(String type) {
        orderType = type; 
        productIDs = new ArrayList();
        quantities = new ArrayList();
    }

    // Get user input for options 1,2,3
    // Decides what to ask/store based on this.orderType passed at construction 
    public Boolean handleOption() {

        String opt = "y";

        try {
            while (opt.equals("y") || opt.equals("Y")) {
                opt = Assignment.readEntry("Enter a product ID: ");
                productIDs.add(Integer.valueOf(opt));
                opt = Assignment.readEntry("Enter the quantity sold: ");
                if (Integer.valueOf(opt) <= 0) {
                    throw new Exception(); 
                }
                quantities.add(Integer.valueOf(opt));
                opt = Assignment.readEntry("Is there another product in the order?: ");
            }

            saleDate = Assignment.readEntry("Enter the date sold: ");

            if (orderType.equals("Collection") || orderType.equals("Delivery")) {
                collectionDate = Assignment.readEntry("Enter the date of collection: "); 
                if (Assignment.getSQLDate(saleDate).compareTo(Assignment.getSQLDate(collectionDate)) > 0) {
                    System.out.println("\nYour delivery date can't be before your order date.");
                    throw new Exception(); 
                }
                firstName = Assignment.readEntry("Enter the first name of the collector: "); 
                lastName = Assignment.readEntry("Enter the last name of the collector: "); 
            }


            if (orderType.equals("Delivery")) {
                house = Assignment.readEntry("Enter the house name/no: "); 
                street = Assignment.readEntry("Enter the street: "); 
                city = Assignment.readEntry("Enter the City: "); 
            }

            staffID = Integer.valueOf(Assignment.readEntry("Enter your staff ID: "));

            System.out.println("");

            quantitiesArray = new int[quantities.size()];
            productIDsArray = new int[quantities.size()];

            // Convert from our array list to an array that the option function can use
            for (int i = 0; i < productIDs.size(); i++) {
                productIDsArray[i] = productIDs.get(i);
                quantitiesArray[i] = quantities.get(i);
            }
        } catch (Exception e) {
            System.out.println("Your input is in the wrong format.\n");
            return false; 
        }
        return true; 

    }
}
