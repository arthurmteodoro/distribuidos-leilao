import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;

import java.util.ArrayList;
import java.util.List;

// TODO : caso exista mais de um controle, ele tambem vai ter que estar na lista de usuarios que vai participar do leilao para poder manter sua copia ok. Possiveis solucoes: (1) pegar todos os caras que sao controle antes e ja inserilos, (2) ficar trocando mensagem entre o controle para resolver isso, (3) juntas as respostas das salas diferentes

public class Control extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private JChannel channelView;
    private RequestDispatcher dispatcherView;

    private ArrayList<Sala> salas;
    int sala_id = 0;

    public static void main(String[] args)
    {
        try
        {
            new Control().start();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void start() throws Exception
    {
        this.salas = new ArrayList<>();

        // instancia o canal e o despachante do controle
        this.channelControl = new JChannel("auction.xml");
        this.channelControl.setReceiver(this);
        this.dispatcherControl = new RequestDispatcher(this.channelControl, this);

        // instancia o canal e o despachante da visao
        this.channelView = new JChannel("auction.xml");
        this.channelView.setReceiver(this);
        this.dispatcherView = new RequestDispatcher(this.channelView, this);

        // se conecta aos canais do controle e da visao
        this.channelControl.connect("AuctionControlCluster");
        this.channelView.connect("AuctionViewCluster");
        eventloop();
        this.channelControl.close();
        this.channelView.close();
    }

    private void eventloop() throws Exception
    {
        while(true);
    }

    // funcao de interrupcao quando uma nova mensagem e recebida
    @Override
    public Object handle(Message message) throws Exception
    {
        // caso a mensagem nao seja da instancia do pacote de aplicacao padrao
        if(message.getObject() instanceof AppMessage)
        {
            AppMessage messageReceived = (AppMessage) message.getObject(); // faz a conversao para o tipo apropriado
            System.out.println("Messagem do tipo: "+messageReceived.requisition);
            if(messageReceived.requisition == Requisition.VIEW_REQUEST_LOGIN) // caso seja uma requisicao de login da visao
                return login(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_CREATE_USER) // caso seja uma requisicao de criacao de usuario da visao
                return create_user(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_CREATE_ITEM)
                return create_item(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_LIST_ITEM)
                return list_item(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_CREATE_ROOM)
                return create_room(messageReceived, message.getSrc());
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_LIST_ROOM)
                return new AppMessage(Requisition.CONTROL_RESPONSE_LIST_ROOM, this.salas);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_AUCTIONEER)
                return get_leiloeiro(messageReceived);
            else if(messageReceived.requisition == Requisition.BONJOUR)
            {
                Object[] content = (Object[]) messageReceived.content;
                salas.get((int) content[1]).insert_user((String)content[0], message.getSrc());
                return new AppMessage(Requisition.SALUT, null);
            }
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_NEW_BID)
                return recebe_lance(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_CLOSE_ROOM)
                return fecha_leilao(messageReceived);

            return new AppMessage(Requisition.NOP, null); //caso nao seja nenhuma requisicao para o controle
        }
        else // else para mensagem de um tipo invalido
        {
            System.err.println("[ERROR-MODEL] Enviou classe nao reconhecida");
            System.err.flush();
            return new AppMessage(Requisition.CLASS_ERROR, null);
        }
    }

    // funcao para tratamento do pedido de login
    private Object login(AppMessage messageReceived) throws Exception
    {
        String[] userLoginRequest = (String[]) messageReceived.content; // converte o conteudo para o tipo correto

        //cria uma nova mensagem, agora como um pedido do controle para fazer login, passando os dados que veio na mensagem da visao
        AppMessage controlLogin = new AppMessage(Requisition.CONTROL_REQUEST_LOGIN, userLoginRequest,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        // pega a lista de respostas do canal de controle para o pedido de login
        List responses = dispatcherControl.sendRequestMulticast(controlLogin, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        // caso a quantidade de respostas seja 0, responde que o login nao foi realizado com sucesso
        if(responses.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_LOGIN, false);

        int nop_counter = 0; // inicia o contador de nao operacao em 0

        for(Object response : responses) // percorre todos os objetos que veio na resposta
        {
            AppMessage msg = (AppMessage) response; // faz a conversao para o tipo correto

            // se a resposta foi uma resposta vinda do modelo e seu conteudo foi falso, envia uma resposta de falha de login
            if(msg.requisition == Requisition.MODEL_RESPONSE_LOGIN && ((boolean) msg.content) == false)
                return new AppMessage(Requisition.CONTROL_RESPONSE_LOGIN, false);
            // caso a resposta foi uma nao execucao de operacao, incrementa o contador de nop
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        // caso todos as respostas foi nop, envia que a resposta do login foi nop
        if(nop_counter == responses.size())
            return new AppMessage(Requisition.NOP, null);

        // caso contrario, responde que o login foi realizo com sucesso
        return new AppMessage(Requisition.CONTROL_RESPONSE_LOGIN, true);
    }

    // funcao para criacao de usuario
    private Object create_user(AppMessage messageReceived) throws Exception
    {
        // faz a conversao para o tipo da mensagem
        String[] userCreateRequest = (String[]) messageReceived.content;

        // cria uma nova mensagem como um pedido do controle para criar um novo usuario
        AppMessage controlCreate = new AppMessage(Requisition.CONTROL_REQUEST_CREATE_USER, userCreateRequest,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        // pega todos os resultados vindos do canal
        List responsesCreateUser = dispatcherControl.sendRequestMulticast(controlCreate, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        // se a quantidade de respostas for 0, envia que a resposta do controle para a criacao de um novo usuario falhou
        if(responsesCreateUser.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_USER, false);

        int nop_counter = 0;

        for(Object response : responsesCreateUser)
        {
            AppMessage msg = (AppMessage) response;

            // caso a mensagem seja do tipo resposta do modelo para criacao de um novo usuario e seu conteudo for falso, responde que deu erro
            if(msg.requisition == Requisition.MODEL_RESPONSE_CREATE_USER && ((boolean) msg.content == false))
                return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_USER, false);
            // caso for nenhuma operacao realiza, incrementa a contagem
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        // caso so veio respostas nop, responde que nao realizou nenhuma operacao
        if(nop_counter == responsesCreateUser.size())
            return new AppMessage(Requisition.NOP, null);

        // caso contrario, responde que criou um novo usuario com sucesso
        return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_USER, true);
    }

    private Object create_item(AppMessage messageReceived) throws Exception
    {
        Item new_item = (Item) messageReceived.content;

        AppMessage controlMessage = new AppMessage(Requisition.CONTROL_REQUEST_CREATE_ITEM, new_item,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        List responsesCreateItem = dispatcherControl.sendRequestMulticast(controlMessage, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        if(responsesCreateItem.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ITEM, false);

        int nop_counter = 0;

        for(Object response : responsesCreateItem)
        {
            AppMessage msg = (AppMessage) response;

            // caso a mensagem seja do tipo resposta do modelo para criacao de um novo usuario e seu conteudo for falso, responde que deu erro
            if(msg.requisition == Requisition.MODEL_RESPONSE_CREATE_ITEM && ((boolean) msg.content == false))
                return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ITEM, false);
                // caso for nenhuma operacao realiza, incrementa a contagem
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        // caso so veio respostas nop, responde que nao realizou nenhuma operacao
        if(nop_counter == responsesCreateItem.size())
            return new AppMessage(Requisition.NOP, null);

        // caso contrario, responde que criou um novo usuario com sucesso
        return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ITEM, true);
    }

    private Object list_item(AppMessage messageReceived) throws Exception
    {
        AppMessage control_list_item = new AppMessage(Requisition.CONTROL_REQUEST_LIST_ITEM, null,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        List responsesListItem = dispatcherControl.sendRequestMulticast(control_list_item, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        if(responsesListItem.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_LIST_ITEM, null);

        int nop_counter = 0;
        int index_nao_nop = 0;
        int cont = -1;

        for(Object response : responsesListItem)
        {
            cont++;
            AppMessage msg = (AppMessage) response;

            if(msg.requisition == Requisition.NOP)
                nop_counter++;
            else
                index_nao_nop = cont;
        }

        // caso so veio respostas nop, responde que nao realizou nenhuma operacao
        if(nop_counter == responsesListItem.size())
            return new AppMessage(Requisition.NOP, null);

        // caso contrario, responde que criou um novo usuario com sucesso
        AppMessage msg_model = (AppMessage) responsesListItem.get(index_nao_nop);
        return new AppMessage(Requisition.CONTROL_RESPONSE_LIST_ITEM, msg_model.content);
    }

    private Object create_room(AppMessage messageReceived, Address leiloeiro) throws Exception
    {
        Object[] content = (Object[]) messageReceived.content;

        Object[] pedido_troca = {content[0], true};
        AppMessage control_list_item = new AppMessage(Requisition.CONTROL_REQUEST_CHANGE_ITEM_STATE, pedido_troca,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        List responsesListItem = dispatcherControl.sendRequestMulticast(control_list_item, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        if(responsesListItem.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ROOM, Boolean.FALSE);

        int nop_counter = 0;

        for(Object response : responsesListItem)
        {
            AppMessage msg = (AppMessage) response;

            if(msg.requisition == Requisition.MODEL_RESPONSE_CHANGE_ITEM_STATE && ((boolean) msg.content == false))
                return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ROOM, Boolean.FALSE);
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        if(nop_counter == responsesListItem.size())
            return new AppMessage(Requisition.NOP, null);


        Item item = (Item) content[0];
        Address end = (Address) content[1];
        String leiloeiro_nome = (String) content[2];

        Sala nova_sala = new Sala(item, end, leiloeiro_nome, sala_id);
        nova_sala.insert_user("CONTROLE"+channelView.getAddress(), channelView.getAddress());
        nova_sala.insert_user(leiloeiro_nome, leiloeiro);

        salas.add(nova_sala);
        sala_id++;
        return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ROOM, nova_sala);
    }

    private Object get_leiloeiro(AppMessage messageReceived) throws Exception
    {
        Sala sala_buscada = (Sala) messageReceived.content;

        for(int i = 0; i < this.salas.size(); i++)
        {
            if(sala_buscada.equals(salas.get(i)))
            {
                return new AppMessage(Requisition.CONTROL_RESPONSE_AUCTIONEER, salas.get(i).getLeiloeiro());
            }
        }
        return new AppMessage(Requisition.NOP, null);
    }

    private Object recebe_lance(AppMessage message) throws Exception
    {
        Lance lance = (Lance) message.content;

        if(salas.get(lance.sala).getLances().size() > 0)
        {
            if (lance.getValue() > salas.get(lance.sala).getLances().lastElement().getValue())
            {
                salas.get(lance.sala).insert_lance(lance);
                return new AppMessage(Requisition.VIEW_RESPONSE_NEW_BID, true);
            }
            return new AppMessage(Requisition.VIEW_RESPONSE_NEW_BID, false);
        }
        salas.get(lance.sala).insert_lance(lance);
        return new AppMessage(Requisition.VIEW_RESPONSE_NEW_BID, true);
    }

    private Object fecha_leilao(AppMessage messageReceived) throws Exception
    {
        Sala sala = salas.get((int) messageReceived.content);
        Lance lance_final = sala.getLances().lastElement();

        LeilaoResultado resultado = new LeilaoResultado(lance_final.getUser(), sala.getItem(), lance_final.getValue());

        AppMessage controlLogin = new AppMessage(Requisition.CONTROL_REQUEST_SAVE_RESULT, resultado,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        // pega a lista de respostas do canal de controle para o pedido de login
        List responses = dispatcherControl.sendRequestMulticast(controlLogin, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        // caso a quantidade de respostas seja 0, responde que o login nao foi realizado com sucesso
        if(responses.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_CLOSE_ROOM, false);

        int nop_counter = 0; // inicia o contador de nao operacao em 0

        for(Object response : responses) // percorre todos os objetos que veio na resposta
        {
            AppMessage msg = (AppMessage) response; // faz a conversao para o tipo correto

            // se a resposta foi uma resposta vinda do modelo e seu conteudo foi falso, envia uma resposta de falha de login
            if(msg.requisition == Requisition.MODEL_RESPONSE_SAVE_RESULT && ((boolean) msg.content) == false)
                return new AppMessage(Requisition.CONTROL_RESPONSE_CLOSE_ROOM, false);
                // caso a resposta foi uma nao execucao de operacao, incrementa o contador de nop
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        // caso todos as respostas foi nop, envia que a resposta do login foi nop
        if(nop_counter == responses.size())
            return new AppMessage(Requisition.NOP, null);

        // caso contrario, responde que o login foi realizo com sucesso
        return new AppMessage(Requisition.CONTROL_RESPONSE_CLOSE_ROOM, true);
    }

}
