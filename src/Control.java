import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Control extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private JChannel channelView;
    private RequestDispatcher dispatcherView;

    private JChannel channelControlSync;

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

        this.channelControlSync = new JChannel("auction.xml");
        this.channelControlSync.setReceiver(this);
        this.channelControlSync.connect("AuctionControlSyncCluster");
        this.channelControlSync.getState(null, 10000);

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
            else if(messageReceived.requisition == Requisition.CONTROL_GET_CONTROL_VIEW_ADDRESS)
                return pedido_controle_view_addr(messageReceived);
            else if(messageReceived.requisition == Requisition.CONTROL_ASK_CONTROL_PROCESS)
                return pedido_controle_addr(messageReceived);
            else if(messageReceived.requisition == Requisition.CONTROL_SEND_ROOM_CONTROL)
                return recebe_sala_outro_controle(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_HISTORY)
                return list_history(messageReceived);

            return new AppMessage(Requisition.NOP, null); //caso nao seja nenhuma requisicao para o controle
        }
        else // else para mensagem de um tipo invalido
        {
            System.err.println("[ERROR-MODEL] Enviou classe nao reconhecida");
            System.err.flush();
            return new AppMessage(Requisition.CLASS_ERROR, null);
        }
    }

    public void getState(OutputStream output) throws Exception {
        Object[] state = new Object[2];

        state[0] = this.salas;
        state[1] = this.sala_id;

        Util.objectToStream(state, new DataOutputStream(output));
    }

    public void setState(InputStream input) throws Exception {

        Object[] state = (Object[]) Util.objectFromStream(new DataInputStream(input));

        this.salas = (ArrayList<Sala>) state[0];
        this.sala_id = (int) state[1];
    }

    private Object pedido_controle_addr(AppMessage messageReceived)
    {
        return new AppMessage(Requisition.RESPONSE_CONTROL_PROCESS, channelControl.getAddress());
    }

    private Object pedido_controle_view_addr(AppMessage messageReceived)
    {
        return new AppMessage(Requisition.RESPONSE_CONTROL_VIEW_ADDRESS, channelView.getAddress());
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

        if(existe_sala_item((Item) content[0]))
        {
            return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ROOM, Boolean.TRUE);
        }

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

        List<Address> controle_enderecos_na_visao = get_control_view_address();

        Sala nova_sala = new Sala(item, end, leiloeiro_nome, sala_id);
        nova_sala.insert_user("CONTROLE"+channelView.getAddress(), channelView.getAddress());
        for(Address i : controle_enderecos_na_visao)
        {
            nova_sala.insert_user("CONTROLE"+i, i);
        }
        nova_sala.insert_user(leiloeiro_nome, leiloeiro);

        List<Address> enderecos_controle = get_control_address();

        if(enderecos_controle.size() > 0)
        {
            AppMessage msg = new AppMessage(Requisition.CONTROL_SEND_ROOM_CONTROL, nova_sala);
            dispatcherControl.sendRequestAnycast(enderecos_controle, msg, ResponseMode.GET_ALL, channelControl.getAddress());
        }

        salas.add(nova_sala);
        sala_id++;
        return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_ROOM, nova_sala);
    }

    private List<Address> get_control_view_address() throws Exception
    {
        AppMessage msg = new AppMessage(Requisition.CONTROL_GET_CONTROL_VIEW_ADDRESS, null);

        List responses = dispatcherControl.sendRequestMulticast(msg, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        if(responses.size() == 0)
            return new ArrayList<>();

        int nop_counter = 0;

        List<Address> enderecos = new ArrayList<>();

        for(Object response : responses)
        {
            AppMessage msg_controle = (AppMessage) response;

            if(msg_controle.content != null)
            {
                enderecos.add((Address) msg_controle.content);
            }
        }

        return enderecos;
    }

    private List<Address> get_control_address() throws Exception
    {
        AppMessage msg = new AppMessage(Requisition.CONTROL_ASK_CONTROL_PROCESS, null);

        List responses = dispatcherControl.sendRequestMulticast(msg, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        if(responses.size() == 0)
            return new ArrayList<>();

        int nop_counter = 0;

        List<Address> enderecos = new ArrayList<>();

        for(Object response : responses)
        {
            AppMessage msg_controle = (AppMessage) response;

            if(msg_controle.content != null)
            {
                enderecos.add((Address) msg_controle.content);
            }
        }

        return enderecos;
    }

    private Object recebe_sala_outro_controle(AppMessage message)
    {
        Sala nova_sala = (Sala) message.content;
        salas.add(nova_sala);
        sala_id++;
        return new AppMessage(Requisition.CONTROL_REPONSE_RECEIVE_ROOM, null);
    }

    private boolean existe_sala_item(Item item)
    {
        for(Sala s : this.salas)
        {
            if(s.getItem().getProprietario().equals(item.getProprietario()) && s.getItem().getName().equals(item.getName()))
                return true;
        }
        return false;
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

        LeilaoResultado resultado = new LeilaoResultado(lance_final.getUser(), sala.getItem(), lance_final.getValue(), sala.getLances());

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
        this.salas.remove(sala);
        return new AppMessage(Requisition.CONTROL_RESPONSE_CLOSE_ROOM, true);
    }

    private Object list_history(AppMessage messageReceived) throws Exception
    {
        AppMessage control_history = new AppMessage(Requisition.CONTROL_REQUEST_HISTORY, null,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        List responsesListItem = dispatcherControl.sendRequestMulticast(control_history, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();

        if(responsesListItem.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_HISTORY, null);

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
        return new AppMessage(Requisition.CONTROL_RESPONSE_HISTORY, msg_model.content);
    }

}
