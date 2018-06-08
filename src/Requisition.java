public enum Requisition
{
    // mensagem de boas vindas, despedida, nenhuma operacao realizada e erro de tipo de classe
    BONJOUR, SALUT, AU_REVOIR, NOP, BYE, CLASS_ERROR,

    // pedidos e respostas trocadas pelo controle e modelo para realizar o login
    CONTROL_REQUEST_LOGIN, MODEL_RESPONSE_LOGIN,
    // pedidos e respostas trocadas pelo controle e visao para realizar o login
    VIEW_REQUEST_LOGIN, CONTROL_RESPONSE_LOGIN,

    // pedidos e respostas trocadas pelo controle e modelo para realizar a criacao de usuario
    CONTROL_REQUEST_CREATE_USER, MODEL_RESPONSE_CREATE_USER,
    // pedidos e respostas trocadas pela visao e modelo para criar um novo usuario
    VIEW_REQUEST_CREATE_USER, CONTROL_RESPONSE_CREATE_USER,

    // pedidos e respostas trocadas pelo controle e modelo para a criacao de um novo item
    CONTROL_REQUEST_CREATE_ITEM, MODEL_RESPONSE_CREATE_ITEM,
    // pedidos e respostas trocadas pela visao e controle para criar um novo item
    VIEW_REQUEST_CREATE_ITEM, CONTROL_RESPONSE_CREATE_ITEM,

    // pedidos e respostas trocadas pelo controle e modelo para a listagem dos itens disponiveis
    CONTROL_REQUEST_LIST_ITEM, MODEL_RESPONSE_LIST_ITEM,
    // pedidos e respostas trocadas pela visao e controle para listagem dos items disponiveis
    VIEW_REQUEST_LIST_ITEM, CONTROL_RESPONSE_LIST_ITEM,

    //pedidos e respostas trocadas pela visao e controle para criacao de uma nova sala
    VIEW_REQUEST_CREATE_ROOM, CONTROL_RESPONSE_CREATE_ROOM,

    //pedidos e respostas trocados pelo controle com o modelo para alterar o item para em leilao ou nao
    CONTROL_REQUEST_CHANGE_ITEM_STATE, MODEL_RESPONSE_CHANGE_ITEM_STATE,

    //pedidos e respostas trocados pelo controle e visao para listagem das salas
    VIEW_REQUEST_LIST_ROOM, CONTROL_RESPONSE_LIST_ROOM,

    // pedidos e respostas trocadas pelo controle e visao para entrar em uma sala
    VIEW_REQUEST_AUCTIONEER, CONTROL_RESPONSE_AUCTIONEER,

    // pedidos e respostas trocados entre a visao para entrar na sala
    VIEW_REQUEST_ENTER_ROOM, VIEW_RESPONSE_ENTER_ROOM,

    // pedidos e respostas trocados entre a visao para dar um novo lance
    VIEW_REQUEST_NEW_BID, VIEW_RESPONSE_NEW_BID,

    VIEW_REQUEST_CLOSE_ROOM, CONTROL_RESPONSE_CLOSE_ROOM,

    CONTROL_REQUEST_SAVE_RESULT, MODEL_RESPONSE_SAVE_RESULT
}
