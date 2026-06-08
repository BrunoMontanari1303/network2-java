package broker.model;
//todos os tipos de mensagens que podem ser trocadas entre cliente e broker

public enum MessageType {

    CREATE_TOPIC,
    SUBSCRIBE,
    UNSUBSCRIBE,
    PUBLISH,
    DELIVER,
    SUCCESS,
    ERROR,

    AUTH_REQUEST,
    AUTH_OK,

    DISCONNECT,
    DOWNLOAD_PENDING,
    DOWNLOAD_OK
}
