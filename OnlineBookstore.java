package Capabl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// User class to represent a user of the online bookstore
class User {
    private String username;
    private String password;
    private String email;

    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public String getEmail() {
        return email;
    }
}

// Book class to represent a book in the bookstore's catalog
class Book {
    private String title;
    private String author;
    private double price;
    private int stock;

    public Book(String title, String author, double price, int stock) {
        this.title = title;
        this.author = author;
        this.price = price;
        this.stock = stock;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public double getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}

// ShoppingCart class to represent a user's shopping cart
class ShoppingCart {
    private Map<Book, Integer> items;

    public ShoppingCart() {
        this.items = new HashMap<>();
    }

    public void addItem(Book book, int quantity) {
        if (items.containsKey(book)) {
            items.put(book, items.get(book) + quantity);
        } else {
            items.put(book, quantity);
        }
    }

    public void removeItem(Book book, int quantity) {
        if (items.containsKey(book)) {
            int currentQuantity = items.get(book);
            if (quantity >= currentQuantity) {
                items.remove(book);
            } else {
                items.put(book, currentQuantity - quantity);
            }
        }
    }

    public Map<Book, Integer> getItems() {
        return items;
    }

    public double getTotal() {
        double total = 0;
        for (Book book : items.keySet()) {
            total += book.getPrice() * items.get(book);
        }
        return total;
    }
}

// Order class to represent a user's order
class Order {
    private User user;
    private Map<Book, Integer> items;
    private double total;

    public Order(User user, Map<Book, Integer> items) {
        this.user = user;
        this.items = items;
        this.total = 0;
        for (Book book : items.keySet()) {
            total += book.getPrice() * items.get(book);
        }
    }

    public User getUser() {
        return user;
    }

    public Map<Book, Integer> getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }

    public String generateInvoice() {
        StringBuilder invoice = new StringBuilder();
        invoice.append("Invoice for user ").append(user.getUsername()).append(":\n");
        for (Book book : items.keySet()) {
            invoice.append(book.getTitle()).append(" by ").append(book.getAuthor()).append(": ").append(items.get(book)).append(" x $").append(book.getPrice()).append("\n");
        }
        invoice.append("Total: $").append(total).append("\n");
        return invoice.toString();
    }
}

// OnlineBookstore class to represent the online bookstore
public class OnlineBookstore {
    private List<Book> catalog;
    private Map<String, User> users;
    private Map<User, ShoppingCart> shoppingCarts;
    private Map<User, List<Order>> orderHistory;
    private Map<Book, Integer> inventory;

    public OnlineBookstore() {
        this.catalog = new ArrayList<>();
        this.users = new HashMap<>();
        this.shoppingCarts = new ConcurrentHashMap<>();
        this.orderHistory = new ConcurrentHashMap<>();
        this.inventory = new ConcurrentHashMap<>();
    }

    public void addBook(Book book) {
        catalog.add(book);
        inventory.put(book, book.getStock());
    }

    public void removeBook(Book book) {
        catalog.remove(book);
        inventory.remove(book);
    }

    public List<Book> getCatalog() {
        return catalog;
    }

    public void registerUser(String username, String password, String email) {
        User user = new User(username, password, email);
        users.put(username, user);
        shoppingCarts.put(user, new ShoppingCart());
        orderHistory.put(user, new ArrayList<>());
    }

    public User login(String username, String password) {
        User user = users.get(username);
        if (user != null && user.checkPassword(password)) {
            return user;
        }
        return null;
    }

    public synchronized void addToCart(User user,Book book, int quantity) {
        ShoppingCart cart = shoppingCarts.get(user);
        cart.addItem(book, quantity);
    }

    public synchronized void removeFromCart(User user, Book book, int quantity) {
        ShoppingCart cart = shoppingCarts.get(user);
        cart.removeItem(book, quantity);
    }

    public synchronized void checkout(User user) {
        ShoppingCart cart = shoppingCarts.get(user);
        Map<Book, Integer> items = cart.getItems();
        for (Book book : items.keySet()) {
            int quantity = items.get(book);
            int stock = inventory.get(book);
            if (quantity > stock) {
                throw new RuntimeException("Insufficient stock for book " + book.getTitle());
            }
            inventory.put(book, stock - quantity);
        }
        Order order = new Order(user, items);
        orderHistory.get(user).add(order);
        shoppingCarts.put(user, new ShoppingCart());
        System.out.println(order.generateInvoice());
    }

    public static void main(String[] args) {
        OnlineBookstore bookstore = new OnlineBookstore();

        // Add some books to the catalog
        Book book1 = new Book("The Great Gatsby", "F. Scott Fitzgerald", 10.99, 20);
        Book book2 = new Book("To Kill a Mockingbird", "Harper Lee", 12.99, 15);
        Book book3 = new Book("1984", "George Orwell", 8.99, 30);
        bookstore.addBook(book1);
        bookstore.addBook(book2);
        bookstore.addBook(book3);

        // Register some users
        bookstore.registerUser("alice", "password1", "alice@example.com");
        bookstore.registerUser("bob", "password2", "bob@example.com");

        // Login as Alice and add some books to her cart
        User alice = bookstore.login("alice", "password1");
        bookstore.addToCart(alice, book1, 2);
        bookstore.addToCart(alice, book2, 1);

        // Login as Bob and add some books to his cart
        User bob = bookstore.login("bob", "password2");
        bookstore.addToCart(bob, book2, 3);
        bookstore.addToCart(bob, book3, 2);

        // Checkout for Alice and Bob
        bookstore.checkout(alice);
        bookstore.checkout(bob);

        // Print the remaining inventory
        System.out.println("Remaining inventory:");
        for (Book book : bookstore.inventory.keySet()) {
            int stock = bookstore.inventory.get(book);
            System.out.println(book.getTitle() + " by " + book.getAuthor() + ": " + stock);
        }
    }
}