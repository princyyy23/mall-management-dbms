import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Server {
    static String dbUrl;

    public static void main(String[] args) throws Exception {
        // read DB_URL from .env
        for (String line : Files.readAllLines(Path.of(".env"))) {
            if (line.startsWith("DB_URL=")) dbUrl = line.substring(7).trim();
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);
        server.createContext("/", ex -> send(ex, "text/html", Files.readString(Path.of("index.html"))));
        server.createContext("/shops", Server::handleShops);
        server.start();
        System.out.println("Running at http://localhost:3000");
    }

    static void handleShops(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String id = ex.getRequestURI().getPath().replace("/shops", "").replace("/", ""); // "" or "5"
        Map<String, String> form = parseForm(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

        try (Connection db = DriverManager.getConnection(dbUrl)) {
            switch (method) {
                case "GET" -> { // READ - list all shops
                    send(ex, "application/json", readShops(db));
                }
                case "POST" -> { // CREATE - add a new shop
                    PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO Shop (shop_name, owner_name, floor_no, rent) VALUES (?, ?, ?, ?)");
                    ps.setString(1, form.get("shop_name"));
                    ps.setString(2, form.get("owner_name"));
                    ps.setString(3, form.get("floor_no"));
                    ps.setString(4, form.get("rent"));
                    ps.executeUpdate();
                    send(ex, "text/plain", "Shop added");
                }
                case "PUT" -> { // UPDATE - modify an existing shop
                    PreparedStatement ps = db.prepareStatement(
                        "UPDATE Shop SET shop_name = ?, owner_name = ?, floor_no = ?, rent = ? WHERE shop_id = ?");
                    ps.setString(1, form.get("shop_name"));
                    ps.setString(2, form.get("owner_name"));
                    ps.setString(3, form.get("floor_no"));
                    ps.setString(4, form.get("rent"));
                    ps.setString(5, id);
                    ps.executeUpdate();
                    send(ex, "text/plain", "Shop updated");
                }
                case "DELETE" -> { // DELETE - remove a shop
                    PreparedStatement ps = db.prepareStatement("DELETE FROM Shop WHERE shop_id = ?");
                    ps.setString(1, id);
                    ps.executeUpdate();
                    send(ex, "text/plain", "Shop deleted");
                }
            }
        } catch (SQLException e) {
            System.out.println("DB error: " + e.getMessage());
            send(ex, 500, "text/plain", "Error: " + e.getMessage());
        }
    }

    // turn all rows of Shop into a JSON array
    static String readShops(Connection db) throws SQLException {
        StringBuilder json = new StringBuilder("[");
        ResultSet rs = db.createStatement().executeQuery("SELECT * FROM Shop");
        while (rs.next()) {
            if (json.length() > 1) json.append(",");
            json.append(String.format(
                "{\"shop_id\":%d,\"shop_name\":\"%s\",\"owner_name\":\"%s\",\"floor_no\":%d,\"rent\":\"%s\"}",
                rs.getInt("shop_id"), rs.getString("shop_name"), rs.getString("owner_name"),
                rs.getInt("floor_no"), rs.getString("rent")));
        }
        return json.append("]").toString();
    }

    // "shop_name=Zara&rent=500" -> {shop_name=Zara, rent=500}
    static Map<String, String> parseForm(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return map;
    }

    static void send(HttpExchange ex, String type, String body) throws IOException {
        send(ex, 200, type, body);
    }

    static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}
