import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;

import java.io.Console;
import java.util.List;
import java.util.Scanner;

public class View extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelView;
    private RequestDispatcher dispatcherView;

    private int sequenceNumber;

    public static void main(String[] args)
    {
        try
        {
            new View().start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void start() throws Exception
    {
        this.sequenceNumber = -1; // inicia o numero de sequencia

        // cria o canal e o despachante do canal da visao
        this.channelView = new JChannel("auction.xml");
        this.channelView.setReceiver(this);
        this.dispatcherView = new RequestDispatcher(channelView, this);

        // se conecta ao canal da visao
        this.channelView.connect("AuctionViewCluster");
        eventloop();
        this.channelView.close();
    }

    private void eventloop() throws Exception
    {
        boolean exit = false;
        Scanner keyboard = new Scanner(System.in);
        String input = "";

        // enquanto o usuario nao pedir para sair "ba dum tss :)"
        while(!exit)
        {
            System.out.print(">");
            input = keyboard.nextLine().toLowerCase();

            // caso for para sair,
            if(input.startsWith("exit"))
                //System.exit(0);
                exit = true;
            else
            {
                // caso o pedido seja para criar um novo usuario
                if(input.equals("create user"))
                {
                    System.out.print("User: ");
                    String user = keyboard.nextLine().toLowerCase();
                    System.out.print("Password: ");
                    String password = keyboard.nextLine().toLowerCase();

                    if(createUser(user, password)) //chama a funcao de criacao de um novo usuario
                        System.out.println("user created successfully");
                    else
                        System.out.println("user creation failure");
                }
                // caso seja um pedido de login
                else if(input.equals("login"))
                {
                    System.out.print("User: ");
                    String user = keyboard.nextLine().toLowerCase();
                    System.out.print("Password: ");
                    String password = keyboard.nextLine().toLowerCase();

                    if(login(user, password)) // chama a funcao de login
                        System.out.println("Login ok");
                    else
                        System.out.println("Login error");
                }
            }
        }
    }

    // funcao que trata a interrupcao de uma nova mensagem
    @Override
    public Object handle(Message message) throws Exception
    {
        if(message.getObject() instanceof AppMessage)
        {
            AppMessage messageReceived = (AppMessage) message.getObject();

            return new AppMessage(Requisition.NOP, null);
        }
        return new AppMessage(Requisition.CLASS_ERROR, null);
    }

    // funcao para realizar o login
    private boolean login(String user, String password) throws Exception
    {
        // gera a hash da senha e armazena em um vetor com o usuario
        String[] content = {user, Utils.gerarSHA256(password.getBytes())};

        // cria o mensagem com a requisicao de login da visao, passando seu endereco no canal da visao e seu numero de sequencia
        AppMessage loginMessage = new AppMessage(Requisition.VIEW_REQUEST_LOGIN, content,
                channelView.getAddress(), sequenceNumber);
        this.sequenceNumber++;

        // pega todos os resultados vindos do canal da visao
        List controlResponse = dispatcherView.sendRequestMulticast(loginMessage, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        // se nao tiver nenhuma resposta, retorna que nao foi possivel realizar o login
        if(controlResponse.size() == 0)
            return false;

        int nop_counter = 0;

        for(Object value : controlResponse)
        {
            AppMessage response = (AppMessage) value;
            // caso a mensagem seja do tipo resposta do controle a um pedido de login e seu conteudo for false, retorna que nao foi fazer o login
            if(response.requisition == Requisition.CONTROL_RESPONSE_LOGIN && ((boolean) response.content == false))
                return false;
            // caso a resposta foi nop, incrementa o contador de nop
            else if(response.requisition == Requisition.NOP)
                nop_counter++;
        }

        // caso so teve como resposta nop, retorna falha no login
        if(nop_counter == controlResponse.size())
            return false;
        // caso contrario, retorna que o login deu certo
        return true;
    }

    // funcao para criar um novo usuario
    private boolean createUser(String user, String password) throws Exception
    {
        // gera a hash da senha e armazena em um vetor com o usuario
        String[] content = {user, Utils.gerarSHA256(password.getBytes())};

        // cria uma nova mensagem do tipe pedido de criacao de um novo usuario da visao
        AppMessage createUser = new AppMessage(Requisition.VIEW_REQUEST_CREATE_USER, content,
                channelView.getAddress(), sequenceNumber);
        this.sequenceNumber++;

        // pega todos as respostas do canal da visao
        List controlResponse = dispatcherView.sendRequestMulticast(createUser, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        // caso nao teve resposta, retorna falha na criacao de um novo usuario
        if(controlResponse.size() == 0)
            return false;

        int nop_counter = 0;

        for(Object value : controlResponse)
        {
            AppMessage response = (AppMessage) value;

            // caso a resposta seja do tipo resposta do controle a um pedido de criacao de usuario e seu conteudo seja false, retorna falha na criacao de novo usuario
            if(response.requisition == Requisition.CONTROL_RESPONSE_CREATE_USER && ((boolean) response.content == false))
                return false;
            // se nao realizou nenhuma operacao, incrementa o contador de nop
            else if (response.requisition == Requisition.NOP)
                nop_counter++;
        }

        // caso nenhuma acao foi tomada ao pedido de criacao de usuario, retorna que o usuario nao foi criado
        if(nop_counter == controlResponse.size())
            return false;

        // caso contrario, diz que o usuario foi criado com sucesso
        return true;
    }
}
