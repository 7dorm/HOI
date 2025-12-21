package com.task3;

import java.util.List;

/**
 * Класс для представления ответа сервера
 */
public class ServerResponse {
    private String message;
    private List<String> successors;
    
    public ServerResponse() {
    }
    
    public ServerResponse(String message, List<String> successors) {
        this.message = message;
        this.successors = successors;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<String> getSuccessors() {
        return successors;
    }
    
    public void setSuccessors(List<String> successors) {
        this.successors = successors;
    }
    
    @Override
    public String toString() {
        return "ServerResponse{" +
                "message='" + message + '\'' +
                ", successors=" + successors +
                '}';
    }
}

