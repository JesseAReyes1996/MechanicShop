import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * This class defines a simple embedded SQL utility class that is designed to
 * work with PostgreSQL JDBC drivers.
 *
 */

public class MechanicShop{
	//For retrieving VIN from customer without any cars when inserting a service request
	private boolean insertingSR = false;
	private ArrayList<String> SRList = new ArrayList<String>();

	//public int getCustomerID (MechanicShop esql) throws SQLException {
	public int getCustomerID (MechanicShop esql) throws SQLException {
		String customerID = "";
		String lname = ""; //Customer's last name
		String input = ""; //For getting user input
		boolean chosen = false;

		//Get customer's info
		System.out.print("Enter customer's last name: ");
		try{
			lname = in.readLine();
		}catch(IOException e){
			e.printStackTrace();
		}

		//Select customers whose name matches the given last name
		String query = "SELECT TRIM(fname), phone, address, id FROM Customer WHERE LOWER(lname) = LOWER('" + lname + "')";
		List<List<String>> potentialCustomers = esql.executeQueryAndReturnResult(query);

		//If no customer exists with given last name
		if(potentialCustomers.size() == 0){
			System.out.println("Sorry, we couldn't find any customers with that last name");
			System.out.println("Would you like to add a new customer?");
			System.out.println("1. Yes\n2. No");

			try{
				input = in.readLine();
			}catch(IOException e){
				e.printStackTrace();
			}

			if(Integer.parseInt(input) == 1){
				AddCustomer(esql);
				query = "SELECT id FROM Customer ORDER BY id DESC LIMIT 1";
				List<List<String>> custID = esql.executeQueryAndReturnResult(query);
				customerID = custID.get(0).get(0);
				return Integer.parseInt(customerID);
			}
			else if(Integer.parseInt(input) == 2){
				//do nothing
				return -1; //Error code
			}
		}

		//If more than one customer with the same last name
		else if(potentialCustomers.size() > 1){
			//Print out all the different customers with the same last name
			for(int i = 0; i < potentialCustomers.size(); ++i){
				System.out.println(Integer.toString(i + 1) + ". First Name: " + potentialCustomers.get(i).get(0) + ", Phone Number: " + potentialCustomers.get(i).get(1) + ", Address: " + potentialCustomers.get(i).get(2));
			}
			//Choose the current customer
			System.out.println("Choose a customer");
			chosen = false;
			while(!chosen){
				try{
					input = in.readLine();//TODO Input error checking
				}catch(IOException e){
					e.printStackTrace();
				}
				if(Integer.parseInt(input) > potentialCustomers.size() || Integer.parseInt(input) <= 0){
					System.out.println("Invalid input, enter a number from 1-" + Integer.toString(potentialCustomers.size()));
				}
				else{
					chosen = true;
				}
			}
			//id of chosen customer
			customerID = potentialCustomers.get(Integer.parseInt(input) - 1).get(3);
		}

		//If only one customer exists with the given last name
		else if(potentialCustomers.size() == 1){
			//Get the id of the customer
			customerID = potentialCustomers.get(0).get(3);
		}
		return Integer.parseInt(customerID);
	}

	//reference to physical database connection
	private Connection _connection = null;
	static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	public MechanicShop(String dbname, String dbport, String user, String passwd) throws SQLException {
		System.out.print("Connecting to database...");
		try{
			// constructs the connection URL
			String url = "jdbc:postgresql://localhost:" + dbport + "/" + dbname;
			System.out.println ("Connection URL: " + url + "\n");

			// obtain a physical connection
	        this._connection = DriverManager.getConnection(url, user, passwd);
	        System.out.println("Done");
		}catch(Exception e){
			System.err.println("Error - Unable to Connect to Database: " + e.getMessage());
	        System.out.println("Make sure you started postgres on this machine");
	        System.exit(-1);
		}
	}

