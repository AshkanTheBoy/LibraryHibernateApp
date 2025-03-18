import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.EmptyInterceptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class Database{
    private static Database INSTANCE;//single instance - singleton

    private final Set<String> tables;//i thought that using set's is more efficient, than lists
    private final SessionFactory sessionFactory;//single sessionFactory
    private final TableInterceptor interceptor;//custom interceptor for switching tables

    private int tableIndex = 0;//incrementing index for naming tables
    private String currentTable;//currently selected table field

    private Database() throws SQLException {
        INSTANCE = this;
        tables = new HashSet<>();
        interceptor = new TableInterceptor();
        currentTable = "";
        Configuration cfg = new Configuration();
        cfg.configure("hibernate.cfg.xml");
        sessionFactory = cfg.buildSessionFactory();
    }

    public static Database getDatabase() {
        if (INSTANCE==null){
            try {
                INSTANCE = new Database();
                return INSTANCE;
            } catch (SQLException e){
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    //if set contains the input as table name - select it
    //might've been better to throw an exception instead of just printing a message
    public void setCurrentTable(String tableName){
        if (tables.contains(tableName)){
            currentTable = tableName;
        } else {
            System.out.println("No such table available");
        }
    }

    public void addTable(){
        //DON'T ALLOW USER TO INPUT TABLE NAMES;)
        //just add the incrementing index and set it as our table name
        //might cause trouble for long usage, since the index does not reset when deleting tables
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        try {
            String tableName = "Books_"+tableIndex;
            session.createNativeQuery(getCreateTableQuery(tableName), Book.class).executeUpdate();
            session.getTransaction().commit();
            tables.add(tableName);//add to the set
            tableIndex++;
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
        } finally {
            if (session!=null){
                session.close();
            }
        }
    }

    public void deleteSelectedTable(){
        if (tables.contains(currentTable)){//maybe an unneccessary check, but prevents from deleting "" table name
            Session session = sessionFactory.openSession();
            Transaction transaction = session.beginTransaction();
            try {
                session.createNativeQuery(getDeleteTableQuery(), Book.class).executeUpdate();
                session.getTransaction().commit();
                tables.remove(currentTable);
            } catch (RuntimeException e){
                if (transaction!=null){
                    transaction.rollback();
                }
                System.out.println(e.getMessage());
            } finally {
                if (session!=null){
                    session.close();
                }
            }
        } else {
            System.out.println("This table does not exist");
        }
    }

    public void deleteEntryById(int id){
        Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
        Transaction transaction = session.beginTransaction();
        try {
            Book book = (Book) session.get(Book.class, (long)id);
            session.delete(book);
            transaction.commit();
            System.out.println("Deleted successfully");
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
            System.out.println("Error. Nothing was deleted");
        } finally {
            if (session!=null){
                session.close();
            }
        }
    }

    public void editEntryNameById(String newName, int id){
        Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
        Transaction transaction = session.beginTransaction();
        try {
            Book book = (Book) session.get(Book.class, (long)id);
            book.setName(newName);
            transaction.commit();
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
        } finally {
            if (session!=null){
                session.close();
            }
        }
    }
    
    public void editEntryStockById(int newStock, int id){
        Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
        Transaction transaction = session.beginTransaction();
        try {
            Book book = (Book) session.get(Book.class, (long)id);
            book.setStock(newStock);
            transaction.commit();
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
        } finally {
            if (session!=null){
                session.close();
            }
        }
    }

    public void executeSelectQuery(String query) {
        Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
        Transaction transaction = session.beginTransaction();
        try {
            List<Book> books = session.createNativeQuery(query, Book.class).list();
            for (Book book: books){
                System.out.printf("| %d | %-10s | %-5d |%n",book.getId(),
                    book.getName(),
                    book.getStock());
            }
            transaction.commit();
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
        } finally {
            if (session!=null){
                session.close();
            }
        }
    }

    public String getCurrentTable(){
        if (!currentTable.isBlank()){
            return currentTable;
        } else {
            return "";
        }
    }

    public void showAllTables() {
        for (String table: tables){
            Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
            Transaction transaction = session.beginTransaction();
            try {
                //this method signature is deprecated
                //but I didn't find other solutions.
                //execute count and get the result as list of Object's
                List result = session.createNativeQuery(getSelectRowCountQuery(table)).list();
                //cast the count to Long
                Long count = ((Number)result.get(0)).longValue();
                if (table.equals(currentTable)){
                    System.out.print("   >");//if found selected table - mark it for convenience
                }
                System.out.printf("|%-20s|%-5d|%n",table,count);//get the row count
            } catch (RuntimeException e){
                if (transaction!=null){
                    transaction.rollback();
                }
                System.out.println(e.getMessage());
            } finally {
                if (session!=null){
                    session.close();
                }
            }
        }
    }

    //method to show and entry to the user, before editing it
    public boolean showEntryIfIdExists(int id){
        Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
        Transaction transaction = session.beginTransaction();
        try {
            //Here we get a list of Book type
            List<Book> books = session.createNativeQuery(getSelectByIdQuery(), Book.class).setLong(1,(long)id).list();
            //check if the list contains our book object
            if (books.size()>0){
                Book book = books.get(0);
                System.out.printf("| %d | %-10s | %-5d |%n",book.getId(),
                    book.getName(),
                    book.getStock());
                transaction.commit();
                return true;
            } else {
                System.out.println("Entry not found");
                transaction.commit();
                return false;
            }
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
        } finally {
            if (session!=null){
                session.close();
            }
        }
        return false;
    }

    public void insertEntry(String name, int stock){
        Session session = sessionFactory.withOptions().interceptor(interceptor).openSession();
        Transaction transaction = session.beginTransaction();
        try {
            Book newBook = new Book(name,stock);
            session.persist(newBook);
            transaction.commit();
        } catch (RuntimeException e){
            if (transaction!=null){
                transaction.rollback();
            }
            System.out.println(e.getMessage());
        } finally {
            if (session!=null){
                session.close();
            }
        }
    }

    public String getSelectByIdQuery(){
        return String.format("SELECT * FROM %s WHERE BOOK_ID = ?;",currentTable);
    }

    public String getSelectAllQuery(){
        return String.format("SELECT * FROM %s ",currentTable);
    }

    public String getSelectRowCountQuery(String tableName){
        return String.format("SELECT COUNT(*) AS count FROM %s;",tableName);
    }

    public String getCreateTableQuery(String tableName){
        return String.format("CREATE TABLE IF NOT EXISTS %s ("+
            "BOOK_ID BIGINT PRIMARY KEY AUTO_INCREMENT,"+
            "BOOK_NAME text,"+
            "BOOK_STOCK INTEGER"+
            ");",tableName);
    }

    public String getDeleteTableQuery(){
        return String.format("DROP TABLE IF EXISTS %s;",currentTable);
    }
    
    public void closeDatabase(){
        if (sessionFactory!=null){
            sessionFactory.close();
        }
    }

    /*
     * This interceptor allows me to change queries, so Hibernate sends
     *  data to different tables.
     * The problem is that I no longer can execute queries(select),
     *  since I have no mapping for these tables.
     * HQL is also out of the way - no mapping.
     * So, I guess, anything related to mapping will fail.
    */
    class TableInterceptor extends EmptyInterceptor{
        @Override
        public String onPrepareStatement(String sql){
            if (sql.contains("BOOKS")){
                sql = sql.replace("BOOKS", currentTable);
            }
            return sql;
        }
    }

}
