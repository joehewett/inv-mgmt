import java.io.*;
import java.sql.*;
import java.util.*; 

import java.util.Properties;

// TO DO 
// Create connection to database 
// Create schema.sql for the tables 
// Get simple data pulling from database
// Get simple create data insertion to database

// alternatively, complete option 1
 

class Assignment {

	private static String readEntry(String prompt) {
		try {
			StringBuffer buffer = new StringBuffer();
			System.out.print(prompt);
			System.out.flush();
			int c = System.in.read();
			while(c != '\n' && c != -1) {
				buffer.append((char)c);
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

        while (opt != "0") {
            printMenu(); 
            opt = readEntry("Enter your choice: ");
            
            switch (opt) {
                case "1":
                    handleOption1(conn); 
                    break;
                case "2":
                    //option2(); 
                    break;
                case "3":
                    //option3(); 
                    break;
                case "4":
                    //option4(); 
                    break;
                case "5":
                    //option5(); 
                    break;
                case "6":
                    //option6(); 
                    break;
                case "7":
                    //option7(); 
                    break;
                case "8":
                    //option8(); 
                    break;
                default:
                    // code block
            }
        }

		conn.close();
	}

    /** 
    * Handle the selection of option1, fetching arrays of products and their quantities related info
    */
    private static String handleOption1(Connection conn) {
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

        for (int i = 0; i < productIDs.size(); i++) {
            productIDsArray[i] = productIDs.get(i); 
            quantitiesArray[i] = quantities.get(i); 
        }
        
        option1(conn, productIDsArray, quantitiesArray, saleDate, staffID); 
        return "";
    }

	/**
	* @param conn An open database connection 
	* @param productIDs An array of productIDs associated with an order
    * @param quantities An array of quantities of a product. The index of a quantity correspeonds with an index in productIDs
	* @param orderDate A string in the form of 'DD-Mon-YY' that represents the date the order was made
	* @param staffID The id of the staff member who sold the order
	*/
	public static void option1(Connection conn, int[] productIDs, int[] quantities, String orderDate, int staffID) {
        // ####SELECT Check that products and their respective quantities are avaialable. 
        // ####UPDATE: If so, reduce stock count in inventory by quantities amount 
        // INSERT: add an InStore, completed order to orders 
        // INSERT: Add a row to order_products for each item in the order
        // INSERT: add a staff order entry to staff_orders

        // Create sequences for increment ID's 
        // Create functions for checking stock count is sufficient, and decreases stock count

        try {
            // Check we can have enough stock in our inventory to process this order. If not, don't update and return with error. 
            for (int i = 0; i < productIDs.length; i++) {
                if (!sufficientStock(conn, productIDs[i], quantities[i])) {
                    System.out.println("Unable to process order - insufficient stock in inventory for product ID " + productIDs[i]);
                    return; 
                }
            }
            System.out.println("Sufficient stock for all items :) "); 
            
            // If we have determined we have sufficient stock to process the order, then update our inventory to reflect our sold stock 
            for (int i = 0; i < productIDs.length; i++) {
                reduceStock(conn, productIDs[i], quantities[i]);
            }


        //} catch (SQLException e) {
        //    System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            CallableStatement upperFunc = conn.prepareCall("{ call sufficientStock(?, ?) }");
            upperFunc.registerOutParameter(1, Types.BOOLEAN);
            upperFunc.setInt(1, productID);
            upperFunc.setInt(2, quantity);
            upperFunc.execute();
            isSufficient = upperFunc.getBoolean(1);
            upperFunc.close();
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
            CallableStatement upperFunc = conn.prepareCall("{call reduceStock(?, ?)}");
            upperFunc.setInt(1, productID);
            upperFunc.setInt(2, quantity);
            System.out.println("AssigneD quantities\n");
            upperFunc.execute();
            System.out.println("Executed...\n");
            upperFunc.close();
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