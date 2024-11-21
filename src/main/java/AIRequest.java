import java.util.List;

/**
 * Represents the request structure for the AI API.
 */
public class AIRequest {

    private String model;
    private List<Message> messages;

    // Private constructor to enforce the use of the builder
    private AIRequest(Builder builder) {
        this.model = builder.model;
        this.messages = builder.messages;
    }

    // Getters
    public String getModel() {
        return model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Builder class for AIRequest.
     */
    public static class Builder {
        private String model;
        private List<Message> messages;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public AIRequest build() {
            return new AIRequest(this);
        }
    }

    /**
     * Represents a message in the AI request.
     */
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
