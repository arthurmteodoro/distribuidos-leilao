import java.io.Serializable;

public class AppMessage implements Serializable
{
    //mensagens para se apresentar
    final static int BONJOUR = 0;
    final static int AU_REVOIR = 5;

    //identificadores para pedido ao modelo para realizar o login
    final static int MODEL_LOGIN = 1;
    final static int MODEL_LOGIN_OK = 2;
    final static int MODEL_LOGIN_ERROR = 3;

    // identificadores para o modelo criar um novo usuário
    final static int MODEL_CREATE_USER = 6;
    final static int MODEL_CREATE_USER_OK = 7;
    final static int MODEL_CREATE_USER_ERROR = 8;

    //identificadores para o controle realizar o login
    final static int CONTROL_LOGIN = 9;
    final static int CONTROL_LOGIN_OK = 10;
    final static int CONTROL_LOGIN_ERROR = 11;

    // identificadores para o controle criar um novo usuário
    final static int CONTROL_CREATE_USER = 15;
    final static int CONTROL_CREATE_USER_OK = 16;
    final static int CONTROL_CREATE_USER_ERROR = 17;

    //indentificadores para a visao realizar o login
    final static int VISION_LOGIN = 12;
    final static int VISION_LOGIN_OK = 13;
    final static int VISION_LOGIN_ERROR = 14;

    final static int VISION_CREATE_USER = 15;
    final static int VISION_CREATE_USER_OK = 16;
    final static int VISION_CREATE_USER_ERROR = 17;

    // identificador de recebimento de uma mensagem sem utilizar a classe
    final static int CLASS_ERROR = 4;

    public int identifier;
    public Object content;

    public AppMessage(int id, Object content)
    {
        this.identifier = id;
        this.content = content;
    }
}
