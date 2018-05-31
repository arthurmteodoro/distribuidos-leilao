public enum Requisition
{
    BONJOUR, AU_REVOIR, NOP, CLASS_ERROR,
    // pedidos e respostas trocadas pelo controle e modelo para realizar o login
    CONTROL_REQUEST_LOGIN, MODEL_RESPONSE_LOGIN,
    // pedidos e respostas trocadas pelo controle e visao para realizar o login
    VIEW_REQUEST_LOGIN, CONTROL_RESPONSE_LOGIN,

    // pedidos e respostas trocadas pelo controle e modelo para realizar a criacao de usuario
    CONTROL_REQUEST_CREATE_USER, MODEL_RESPONSE_CREATE_USER,
    // pedidos e respostas trocadas pela visao e modelo para criar um novo usuario
    VIEW_REQUEST_CREATE_USER, CONTROL_RESPONSE_CREATE_USER,
}
