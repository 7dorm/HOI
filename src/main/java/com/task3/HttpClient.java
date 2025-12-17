package com.task3;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Класс для выполнения HTTP запросов к серверу
 */
public class HttpClient {
    private final java.net.http.HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;
    
    public HttpClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Выполняет GET запрос к указанному пути и возвращает ответ сервера
     */
    public ServerResponse fetch(String path) throws Exception {
        String url = baseUrl + path;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP error code: " + response.statusCode() + " for path: " + path);
        }
        
        // Парсим JSON ответ
        String json = response.body();
        return objectMapper.readValue(json, ServerResponse.class);
    }
}

