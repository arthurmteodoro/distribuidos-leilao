import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;

import java.util.List;

public class Control extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private JChannel channelView;
    private RequestDispatcher dispatcherView;

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
            if(messageReceived.requisition == Requisition.VIEW_REQUEST_LOGIN) // caso seja uma requisicao de login da visao
                return login(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_CREATE_USER) // caso seja uma requisicao de criacao de usuario da visao
                return create_user(messageReceived);

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
}
