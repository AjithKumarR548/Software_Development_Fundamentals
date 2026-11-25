import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Shop shop = new Shop();
        shop.seed();
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n=== Online Shopping Application ===\n");

        while (true) {
            printMenu();
            System.out.print("Choose (? = menu): ");
            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("0")) {
                System.out.println();
                System.out.println("Thank you for shopping. Goodbye!");
                break;
            }

            if (choice.equals("?")) {
                continue;
            }

            try {
                switch (choice) {
                    case "1":
                        register(shop, scanner);
                        break;
                    case "2":
                        login(shop, scanner);
                        break;
                    case "3":
                        search(shop, scanner);
                        break;
                    case "4":
                        addToCart(shop, scanner);
                        break;
                    case "5":
                        viewCart(shop);
                        break;
                    case "6":
                        removeFromCart(shop, scanner);
                        break;
                    case "7":
                        checkout(shop, scanner);
                        break;
                    case "8":
                        viewOrders(shop);
                        break;
                    case "9":
                        cancelOrder(shop, scanner);
                        break;
                    case "l":
                        shop.logout();
                        System.out.println("Logged out.");
                        break;
                    default:
                        throw new ShopException("Invalid menu choice");
                }
            } catch (ShopException error) {
                System.out.println("ERROR: " + error.getMessage());
            }

            System.out.println();
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("1. Register       2. Login            3. Search         4. Add to Cart");
        System.out.println("5. View Cart      6. Remove Item      7. Checkout       8. Orders");
        System.out.println("9. Cancel Order   L. Logout           0. Exit");
        System.out.println();
    }

    private static void register(Shop shop, Scanner scanner) {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("E-mail: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Address: ");
        String address = scanner.nextLine();
        shop.register(name, email, password, address);
        System.out.println("Registered. Please log in.");
    }

    private static void login(Shop shop, Scanner scanner) {
        System.out.print("E-mail: ");
        String email = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        String name = shop.login(email, password);
        System.out.println("Welcome, " + name + "!");
    }

    private static void search(Shop shop, Scanner scanner) {
        System.out.print("Keyword (blank = all): ");
        String keyword = scanner.nextLine();
        System.out.print("Category (blank = all): ");
        String category = scanner.nextLine();
        java.util.List<Shop.Product> products = shop.search(
            keyword,
            category.trim().isEmpty() ? null : category
        );

        if (products.isEmpty()) {
            System.out.println("No products found.");
            return;
        }

        System.out.println("ID  Product           Category       Price    Stock");
        for (Shop.Product product : products) {
            System.out.printf(
                "%-3d %-17s %-14s %7.2f %5d%n",
                product.id,
                product.name,
                product.category,
                product.price,
                product.stock
            );
        }
    }

    private static int readNumber(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String text = scanner.nextLine().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException error) {
            throw new ShopException("Enter numbers only where a number is required");
        }
    }

    private static void addToCart(Shop shop, Scanner scanner) {
        int productId = readNumber(scanner, "Product ID: ");
        int quantity = readNumber(scanner, "Quantity: ");
        shop.addToCart(productId, quantity);
        System.out.println("Added to cart.");
    }

    private static void viewCart(Shop shop) {
        Shop.CartSummary summary = shop.cart();
        if (summary.items.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }

        for (Shop.CartItem item : summary.items) {
            System.out.printf(
                "%d. %s x %d = Rs.%.2f%n",
                item.id,
                item.name,
                item.quantity,
                item.price * item.quantity
            );
        }
        System.out.printf(
            "Subtotal %.2f | Discount -%.2f | GST %.2f | TOTAL Rs.%.2f%n",
            summary.subtotal,
            summary.discount,
            summary.gst,
            summary.total
        );
    }

    private static void removeFromCart(Shop shop, Scanner scanner) {
        int productId = readNumber(scanner, "Product ID: ");
        shop.removeFromCart(productId);
        System.out.println("Removed.");
    }

    private static void checkout(Shop shop, Scanner scanner) {
        System.out.print("Payment (CARD/UPI/COD): ");
        String method = scanner.nextLine().toUpperCase();
        String card = "";
        if (method.equals("CARD")) {
            System.out.print("16-digit card number: ");
            card = scanner.nextLine();
        }

        Shop.CheckoutResult result = shop.checkout(method, card);
        System.out.printf(
            "Order #%d placed. Total Rs.%.2f%n",
            result.orderId,
            result.total
        );
    }

    private static void viewOrders(Shop shop) {
        java.util.List<Shop.Order> orders = shop.orders();
        if (orders.isEmpty()) {
            System.out.println("No orders yet.");
            return;
        }
        for (Shop.Order order : orders) {
            System.out.printf(
                "#%d  Rs.%.2f  %s  %s%n",
                order.id,
                order.total,
                order.method,
                order.status
            );
        }
    }

    private static void cancelOrder(Shop shop, Scanner scanner) {
        int orderId = readNumber(scanner, "Order ID: ");
        shop.cancelOrder(orderId);
        System.out.println("Order cancelled. Stock restored.");
    }
}
