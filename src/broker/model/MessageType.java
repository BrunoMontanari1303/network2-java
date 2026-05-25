package src.broker.model;

public enum MessageType {
    CREATE_TOPIC,
    SUBSCRIBE,
    UNSUBSCRIBE,
    PUBLISH,
    DELIVER,
    SUCCESS,
    ERROR,
    AUTH,
    DISCONNECT
}