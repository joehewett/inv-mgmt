import java.io.*;
import java.sql.*;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Properties;

class Assignment {

    private static String readEntry(String prompt) {
        try {
            StringBuffer buffer = new StringBuffer();
            System.out.print("\n" + prompt);
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

            switch (opt) {
                case "0":
                    conn.close();
                    System.out.println("Exiting Inventory Management...");
                    return;
                case "1":
                    handleOption1(conn);
                    break;
                case "2":
                    // option2();
                    break;
                case "3":
                    // option3();
                    break;
                case "4":
                    // option4();
                    break;
                case "5":
                    // option5();
                    break;
                case "6":
                    // option6();
                    break;
                case "7":
                    // option7();
                    break;
                case "8":
                    option8(conn, 1000);

                    break;
                default:
                    System.out.println("That's not a valid option. Press a number between 0 and 8.");
                    // code block
            }
        }
    }

    /**
     * @param conn An open database connection Handle the selection of option1,
     *             fetching arrays of products and their quantities related info
     */
    private static void handleOption1(Connection conn) {
        ArrayList<Integer> productIDs = new ArrayList();
        ArrayList<Integer> quantities = new ArrayList();
        String saleDate = "01-jan-00";
        int staffID = 0;
        String opt = "";

        while (!(opt.equals("n") || opt.equals("N"))) {
            opt = readEntry("Enter a product ID: ");
            productIDs.add(Integer.valueOf(opt));
            opt = readEntry("Enter the quantity sold: ");
            quantities.add(Integer.valueOf(opt));
            opt = readEntry("Is there another product in the order?: ");
        }

        saleDate = readEntry("Enter the date sold: ");
        staffID = Integer.valueOf(readEntry("Enter your staff ID: "));

        int[] productIDsArray = new int[productIDs.size()];
        int[] quantitiesArray = new int[quantities.size()];

        // Convert from our array list to an array that the option1 function can use
        for (int i = 0; i < productIDs.size(); i++) {
            productIDsArray[i] = productIDs.get(i);
            quantitiesArray[i] = quantities.get(i);
        }

        option1(conn, productIDsArray, quantitiesArray, saleDate, staffID);
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
        Boolean successfulOrder = false;
        try {
            // Turn off auto-commiting in case the order fails
            conn.setAutoCommit(false);

            // Create a new order
            int id = insertOrder(conn, "In-Store", 1, getSQLDate(orderDate), staffID);
            if (id < 1) {
                throw new Exception("Failed to add a new order to orders table, aborting order...");
            }

            // For each product in the order, create an order_products row
            for (int i = 0; i < productIDs.length; i++) {
                insertOrderProduct(conn, id, productIDs[i], quantities[i]);
            }

            // Now that insertOrderProduct has handled the stock reductions, print the new
            // stock levels
            displayInventory(conn, productIDs);

            // If we've made it this far without an SQLException then we know the order has
            // completed successfully so commit changes to db
            conn.setAutoCommit(true);
            successfulOrder = true;

        } catch (SQLException e) {
            // Rollback the SQL since the order failed
            System.out.println(
                    "\nAn error occurred while trying to add the order:\n- Ensure there is enough stock in the inventory\n- Ensure the staff ID number exists\n");
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
                    System.err.format("Could not roll back to safe state:\nSQL State: %s\n%s", e.getSQLState(),
                            e.getMessage());
                }
            }
        }

    }

    /**
     * @param conn An open database connection Handle the selection of option1,
     *             fetching arrays of products and their quantities related info
     */
    private static void handleOption2(Connection conn) {
        ArrayList<Integer> productIDs = new ArrayList();
        ArrayList<Integer> quantities = new ArrayList();
        String saleDate = "01-Jan-00";
        int staffID = 0;
        String collectionDate = ""; 
        String firstName, lastName = "";

        String opt = "";

        while (!(opt.equals("n") || opt.equals("N"))) {
            opt = readEntry("Enter a product ID: ");
            productIDs.add(Integer.valueOf(opt));
            opt = readEntry("Enter the quantity sold: ");
            quantities.add(Integer.valueOf(opt));
            opt = readEntry("Is there another product in the order?: ");
        }

        saleDate = readEntry("Enter the date sold: ");
        collectionDate = readEntry("Enter the date of collection:"); 
        firstName = readEntry("Enter the first name of the collector: "); 
        lastName = readEntry("Enter the last name of the collector: "); 
        staffID = Integer.valueOf(readEntry("Enter your staff ID: "));

        int[] productIDsArray = new int[productIDs.size()];
        int[] quantitiesArray = new int[quantities.size()];

        // Convert from our array list to an array that the option1 function can use
        for (int i = 0; i < productIDs.size(); i++) {
            productIDsArray[i] = productIDs.get(i);
            quantitiesArray[i] = quantities.get(i);
        }

        option2(conn, productIDsArray, quantitiesArray, saleDate, collectionDate, firstName, lastName, staffID);
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
     */
    public static int insertOrder(Connection conn, String orderType, Integer orderCompleted, java.sql.Date orderPlaced,
            Integer staffID) {
        int newOrderID = -1;
        try {
            CallableStatement stmt = conn.prepareCall("{ call insertOrder(?, ?, ?, ?) }");
            stmt.registerOutParameter(1, Types.INTEGER);
            stmt.setString(1, orderType);
            stmt.setDate(2, orderPlaced);
            stmt.setInt(3, orderCompleted);
            stmt.setInt(4, staffID);
            stmt.execute();
            newOrderID = stmt.getInt(1);
            stmt.close();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        } catch (ParseException e) {
            System.out.println("Unable to parse date input - usage: dd-MMM-yy e.g. 17-Nov-18");
        }
        return null;
    }
    
	/**
	* @param conn An open database connection 
	* @param productID A productID associated with an order
    * @param quantities A quantity of a product - we want to check if we have this quantity in stock of the given product
    */
    public static boolean sufficientStock(Connection conn, int productID, int quantity) {
        Boolean isSufficient = false;
        try {
            // Call a user function sufficientStock that we have defined in our schema
            // The function simply returns a boolean that lets us know if we can 
            CallableStatement stmt = conn.prepareCall("{ call sufficientStock(?, ?) }");
            stmt.registerOutParameter(1, Types.BOOLEAN);
            stmt.setInt(1, productID);
            stmt.setInt(2, quantity);
            stmt.execute();
            isSufficient = stmt.getBoolean(1);
            stmt.close();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isSufficient;
    }

	/**
	* @param conn An open database connection 
	* @param productID A productID associated with an order
    * @param quantities A quantity of a product - we want to check if we have this quantity in stock of the given product
    */
    public static void reduceStock(Connection conn, int productID, int quantity) {
        try {
            // Call a user function sufficientStock that we have defined in our schema
            // The function simply returns a boolean that lets us know if we can 
            CallableStatement stmt = conn.prepareCall("call reduceStock(?, ?)");
            stmt.setInt(1, productID);
            stmt.setInt(2, quantity);
            stmt.execute();
            System.out.println("Stock Reduced...\n");
            stmt.close();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/**
	* @param conn An open database connection 
	* @param productIDs An array of productIDs associated with an order
    * @param quantities An array of quantities of a product. The index of a quantity correspeonds with an index in productIDs
	* @param orderDate A string in the form of 'DD-Mon-YY' that represents the date the order was made
	* @param collectionDate A string in the form of 'DD-Mon-YY' that represents the date the order will be collected
	* @param fName The first name of the customer who will collect the order
	* @param LName The last name of the customer who will collect the order
	* @param staffID The id of the staff member who sold the order
	*/
	public static void option2(Connection conn, int[] productIDs, int[] quantities, String orderDate, String collectionDate, String fName, String LName, int staffID) {
		// Incomplete - Code for option 2 goes here
        Boolean successfulOrder = false;
        try {
            // Turn off auto-commiting in case the order fails
            conn.setAutoCommit(false);

            // Create a new order
            int id = insertOrder(conn, "Collection", 0, getSQLDate(orderDate), staffID);
            if (id < 1) {
                throw new Exception("Failed to add a new order to orders table, aborting order...");
            }

            // For each product in the order, create an order_products row
            for (int i = 0; i < productIDs.length; i++) {
                insertOrderProduct(conn, id, productIDs[i], quantities[i]);
            }

            // Insert the data into the collections table
            insertCollection(id, fName, LName, collectionDate);
            
            // Now that insertOrderProduct has handled the stock reductions, print the new
            // stock levels
            displayInventory(conn, productIDs);

            // If we've made it this far without an SQLException then we know the order has
            // completed successfully so commit changes to db
            conn.setAutoCommit(true);
            successfulOrder = true;

        } catch (SQLException e) {
            // Rollback the SQL since the order failed
            System.out.println(
                    "\nAn error occurred while trying to add the order:\n- Ensure there is enough stock in the inventory\n- Ensure the staff ID number exists\n");
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
                    System.err.format("Could not roll back to safe state:\nSQL State: %s\n%s", e.getSQLState(),
                            e.getMessage());
                }
            }
        }
	}

	/**
	* @param conn An open database connection 
	* @param productIDs An array of productIDs associated with an order
    * @param quantities An array of quantities of a product. The index of a quantity correspeonds with an index in productIDs
	* @param orderDate A string in the form of 'DD-Mon-YY' that represents the date the order was made
	* @param deliveryDate A string in the form of 'DD-Mon-YY' that represents the date the order will be delivered
	* @param fName The first name of the customer who will receive the order
	* @param LName The last name of the customer who will receive the order
	* @param house The house name or number of the delivery address
	* @param street The street name of the delivery address
	* @param city The city name of the delivery address
	* @param staffID The id of the staff member who sold the order
	*/
	public static void option3(Connection conn, int[] productIDs, int[] quantities, String orderDate, String deliveryDate, String fName, String LName,
				   String house, String street, String city, int staffID) {
		// Incomplete - Code for option 3 goes here
	}

	/**
	* @param conn An open database connection 
	*/
	public static void option4(Connection conn) {
		// Incomplete - Code for option 4 goes here
	}

	/**
	* @param conn An open database connection 
	* @param date The target date to test collection deliveries against
	*/
	public static void option5(Connection conn, String date) {
		// Incomplete - Code for option 5 goes here
	}

	/**
	* @param conn An open database connection 
	*/
	public static void option6(Connection conn) {
		// Incomplete - Code for option 6 goes here
	}

	/**
	* @param conn An open database connection 
	*/
	public static void option7(Connection conn) {
		// Incomplete - Code for option 7 goes here
	}

	/**
	* @param conn An open database connection 
	* @param year The target year we match employee and product sales against
	*/
	public static void option8(Connection conn, int year) {
        // Incomplete - Code for option 8 goes here
	}

    public static Connection getConnection(){
        Properties props = new Properties();
        props.setProperty("socketFactory", "org.newsclub.net.unix.AFUNIXSocketFactory$FactoryArg");

        props.setProperty("socketFactoryArg",System.getenv("PGHOST") + "/.s.PGSQL.5432");
        Connection conn;
        try{
          conn = DriverManager.getConnection("jdbc:postgresql://localhost/deptstore", props);
          return conn;
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
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
        System.out.print("(9) Show Inventory\n");
        System.out.print("(0) Quit\n");
    }
	
/*    public static Connection getConnection() {
        //This version of getConnection uses ports to connect to the server rather than sockets
        //If you use this method, you should comment out the above getConnection method, and comment out lines 19 and 21
        String user = "me";
        String passwrd = "mypassword";
        Connection conn;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException x) {
            System.out.println("Driver could not be loaded");
        }

        try {
            conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:15432/deptstore?user="+ user +"&password=" + passwrd);

            return conn;
        } catch(SQLException e) {
                e.printStackTrace();
            System.out.println("Error retrieving connection");
            return null;
        }

    }*/

        /** 
        int foovalue = 500;
        PreparedStatement st = conn.prepareStatement("DELETE FROM mytable WHERE columnfoo = ?");
        st.setInt(1, foovalue);
        int rowsDeleted = st.executeUpdate();
        System.out.println(rowsDeleted + " rows deleted");
        st.close();

        String selectStatement = "SELECT * FROM inventory";


            PreparedStatement preparedStatement = conn.prepareStatement(selectStatement);
            ResultSet players = preparedStatement.executeQuery();

            while (players.next()) {
                System.out.println("product id: " + players.getString("ProductID"));
                System.out.println("product desc: " + players.getString("ProductDesc"));
                System.out.println("product quantity: " + players.getString("ProductStockAmount"));
            }

            // Always close statements, result sets and connections after use
            // Otherwise you run out of available open cursors!
            preparedStatement.close();
            players.close();

        */
	
}