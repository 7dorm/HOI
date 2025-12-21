import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Простой тестовый HTTP-сервер для проверки Task 3
 * Эмулирует поведение Task_J3-server.jar
 */
public class TestServer {
    private static final Map<String, Node> graph = new HashMap<>();
    private static final Random random = new Random();
    
    static class Node {
        String message;
        List<String> successors;
        
        Node(String message, List<String> successors) {
            this.message = message;
            this.successors = successors;
        }
    }
    
    public static void main(String[] args) throws IOException {
        String studentId = args.length > 0 ? args[0] : "TestUser";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        
        // Создаем тестовый граф путей
        initializeGraph(studentId);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new CrawlerHandler());
        server.setExecutor(null);
        server.start();
        
        System.out.println("Тестовый сервер запущен на порту " + port);
        System.out.println("Student ID: " + studentId);
        System.out.println("Нажмите Ctrl+C для остановки");
    }
    
    private static void initializeGraph(String studentId) {
        // Создаем простой граф для тестирования
        // Корневой путь "/"
        graph.put("/", new Node(
            "Message from root",
            Arrays.asList("path1", "path2", "path3")
        ));
        
        // Путь "/path1"
        graph.put("/path1", new Node(
            "Message from path1",
            Arrays.asList("subpath1", "subpath2")
        ));
        
        // Путь "/path2"
        graph.put("/path2", new Node(
            "Message from path2",
            Arrays.asList("subpath3")
        ));
        
        // Путь "/path3"
        graph.put("/path3", new Node(
            "Message from path3",
            Collections.emptyList()
        ));
        
        // Подпути
        graph.put("/path1/subpath1", new Node(
            "Message from subpath1",
            Collections.emptyList()
        ));
        
        graph.put("/path1/subpath2", new Node(
            "Message from subpath2",
            Collections.emptyList()
        ));
        
        graph.put("/path2/subpath3", new Node(
            "Message from subpath3",
            Collections.emptyList()
        ));
    }
    
    static class CrawlerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Имитируем задержку сервера (0-12 секунд как в задании)
            // Используем меньшую задержку для тестирования (0-5 секунд)
            int delay = random.nextInt(5000);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Node node = graph.get(path);
            
            if (node == null) {
                // Путь не найден
                String response = "{\"message\":\"Path not found\",\"successors\":[]}";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }
            
            // Формируем JSON ответ
            StringBuilder json = new StringBuilder();
            json.append("{\"message\":\"").append(node.message).append("\",\"successors\":[");
            
            for (int i = 0; i < node.successors.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(node.successors.get(i)).append("\"");
            }
            
            json.append("]}");
            
            String response = json.toString();
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