	/**
	 * Method to execute an update SQL statement.  Update SQL instructions
	 * includes CREATE, INSERT, UPDATE, DELETE, and DROP.
	 *
	 * @param sql the input SQL string
	 * @throws java.sql.SQLException when update failed
	 * */
	public void executeUpdate (String sql) throws SQLException {
		// creates a statement object
		Statement stmt = this._connection.createStatement ();

		// issues the update instruction
		stmt.executeUpdate (sql);

		// close the instruction
	    stmt.close ();
	}//end executeUpdate

	/**
	 * Method to execute an input query SQL instruction (i.e. SELECT).  This
	 * method issues the query to the DBMS and outputs the results to
	 * standard out.
	 *
	 * @param query the input query string
	 * @return the number of rows returned
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	public int executeQueryAndPrintResult (String query) throws SQLException {
		//creates a statement object
		Statement stmt = this._connection.createStatement ();

		//issues the query instruction
		ResultSet rs = stmt.executeQuery (query);

		/*
		 *  obtains the metadata object for the returned result set.  The metadata
		 *  contains row and column info.
		 */
		ResultSetMetaData rsmd = rs.getMetaData ();
		int numCol = rsmd.getColumnCount ();
		int rowCount = 0;

		//iterates through the result set and output them to standard out.
		boolean outputHeader = true;
		while (rs.next()){
			if(outputHeader){
				for(int i = 1; i <= numCol; i++){
					System.out.print(rsmd.getColumnName(i) + "\t");
			    }
			    System.out.println();
			    outputHeader = false;
			}
			for (int i=1; i<=numCol; ++i)
				System.out.print (rs.getString (i) + "\t");
			System.out.println ();
			++rowCount;
		}//end while
		stmt.close ();
		return rowCount;
	}

	/**
	 * Method to execute an input query SQL instruction (i.e. SELECT).  This
	 * method issues the query to the DBMS and returns the results as
	 * a list of records. Each record in turn is a list of attribute values
	 *
	 * @param query the input query string
	 * @return the query result as a list of records
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	public List<List<String>> executeQueryAndReturnResult (String query) throws SQLException {
		//creates a statement object
		Statement stmt = this._connection.createStatement ();

		//issues the query instruction
		ResultSet rs = stmt.executeQuery (query);

		/*
		 * obtains the metadata object for the returned result set.  The metadata
		 * contains row and column info.
		*/
		ResultSetMetaData rsmd = rs.getMetaData ();
		int numCol = rsmd.getColumnCount ();
		int rowCount = 0;

		//iterates through the result set and saves the data returned by the query.
		boolean outputHeader = false;
		List<List<String>> result  = new ArrayList<List<String>>();
		while (rs.next()){
			List<String> record = new ArrayList<String>();
			for (int i=1; i<=numCol; ++i)
				record.add(rs.getString (i));
			result.add(record);
		}//end while
		stmt.close ();
		return result;
	}//end executeQueryAndReturnResult

	/**
	 * Method to execute an input query SQL instruction (i.e. SELECT).  This
	 * method issues the query to the DBMS and returns the number of results
	 *
	 * @param query the input query string
	 * @return the number of rows returned
	 * @throws java.sql.SQLException when failed to execute the query
	 */
	public int executeQuery (String query) throws SQLException {
		//creates a statement object
		Statement stmt = this._connection.createStatement ();

		//issues the query instruction
		ResultSet rs = stmt.executeQuery (query);

		int rowCount = 0;

		//iterates through the result set and count nuber of results.
		if(rs.next()){
			rowCount++;
		}//end while
		stmt.close ();
		return rowCount;
	}

	/**
	 * Method to fetch the last value from sequence. This
	 * method issues the query to the DBMS and returns the current
	 * value of sequence used for autogenerated keys
	 *
	 * @param sequence name of the DB sequence
	 * @return current value of a sequence
	 * @throws java.sql.SQLException when failed to execute the query
	 */

	public int getCurrSeqVal(String sequence) throws SQLException {
		Statement stmt = this._connection.createStatement ();

		ResultSet rs = stmt.executeQuery (String.format("Select currval('%s')", sequence));
		if (rs.next()) return rs.getInt(1);
		return -1;
	}

	/**
	 * Method to close the physical connection if it is open.
	 */
	public void cleanup(){
		try{
			if (this._connection != null){
				this._connection.close ();
			}//end if
		}catch (SQLException e){
	         // ignored.
		}//end try
	}//end cleanup

	/**
	 * The main execution method
	 *
	 * @param args the command line arguments this inclues the <mysql|pgsql> <login file>
	 */
	public static void main (String[] args) {
		if (args.length != 3) {
			System.err.println (
				"Usage: " + "java [-classpath <classpath>] " + MechanicShop.class.getName () +
		            " <dbname> <port> <user>");
			return;
		}//end if

		MechanicShop esql = null;

		try{
			System.out.println("(1)");

			try {
				Class.forName("org.postgresql.Driver");
			}catch(Exception e){

				System.out.println("Where is your PostgreSQL JDBC Driver? " + "Include in your library path!");
				e.printStackTrace();
				return;
			}

			System.out.println("(2)");
			String dbname = args[0];
			String dbport = args[1];
			String user = args[2];

			esql = new MechanicShop (dbname, dbport, user, "");

			boolean keepon = true;
			while(keepon){
				System.out.println("MAIN MENU");
				System.out.println("---------");
				System.out.println("1. AddCustomer");
				System.out.println("2. AddMechanic");
				System.out.println("3. AddCar");
				System.out.println("4. InsertServiceRequest");
				System.out.println("5. CloseServiceRequest");
				System.out.println("6. ListCustomersWithBillLessThan100");
				System.out.println("7. ListCustomersWithMoreThan20Cars");
				System.out.println("8. ListCarsBefore1995With50000Milles");
				System.out.println("9. ListKCarsWithTheMostServices");
				System.out.println("10. ListCustomersInDescendingOrderOfTheirTotalBill");
				System.out.println("11. < EXIT");

				/*
				 * FOLLOW THE SPECIFICATION IN THE PROJECT DESCRIPTION
				 */
				switch (readChoice()){
					case 1: AddCustomer(esql); break;
					case 2: AddMechanic(esql); break;
					case 3: AddCar(esql); break;
					case 4: InsertServiceRequest(esql); break;
					case 5: CloseServiceRequest(esql); break;
					case 6: ListCustomersWithBillLessThan100(esql); break;
					case 7: ListCustomersWithMoreThan20Cars(esql); break;
					case 8: ListCarsBefore1995With50000Milles(esql); break;
					case 9: ListKCarsWithTheMostServices(esql); break;
					case 10: ListCustomersInDescendingOrderOfTheirTotalBill(esql); break;
					case 11: keepon = false; break;
				}
			}
		}catch(Exception e){
			System.err.println (e.getMessage ());
		}finally{
			try{
				if(esql != null) {
					System.out.print("Disconnecting from database...");
					esql.cleanup ();
					System.out.println("Done\n\nBye!");
				}//end if
			}catch(Exception e){
				// ignored.
			}
		}
	}

	public static int readChoice() {
		int input;
		// returns only if a correct value is given.
		do {
			System.out.print("Please make your choice: ");
			try { // read the integer, parse it and break.
				input = Integer.parseInt(in.readLine());
				break;
			}catch (Exception e) {
				System.out.println("Your input is invalid!");
				continue;
			}//end try
		}while (true);
		return input;
	}//end readChoice

	public static void AddCustomer(MechanicShop esql){//1
		String fname, lname, phone = "", address;
		int id; //Customer PRIMARY KEY

		boolean correctFormat = false;

		try{
			//auto increment Customer ID
			String query = "SELECT id FROM Customer ORDER BY id DESC LIMIT 1";
			List<List<String>> custID = esql.executeQueryAndReturnResult(query);
			id = Integer.parseInt(custID.get(0).get(0)) + 1;
			//Get customer info
			System.out.print("Customer's first name: ");
			fname = in.readLine();
			System.out.print("Customer's last name: ");
			lname = in.readLine();
			System.out.print("Customer's phone number (xxx)xxx-xxxx: ");
			while(!correctFormat){
				phone = in.readLine();
				//TODO Check if chars entered are numeric
				if(phone.length() != 13 || phone.charAt(0) != '(' || phone.charAt(4) != ')' || phone.charAt(8) != '-'){
					System.out.print("Enter the customer's phone number in the format (xxx)xxx-xxxx: ");
				}
				else{
					correctFormat = true;
				}
			}
			System.out.print("Customer's address: ");
			address = in.readLine();
			//Store info in DB
			query = "INSERT INTO Customer(id, fname, lname, phone, address) VALUES (" + id + ", '" + fname + "', '" + lname + "', '" + phone + "', '" + address + "')";
			esql.executeUpdate(query);
		}catch(Exception e){
			System.err.println(e.getMessage());
		}

	}

	public static void AddMechanic(MechanicShop esql){//2
		String fname, lname, experience = "-1";
		int id; //Mechanic PRIMARY KEY

		//To check if a proper value for 'experience' has been given
		boolean correctDomain = false;
		int checkYears;

		try{
			//auto increment Mechanic ID
			String query = "SELECT id FROM Mechanic ORDER BY id DESC LIMIT 1";
			List<List<String>> mechID = esql.executeQueryAndReturnResult(query);
			id = Integer.parseInt(mechID.get(0).get(0)) + 1;
			//Get mechanic info
			System.out.print("Mechanic's first name: ");
			fname = in.readLine();
			System.out.print("Mechanic's last name: ");
			lname = in.readLine();
			System.out.print("Mechanic's years of experience: ");
			//Check if experience is in its declared domain
			while(!correctDomain){
				try{
					experience = in.readLine();
					checkYears = Integer.parseInt(experience);
				}catch(NumberFormatException e){
					System.out.println("Value entered must be an integer");
					System.out.print("Mechanic's years of experience: ");
					continue;
				}
				if(checkYears < 0 || checkYears >= 100){
					System.out.print("Enter a value greater than or equal to 0 or less than 100: ");
				}
				else{
					correctDomain = true;
				}
			}
			//Store info in DB
			query = "INSERT INTO Mechanic(id, fname, lname, experience) VALUES (" + id + ", '" + fname + "', '" + lname + "', '" + experience + "')";
			esql.executeUpdate(query);
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void AddCar(MechanicShop esql){//3
		//VIN is Car PRIMARY KEY
		String make, model, year = "-1", vin;

		//To check if a proper value for 'year' has been given
		boolean correctDomain = false;
		int checkYear;

		//Information about the car's owner
		int custID = -1; //id of customer adding a car

		String input = ""; //For getting user input
		boolean correctVIN = false;

		try{
			//When adding a new car for InsertServiceRequest, don't use getCustomerID()
			if(!esql.insertingSR){
				//Get ID of customer adding a car
				custID = esql.getCustomerID(esql);
				if(custID == -1){
					System.out.println("ERROR: Invalid customer ID");
					return;
				}
			}
			else{
				custID = Integer.parseInt(esql.SRList.get(0));
				esql.SRList.remove(0);
			}

			//Get car's info
			System.out.print("Car's VIN: ");
			while(true){
				vin = in.readLine();
				if(vin.length() != 16){
					System.out.println("VIN must be 16 characters");
					System.out.print("Car's VIN: ");
				}
				else{
					break;
				}
			}

			System.out.print("Car's make: ");
			make = in.readLine();
			System.out.print("Car's model: ");
			model = in.readLine();
			System.out.print("Car's year: ");
			//Check if year is in its declared domain
			while(!correctDomain){
				try{
					year = in.readLine();
					checkYear = Integer.parseInt(year);
				}catch(NumberFormatException e){
					System.out.println("Value entered must be an integer");
					System.out.print("Car's year: ");
					continue;
				}
				if(checkYear < 1970){
					System.out.print("Car's year must be greater than or equal to 1970: ");
				}
				else{
					correctDomain = true;
				}
			}
			//Store info in DB
			String query = "INSERT INTO Car(vin, make, model, year) VALUES ('" + vin + "', '" + make + "', '" + model + "', " + Integer.parseInt(year) + ")";
			esql.executeUpdate(query);

			//auto increment Owns ID
			query = "SELECT ownership_id FROM Owns ORDER BY ownership_id DESC LIMIT 1";
			List<List<String>> ownsID = esql.executeQueryAndReturnResult(query);
			int ownershipID = Integer.parseInt(ownsID.get(0).get(0)) + 1;

			query = "INSERT INTO Owns(ownership_id, customer_id, car_vin) VALUES (" + ownershipID + ", " + custID + ", '" + vin + "')";
			esql.executeUpdate(query);

			//When adding a new car for InsertServiceRequest, return the car's VIN
			if(esql.insertingSR){
				esql.SRList.add(vin);
				esql.insertingSR = false;
			}
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void InsertServiceRequest(MechanicShop esql){//4
		String input = ""; //For getting user input
		boolean chosen = false;
		int rid = 0; //Service Request id
		int custID = -1; //id of customer initiating the service request
		String vin = ""; //VIN of car needing the service
		String date = ""; //Date brought in for service
		int odometer = 0; //Number of miles on the cars odometer
		String complaint = ""; //The reason for bringing the car in for service

		try{
			custID = esql.getCustomerID(esql);
			if(custID == -1){
				System.out.println("ERROR: Invalid customer ID");
				return;
			}

			//Get list of VINs from cars belonging to the customer
			String query = "SELECT car_vin FROM Owns WHERE customer_id = " + Integer.toString(custID);
			List<List<String>> vins = esql.executeQueryAndReturnResult(query);

			//Print list of cars to potentially service
			List<List<String>> cars;
			if(vins.size() > 0){
				System.out.println("Choose which car needs to be serviced:");
				for(int i = 0; i < vins.size(); ++i){
					query = "SELECT * FROM Car WHERE vin = '" + vins.get(i).get(0) + "'";
					cars = esql.executeQueryAndReturnResult(query);
					//Print car year make model, VIN
					System.out.println(Integer.toString(i + 1) + ": " + cars.get(0).get(3) + " " + cars.get(0).get(1) + " " + cars.get(0).get(2) + ", VIN: " + cars.get(0).get(0));
				}
				System.out.println();
				chosen = false;
				while(!chosen){
					input = in.readLine();
					if(Integer.parseInt(input) > vins.size() || Integer.parseInt(input) <= 0){
						System.out.print("Invalid input, enter a number from 1-" + Integer.toString(vins.size()));
					}
					else{
						chosen = true;
						System.out.println();
					}
				}
				//VIN of chosen car
				vin = vins.get(Integer.parseInt(input) - 1).get(0);
			}
			else{
				//Add a new car and return the car's VIN
				esql.insertingSR = true;
				esql.SRList.add(Integer.toString(custID));
				AddCar(esql);
				vin = esql.SRList.get(0);
				esql.SRList.remove(0);
			}

			//Get the miles from the odometer
			System.out.println("Enter the amount of miles displayed on the odometer: ");
			chosen = false;
			while(!chosen){
				input = in.readLine();
				if(Integer.parseInt(input) <= 0){
					System.out.println("Invalid input, enter a number greater than 0");
				}
				else{
					chosen = true;
					odometer = Integer.parseInt(input);
				}
			}

			//auto increment Service_Request ID
			query = "SELECT rid FROM Service_Request ORDER BY rid DESC LIMIT 1";
			List<List<String>> servicerequestID = esql.executeQueryAndReturnResult(query);
			rid = Integer.parseInt(servicerequestID.get(0).get(0)) + 1;

			//Get the current date
			Date getDate = new Date();
			date = new SimpleDateFormat("MM-dd-yyyy hh:mm").format(getDate);

			//Get the customer's complaint
			System.out.println("Enter a brief description of the problem: ");
			complaint = in.readLine();
			complaint = complaint.replace("'","''");
			System.out.println();

			//Execute the query
			query = "INSERT INTO Service_Request(rid, customer_id, car_vin, date, odometer, complain) VALUES (" + rid + ", " + custID + ", '" + vin + "', '" + date + "', " + odometer + ", '" + complaint + "')";
			esql.executeUpdate(query);

		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void CloseServiceRequest(MechanicShop esql) throws Exception{//5
		int mechID = -1;
		int custID = -1;

		String date = "";

		int bill = 0; //The customer's bill for the service request

		String input = ""; //For getting user input

		try{
			//Get mechanic ID
			System.out.print("Enter the mechanic's ID: ");
			input = in.readLine();

			//Check to see if the mechanic is an employee
			String query = "SELECT id FROM Mechanic WHERE id = " + input;
			List<List<String>> mechanic = esql.executeQueryAndReturnResult(query);

			//If no mechanic exists with given ID
			if(mechanic.size() == 0){
				System.out.println("ERROR: Invalid mechanic ID");
				return;
			}
			mechID = Integer.parseInt(mechanic.get(0).get(0));

			//Get customer ID
			custID = esql.getCustomerID(esql);
			if(custID == -1)
			{
				System.out.println("ERROR: Invalid customer ID");
				return;
			}

			//Get cars that are being serviced
			query = "SELECT car_vin, rid, complain FROM Service_Request WHERE customer_id = " + Integer.toString(custID);
			List<List<String>> service_requests = esql.executeQueryAndReturnResult(query);

			//Customer does not have any open service requests
			if(service_requests.size() == 0){
				System.out.println("Customer currently has no open service requests");
				return;
			}

			//Select which request to close
			System.out.println("Select the service request to close:"); //TODO Do not show requests that have already been closed
			for(int i = 0; i < service_requests.size(); ++i){
				System.out.println((i + 1) + ". " + service_requests.get(i).get(0) + " " + service_requests.get(i).get(1) + " " + service_requests.get(i).get(2));
			}

			boolean chosen = false;
			while(!chosen){
				input = in.readLine();
				if(Integer.parseInt(input) > service_requests.size() || Integer.parseInt(input) <= 0){
					System.out.print("Invalid input, enter a number from 1-" + Integer.toString(service_requests.size()));
				}
				else{
					chosen = true;
					System.out.println();
				}
			}

			//Get the ID of the request to be closed
			int rid = Integer.parseInt(service_requests.get((Integer.parseInt(input) - 1)).get(1));

			//auto increment closed request ID
			query = "SELECT wid FROM Closed_Request ORDER BY wid DESC LIMIT 1";
			List<List<String>> closed_request_ID = esql.executeQueryAndReturnResult(query);
			int wid = Integer.parseInt(closed_request_ID.get(0).get(0)) + 1;

			//Get date request is being closed on
			Date getDate = new Date();
			date = new SimpleDateFormat("MM-dd-yyyy hh:mm").format(getDate);

			//Get the mechanic's comment
			System.out.println("Enter a brief comment on the outcome of the service request: ");
			String comment = in.readLine();
			comment = comment.replace("'","''");
			System.out.println();

			//Charge the bill
			chosen = false;
			while(!chosen){
				System.out.print("Enter the total bill for the service request: ");
				input = in.readLine();
				if(Integer.parseInt(input) <= 0){
					System.out.println("Invalid input, the bill must be greater than 0");
				}
				else{
					chosen = true;
					bill = Integer.parseInt(input);
					System.out.println();
				}
			}

			//Insert the closed request
			query = "INSERT INTO Closed_Request(wid, rid, mid, date, comment, bill) VALUES (" + wid + ", " + rid + ", " + mechID + ", '" + date + "', '" + comment + "', " + bill + ")";
			esql.executeUpdate(query);

			//Delete the open service request cannot delete because of foreign key constraints
			//query = "DELETE FROM Service_Request WHERE rid = " + Integer.toString(rid);
			//esql.executeUpdate(query);

		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void ListCustomersWithBillLessThan100(MechanicShop esql){//6
		try{
			//Select closed requests with bills < $100
			String query = "SELECT date, comment, bill FROM Closed_Request WHERE bill < 100";
			List<List<String>> customers = esql.executeQueryAndReturnResult(query);
			for(int i = 0; i < customers.size(); ++i){
				System.out.println((i + 1) + ". " + customers.get(i).get(0) + " " + customers.get(i).get(1) + " " + customers.get(i).get(2));
			}
			System.out.println();
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void ListCustomersWithMoreThan20Cars(MechanicShop esql){//7
		try{
			//Select customers with more than 20 cars
			String query = "SELECT TRIM(fname), lname FROM Customer, (SELECT customer_id, COUNT(customer_id) as car_num FROM Owns GROUP BY customer_id HAVING COUNT(customer_id) > 20) AS O WHERE O.customer_id = id";
			List<List<String>> customers = esql.executeQueryAndReturnResult(query);
			for(int i = 0; i < customers.size(); ++i){
				System.out.println((i + 1) + ". " + customers.get(i).get(0) + " " + customers.get(i).get(1));
			}
			System.out.println();
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void ListCarsBefore1995With50000Milles(MechanicShop esql){//8
		try{
			//Select cars built before 1995 with less than 50000 miles
			String query = "SELECT DISTINCT make, model, year FROM Car AS C, Service_Request AS S WHERE year < 1995 AND S.car_vin = C.vin AND S.odometer < 50000";
			List<List<String>> cars = esql.executeQueryAndReturnResult(query);
			for(int i = 0; i < cars.size(); ++i){
				System.out.println((i + 1) + ". " + cars.get(i).get(2) + " " + cars.get(i).get(0) + " " + cars.get(i).get(1));
			}
			System.out.println();
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void ListKCarsWithTheMostServices(MechanicShop esql){//9
		try{
			//Get k from user
			System.out.println("Enter the amount of cars you would like to display: ");
			String input; //For getting user input
			boolean chosen = false;
			int k = 0; //Amount of cars to list
			while(!chosen){
				input = in.readLine();
				if(Integer.parseInt(input) <= 0){
					System.out.println("Invalid input, enter a number greater than 0");
				}
				else{
					chosen = true;
					k = Integer.parseInt(input);
				}
			}

			//Select k cars with the highest number of service requests
			String query = "SELECT TRIM(make), TRIM(model), R.creq FROM Car AS C, (SELECT car_vin, COUNT(rid) AS creq FROM Service_Request GROUP BY car_vin ) AS R WHERE R.car_vin = C.vin ORDER BY R.creq DESC LIMIT " + Integer.toString(k);
			List<List<String>> cars = esql.executeQueryAndReturnResult(query);
			for(int i = 0; i < cars.size(); ++i){
				System.out.println((i + 1) + ". " + cars.get(i).get(0) + " " + cars.get(i).get(1) + " " + cars.get(i).get(2));
			}
			System.out.println();
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

	public static void ListCustomersInDescendingOrderOfTheirTotalBill(MechanicShop esql){//10
		try{
			//Select customers in descending order by their total bill
			String query = "SELECT TRIM(C.fname), TRIM(C.lname), Total FROM Customer AS C, (SELECT sr.customer_id, SUM(CR.bill) AS Total FROM Closed_Request AS CR, Service_Request AS SR WHERE CR.rid = SR.rid GROUP BY SR.customer_id) AS A WHERE C.id=A.customer_id ORDER BY A.Total DESC";
			List<List<String>> customers = esql.executeQueryAndReturnResult(query);
			for(int i = 0; i < customers.size(); ++i){
				System.out.println((i + 1) + ". " + customers.get(i).get(0) + " " + customers.get(i).get(1) + " " + customers.get(i).get(2));
			}
			System.out.println();
		}catch(Exception e){
			System.err.println(e.getMessage());
		}
	}

}
