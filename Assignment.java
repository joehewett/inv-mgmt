import java.io.*;
import java.sql.*;
import java.util.*; 

import java.util.Properties;

// TO DO 
// Create connection to database 
// Create schema.sql for the tables 
// Get simple data pulling from database
// Get simple create data insertion to database

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
            String opt = readEntry("Enter your choice: ");
            System.out.print("Option chosen is " + opt + "\n"); 
            
            switch (opt) {
                case 1:
                    option1(); 
                    break;
                case 2:
                    //option2(); 
                    break;
                case 3:
                    //option3(); 
                    break;
                case 4:
                    //option4(); 
                    break;
                case 5:
                    //option5(); 
                    break;
                case 6:
                    //option6(); 
                    break;
                case 7:
                    //option7(); 
                    break;
                case 8:
                    //option8(); 
                    break;
                default:
                    // code block
            }
        }

		conn.close();
	}

	/**
	* @param conn An open database connection 
	* @param productIDs An array of productIDs associated with an order
    * @param quantities An array of quantities of a product. The index of a quantity correspeonds with an index in productIDs
	* @param orderDate A string in the form of 'DD-Mon-YY' that represents the date the order was made
	* @param staffID The id of the staff member who sold the order
	*/
	public static void option1(Connection conn, int[] productIDs, int[] quantities, String orderDate, int staffID) {
		// Incomplete - Code for option 1 goes here
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
        System.out.print("(1) In-Store Purchase");
        System.out.print("(2) Collection");
        System.out.print("(3) Delivery");
        System.out.print("(4) Biggest Sellers");
        System.out.print("(5) Reserved Stock");
        System.out.print("(6) Staff Life-Time Success");
        System.out.print("(7) Staff Contribution");
        System.out.print("(8) Employee of the Year");
        System.out.print("(0) Quit");
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

	
}