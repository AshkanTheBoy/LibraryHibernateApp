public class Book{
    private long id;
    private String name;
    private int stock;

    private Book(){}

    public Book(String name, int stock){
        this.name = name;
        this.stock = stock;
    }

    public Book(int stock){
        this.name = "UNKNOWN";
        this.stock = stock;
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        this.id = id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public int getStock(){
        return stock;
    }

    public void setStock(int stock){
        this.stock = stock;
    }
}
