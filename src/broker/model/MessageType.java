package broker.model;
//todos os tipos de mensagens que podem ser trocadas entre cliente e broker

public enum MessageType {
    CREATE_TOPIC, //solita a criação de um topico 
    SUBSCRIBE, //solicita a inscrição em um topico 
    UNSUBSCRIBE, //remoção da inscrição de um topico 
    PUBLISH, //publica uma mensagem em um topico 
    DELIVER, //mensagem entregue aos inscritos do topico 
    SUCCESS, //mensagem de sucesso enviado pelo broker 
    ERROR, //mensagem de erro enviado ppelo broker 
    AUTH, //tipo relacionado a autenticação
    DISCONNECT //solicita desconexão

}
