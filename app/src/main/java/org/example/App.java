import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

/*
 * An interactive console library database management application.
 * You can:
 *  1 - Create many identical tables, which will hold their own data.
 *  2 - Manipulate each table's contents, such as: adding, editing, deleting or selecting(printing to the console).
 * The database is in-memory, so, all data will be erased after closing this app.
 * The tables' schema is: | BOOK_ID | BOOK_NAME | BOOK_STOCK |
 * The DBMS used is H2.
 */




public class App {

    public static Database database; //single database

    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            database = Database.getDatabase(); //create the database, if none exist
            if (database!=null){
                mainMenuPrompt(br); //start main menu prompt
            }
            database.closeDatabase();//close the sessionFactory
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
   }

   //main menu method
    public static void mainMenuPrompt(BufferedReader br) {
        String input;
        while (true){
            System.out.println("Welcome to the book database\n"+
                    "Available options:\n"+
                    "1 - Select a table as current\n"+
                    "2 - Show all available tables\n"+
                    "3 - Create a new table\n"+
                    "4 - Show selected table name\n"+
                    "5 - Edit current table\n"+
                    "6 - Delete selected table\n"+
                    "0 - Quit\n");
            try {
                input = br.readLine();
            } catch (IOException e){
                System.err.println(e.getMessage());
                continue;
            }
            if (input.equals("1")){
                chooseTablePrompt(br); //select a table as current working and prompt table-specific commands
            } else if (input.equals("2")){
                database.showAllTables(); //show the list of all existing tables ans their row-count
            } else if (input.equals("3")){
                database.addTable(); //create a new empty table
            } else if (input.equals("4")){
                showCurrentTable(); //show currently selected table
            } else if (input.equals("5")) {
                if (!database.getCurrentTable().isBlank()){
                    //skip to table commands, if seleted table exists, so we don't have to select it again
                    startCurrentTablePrompt(br); 
                }
            } else if (input.equals("6")) {
                database.deleteSelectedTable(); //delete current table
            } else if (input.equals("0")){
                break; //quit if "0" is the input
            }
        }
    }

    //select current table method
    public static void chooseTablePrompt(BufferedReader br){
        System.out.println("Enter the name of the table to select it");
        try {
            String input = br.readLine();
            database.setCurrentTable(input); //set table as current
            //if input is empty, currentTable field will stay empty, so need to check for that
            if (!database.getCurrentTable().isBlank()){ 
                startCurrentTablePrompt(br); //go to table-specific command menu
            }
        } catch (IOException e){
            System.err.println(e.getMessage());
        }
    }

    //show currently selected table name
    public static void showCurrentTable(){
        String tableName = database.getCurrentTable();
        if (tableName.isBlank()){
            System.out.println("No table selected");
        } else {
            System.out.println("Current table: "+tableName);
        }
    }

    //table commands menu
    public static void startCurrentTablePrompt(BufferedReader br) {
        String input = "";
        while (!input.equalsIgnoreCase("b")){
            System.out.println("Table |"+database.getCurrentTable() +"|\n"+
                    "Available options:\n"+
                    "1 - Add a new book entry\n"+
                    "2 - Update a book entry\n"+
                    "3 - Delete a book entry\n"+
                    "4 - Select books from the table\n"+
                    "B/b - Go to main menu\n");
            try {
                input = br.readLine();
            } catch (IOException e){
                System.err.println(e.getMessage());
                continue;
            }
            if (input.equals("1")){
                addEntryPrompt(br);//add a new entry to the table
            } else if (input.equals("2")){
                updateEntryPrompt(br);//update an existing entry choosing by it's id
            } else if (input.equals("3")){
                deleteEntryPrompt(br);//delete an existing entry by it's id
            } else if (input.equals("4")){
                selectEntryPrompt(br);//prompt the user for different selecting modes
            }
        }
    }

    //select entries method
    /*
     * Istead of writing many similar sql queries,
     *  just take a common substring and append the selected parts
     */
    public static void selectEntryPrompt(BufferedReader br) {
        String input = "";
        StringBuilder sb = new StringBuilder();
        while (!input.equalsIgnoreCase("b")){
            System.out.println("Available options:\n"+
                    "1 - Select all - default\n"+ //ordering by id-column
                    "2 - Select all - order by book names\n"+
                    "3 - Select all - order by book stock\n"+
                    "B/b - Go back");
            try {
                input = br.readLine();
            } catch (IOException e){
                System.err.println(e.getMessage());
                continue;
            }
            sb.setLength(0);//flush the string builder
            sb.append(database.getSelectAllQuery()); //append the common part of the query
            if (input.equals("1")){
                sb.append(";"); // if default - just end the query
                                // if those two below - append the "order by" part and select the order-mode
                                // ASC/DESC
            } else if (input.equals("2")){
                selectSortingColumn(br,sb,"BOOK_NAME");
            } else if (input.equals("3")){
                selectSortingColumn(br,sb,"BOOK_STOCK");
            }
            //if canceled at any point - the query is not valid, but it's an easy check for the semicolon
            if (!sb.toString().endsWith(";")){
                continue;
            }             
            database.executeSelectQuery(sb.toString());//execute the constructed query
        }
    }

    

    //select the sorting column method
    public static void selectSortingColumn(BufferedReader br, StringBuilder sb, String columnName){
        String order = "";
        sb.append(String.format(" ORDER BY %s", columnName));//append to the string builder
        while (!order.equalsIgnoreCase("c")){
            System.out.println("Select sorting order:\n"+
                    "A/a - Ascending order\n"+
                    "D/d - Descending order\n"+
                    "C/c - Cancel");
            try {
                order = br.readLine();
            } catch (IOException e){
                System.err.println(e.getMessage());
                continue;
            }
            //if sorting order is selected - the query becomes valid
            if (order.equalsIgnoreCase("a")){
                sb.append(" ASC;");
                break;
            } else if (order.equalsIgnoreCase("d")){
                sb.append(" DESC;");
                break;
            }
        }
    }

    //delete table row method
    public static void deleteEntryPrompt(BufferedReader br) {
        String input;
        int id;
        while (true){
            System.out.println("Enter the id of the entry to delete\n"+
                    "It should be a positive integer\n"+
                    "0-2147483647\n"+
                    "B/b - Go back");
            try {
                input = br.readLine();
                if (input.equalsIgnoreCase("b")){
                    break;
                }
            } catch (IOException e){
                System.err.println(e.getMessage());
                continue;
            }
            try {
                id = checkPositiveInteger(input);//check if the id is a positive integer
            } catch (WrongInputException e){
                System.out.println(e.getMessage());
                continue;
            }
            database.deleteEntryById(id);//all good - delete it
        }
    }

    //add a book entry method
    public static void addEntryPrompt(BufferedReader br){
        String input, secondInput;
        while (true){
            System.out.println("Enter the name for the new book entry\n"+
                    "B/b - Go back");
            try {
                input = br.readLine();
                if (input.equalsIgnoreCase("b")){
                    break;
                }
            } catch (IOException e){
                System.err.println(e.getMessage());
                continue;
            }
            while (true){
                System.out.println("Enter the stock for the new book entry\n"+
                        "It should be a positive integer\n"+
                        "0-2147483647\n"+
                        "C/c - Cancel");
                try {
                    secondInput = br.readLine();
                    if (secondInput.equalsIgnoreCase("c")){
                        break;
                    }
                    int stock = checkPositiveInteger(secondInput);//check if second input is a positive integer
                    database.insertEntry(input,stock);//execute sql query
                    break;//back to the name selection
                } catch (WrongInputException | IOException e){
                    System.err.println(e.getMessage());
                }
            }
        }  
    }
    
    //edit the book entry method
    public static void updateEntryPrompt(BufferedReader br) {
        String input, secondInput;
        int intInput;
        while (true){
            System.out.println("Select id of the entry to edit\n"+
                            "It should be a positive integer\n"+
                            "0-2147483647\n"+
                            "B/b - Go back");
            try {
                input = br.readLine();
                if (input.equalsIgnoreCase("b")){
                    break;
                }
                try {
                    intInput = checkPositiveInteger(input);//check if input is a positive integer
                } catch (WrongInputException e){
                    System.err.println(e.getMessage());
                    continue;
                }
                secondInput = "";
                while (!secondInput.equalsIgnoreCase("c")){
                    if (!database.showEntryIfIdExists(intInput)){//true, if row exists
                        break;//otherwise - try again
                    }
                    System.out.println("Available options:\n"+
                            "1 - Edit the name of the entry\n"+
                            "2 - Edit the stock value of the entry\n"+
                            "C/c - Cancel");
                    secondInput = br.readLine();
                    if (secondInput.equals("1")){
                        editEntryName(br,intInput);//choose new name
                    } else if (secondInput.equals("2")){
                        editEntryStock(br,intInput);//or choose new stock
                    }
                }    
            } catch (IOException e){
                System.err.println(e.getMessage());
            }
        }
    }

    //prompt to edit the entry's name
    public static void editEntryName(BufferedReader br, int entryId) throws IOException{
        System.out.println("Enter new entry name\n"+
                "C/c - Cancel");
        String input = br.readLine();
        if (!input.equalsIgnoreCase("c")){
            database.editEntryNameById(input,entryId);//execute the update query
        }
    }

    
    //prompt to edit the entry's stock value
    public static void editEntryStock(BufferedReader br, int entryId) throws IOException{
        String input;
        while (true){
            System.out.println("Enter new entry stock\n"+
                    "It should be a positive integer\n"+
                    "0-2147483647\n"+
                    "C/c - Cancel");
            input = br.readLine();
            if (input.equalsIgnoreCase("c")){
                break;
            }
            try {
                int stock = checkPositiveInteger(input);//check for positive integer
                database.editEntryStockById(stock, entryId);//execute update query
                break;
            } catch (WrongInputException e){
                System.out.println(e.getMessage());
            }
        }
    }

    /*
     * Two methods to check the input:
     *  1- If the input is checked for numeric - only checkNumericValue()
     *  2- If the input is checked for positive integer - use both
     */
    public static int checkPositiveInteger(String input) throws WrongInputException{
		int inputValue = checkNumericValue(input); //check, if the input is an Integer value
		if (inputValue<0){
			System.out.println();
			System.out.println("Wrong input\nIt should be a positive integer");
			throw new WrongInputException(input);
		}
		return inputValue; //return the value if ok
	}

	public static int checkNumericValue(String input) throws WrongInputException{
		try {
			return Integer.parseInt(input); //parse the input here and return as int value
		} catch (NumberFormatException e){
			System.out.println();
			System.out.println("Wrong input\nIt should be a numeric value");
			throw new WrongInputException(input);
		} 
	}
}
