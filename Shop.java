import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shop {
    public static final double GST = 0.18;
    public static final double DISCOUNT_LIMIT = 5000;
    public static final double DISCOUNT_RATE = 0.10;
    public static final int MAX_FAILED_LOGINS = 3;

    private final Map<Integer, Product> products = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();
    private final Map<Integer, CartItem> cart = new HashMap<>();
    private final List<Order> orders = new ArrayList<>();
    private Integer currentUserId = null;
    private int nextUserId = 1;
    private int nextOrderId = 1;

    public static class Product {
        public final int id;
        public final String name;
        public final String category;
        public final double price;
        public int stock;

        public Product(int id, String name, String category, double price, int stock) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
        }
    }

    public static class User {
        public final int id;
        public final String name;
        public final String email;
        public final String password;
        public final String address;
        public int failedLogins;

        public User(int id, String name, String email, String password, String address) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
            this.address = address;
            this.failedLogins = 0;
        }
    }

    public static class CartItem {
        public final int id;
        public final String name;
        public final double price;
        public final int quantity;

        public CartItem(int id, String name, double price, int quantity) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }

    public static class CartSummary {
        public final List<CartItem> items;
        public final double subtotal;
        public final double discount;
        public final double gst;
        public final double total;

        public CartSummary(
            List<CartItem> items,
            double subtotal,
            double discount,
            double gst,
            double total
        ) {
            this.items = items;
            this.subtotal = subtotal;
            this.discount = discount;
            this.gst = gst;
            this.total = total;
        }
    }

    public static class Order {
        public final int id;
        public final int userId;
        public final double total;
        public final String method;
        public String status;
        public final Map<Integer, Integer> items;

        public Order(
            int id,
            int userId,
            double total,
            String method,
            Map<Integer, Integer> items
        ) {
            this.id = id;
            this.userId = userId;
            this.total = total;
            this.method = method;
            this.status = "PLACED";
            this.items = items;
        }
    }

    public static class CheckoutResult {
        public final int orderId;
        public final double total;

        public CheckoutResult(int orderId, double total) {
            this.orderId = orderId;
            this.total = total;
        }
    }

    public void seed() {
        addProduct("Wireless Mouse", "Electronics", 799, 25);
        addProduct("USB-C Cable", "Electronics", 349, 60);
        addProduct("Running Shoes", "Fashion", 2999, 10);
        addProduct("Cotton T-Shirt", "Fashion", 599, 40);
        addProduct("Python Cookbook", "Books", 850, 15);
        addProduct("Laptop 15-inch", "Electronics", 45999, 3);
    }

    public void addProduct(String name, String category, double price, int stock) {
        int id = products.size() + 1;
        if (!Double.isFinite(price) || price <= 0 || stock < 0) {
            throw new ShopException("Price must be > 0 and stock >= 0");
        }
        products.put(id, new Product(id, name, category, price, stock));
    }

    public void register(String name, String email, String password) {
        register(name, email, password, "Not provided", false);
    }

    public void register(String name, String email, String password, String address) {
        register(name, email, password, address, true);
    }

    private void register(String name, String email, String password, String address, boolean requireAddress) {
        if (name == null || name.trim().isEmpty()) {
            throw new ShopException("Name cannot be empty");
        }
        if (email == null || !email.matches("[\\w.+-]+@[\\w-]+\\.[\\w.]+")) {
            throw new ShopException("Invalid e-mail format");
        }
        if (password == null || password.length() < 6 || !password.matches(".*\\d.*")) {
            throw new ShopException("Password needs 6+ chars including a digit");
        }
        if (requireAddress && (address == null || address.trim().isEmpty())) {
            throw new ShopException("Address cannot be empty");
        }

        String normalizedEmail = email.toLowerCase();
        if (users.containsKey(normalizedEmail)) {
            throw new ShopException("E-mail already registered");
        }
        users.put(
            normalizedEmail,
            new User(nextUserId++, name.trim(), normalizedEmail, password, address.trim())
        );
    }

    public String login(String email, String password) {
        requireEmailPasswordInput(email, password);
        String normalizedEmail = email.toLowerCase();
        User user = users.get(normalizedEmail);
        if (user == null) {
            throw new ShopException("Invalid e-mail or password");
        }
        if (user.failedLogins >= MAX_FAILED_LOGINS) {
            throw new ShopException("Account locked after 3 failed attempts");
        }
        if (!user.password.equals(password)) {
            user.failedLogins++;
            if (user.failedLogins >= MAX_FAILED_LOGINS) {
                throw new ShopException("Account locked after 3 failed attempts");
            }
            throw new ShopException("Invalid e-mail or password");
        }

        user.failedLogins = 0;
        currentUserId = user.id;
        return user.name;
    }

    private void requireEmailPasswordInput(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null) {
            throw new ShopException("Invalid e-mail or password");
        }
    }

    public void logout() {
        currentUserId = null;
    }

    public List<Product> search(String keyword, String category) {
        List<Product> result = new ArrayList<>();
        String text = keyword == null ? "" : keyword.toLowerCase();
        for (Product product : products.values()) {
            boolean matchesName = product.name.toLowerCase().contains(text);
            boolean matchesCategory = category == null ||
                category.isEmpty() ||
                product.category.equals(category);
            if (matchesName && matchesCategory) {
                result.add(product);
            }
        }
        result.sort((left, right) -> Integer.compare(left.id, right.id));
        return result;
    }

    public void addToCart(int productId, int quantity) {
        requireLogin();
        if (quantity <= 0) {
            throw new ShopException("Quantity must be a positive whole number");
        }
        Product product = products.get(productId);
        if (product == null) {
            throw new ShopException("Product not found");
        }

        CartItem existing = cart.get(productId);
        int oldQuantity = existing == null ? 0 : existing.quantity;
        int nextQuantity = oldQuantity + quantity;
        if (nextQuantity > product.stock) {
            throw new ShopException(
                "Only " + product.stock + " unit(s) of " + product.name + " in stock"
            );
        }
        cart.put(
            productId,
            new CartItem(productId, product.name, product.price, nextQuantity)
        );
    }

    public void removeFromCart(int productId) {
        requireLogin();
        if (!cart.containsKey(productId)) {
            throw new ShopException("Item not in cart");
        }
        cart.remove(productId);
    }

    public CartSummary cart() {
        requireLogin();
        List<CartItem> items = new ArrayList<>(cart.values());
        items.sort((left, right) -> Integer.compare(left.id, right.id));
        if (items.isEmpty()) {
            return new CartSummary(items, 0, 0, 0, 0);
        }

        double subtotal = round(items.stream()
            .mapToDouble(item -> round(item.price * item.quantity))
            .sum());
        double discount = subtotal > DISCOUNT_LIMIT
            ? round(subtotal * DISCOUNT_RATE)
            : 0;
        double gst = round((subtotal - discount) * GST);
        double total = round(subtotal - discount + gst);
        return new CartSummary(items, subtotal, discount, gst, total);
    }

    public CheckoutResult checkout(String method, String card) {
        requireLogin();
        CartSummary summary = cart();
        if (summary.items.isEmpty()) {
            throw new ShopException("Cart is empty");
        }

        String normalizedMethod = method == null ? "" : method.toUpperCase();
        if (!normalizedMethod.equals("CARD") &&
            !normalizedMethod.equals("UPI") &&
            !normalizedMethod.equals("COD")) {
            throw new ShopException("Unsupported payment method");
        }
        if (normalizedMethod.equals("CARD") &&
            (card == null || !card.matches("\\d{16}"))) {
            throw new ShopException("Card number must be 16 digits");
        }

        Map<Integer, Integer> orderItems = new HashMap<>();
        for (CartItem item : summary.items) {
            Product product = products.get(item.id);
            if (item.quantity > product.stock) {
                throw new ShopException(product.name + " went out of stock");
            }
            orderItems.put(item.id, item.quantity);
        }
        for (CartItem item : summary.items) {
            products.get(item.id).stock -= item.quantity;
        }

        int orderId = nextOrderId++;
        orders.add(new Order(orderId, currentUserId, summary.total, normalizedMethod, orderItems));
        cart.clear();
        return new CheckoutResult(orderId, summary.total);
    }

    public List<Order> orders() {
        requireLogin();
        List<Order> result = new ArrayList<>();
        for (Order order : orders) {
            if (order.userId == currentUserId) {
                result.add(order);
            }
        }
        return result;
    }

    public void cancelOrder(int orderId) {
        requireLogin();
        for (Order order : orders) {
            if (order.id == orderId && order.userId == currentUserId) {
                if (!order.status.equals("PLACED")) {
                    throw new ShopException("Order already cancelled");
                }
                for (Map.Entry<Integer, Integer> item : order.items.entrySet()) {
                    products.get(item.getKey()).stock += item.getValue();
                }
                order.status = "CANCELLED";
                return;
            }
        }
        throw new ShopException("Order not found");
    }

    public Product product(int id) {
        return products.get(id);
    }

    private void requireLogin() {
        if (currentUserId == null) {
            throw new ShopException("Please log in first");
        }
    }

    private double round(double value) {
        return Math.round((value + Math.ulp(value)) * 100.0) / 100.0;
    }
}
