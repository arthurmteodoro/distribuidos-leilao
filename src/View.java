import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;

import java.io.Console;
import java.util.ArrayList;
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
                    {
                        System.out.println("Login ok");
                        user_logado(user);
                    }
                    else
                        System.out.println("Login error");
                }
            }
        }
    }

    private void user_logado(String usuario) throws Exception
    {
        String usuario_atual = usuario;
        Scanner keyboard = new Scanner(System.in);

        boolean logout = false;

        while(!logout)
        {
            System.out.println("\n\n\n=============== BEM VINDO USUARIO "+usuario.toUpperCase()+" ===============");
            System.out.println("1. Criar item");
            System.out.println("2. Listar itens");
            System.out.println("3. Criar Sala");
            System.out.println("4. Listar salas existentes");
            System.out.println("0. Logout");
            System.out.print(">");
            String input = keyboard.nextLine().toLowerCase();

            if(Integer.valueOf(input) == 1)
            {
                System.out.print("\n\nDigite o nome do item: ");
                String item_name = keyboard.nextLine().toLowerCase();
                System.out.print("Digite a descricao do item: ");
                String item_description = keyboard.nextLine().toLowerCase();
                System.out.print("Digite o valor do item: ");
                Double item_value = Double.valueOf(keyboard.nextLine());

                if(create_item(item_name, item_description, item_value, usuario_atual))
                    System.out.println("Item criado com sucesso");
                else
                    System.out.println("Falha na criacao de novo item");

            }
            else if(Integer.valueOf(input) == 2)
            {
                ArrayList<Item> itens = get_list_itens();
                if(itens == null)
                    System.out.println("Erro na busca de novos itens");
                else
                    System.out.println("[");
                    for(Item i : itens)
                    {
                        System.out.println("\t{Nome: "+i.getName()+" Descrição: "+i.getDescricao()+" Valor mínimo: "+i.getValue()+" Dono: "+i.getName()+"},");
                    }
            }
            else if(Integer.valueOf(input) == 3)
            {
                ArrayList<Item> itens = get_itens_para_leilao();
                if(itens == null)
                    System.out.println("Erro na busca de novos itens");
                else
                    System.out.println("[");
                int cont = 0;
                for(Item i : itens)
                {
                    System.out.println("\t["+cont+"]{Nome: "+i.getName()+" Descrição: "+i.getDescricao()+" Valor mínimo: "+i.getValue()+" Dono: "+i.getName()+"},");
                }

                System.out.print("Digite o indice do item que deseja leiloar: ");
                int index = Integer.parseInt(keyboard.nextLine());

                Object valor = create_room(itens.get(index), usuario_atual);
                if(valor instanceof Boolean)
                    System.out.println("Erro na criacao de sala");
                else
                {
                    System.out.println("Sala criada com sucesso");
                    ArrayList<Address> participantes = (ArrayList<Address>) valor;
                    em_sala(usuario_atual, participantes, usuario_atual+"/"+itens.get(index).getName());
                }
            }
            else if(Integer.valueOf(input) == 4)
            {
                ArrayList<Sala> salas = get_list_salas();
                System.out.println(salas);
            }
            else if(Integer.valueOf(input) == 0)
                logout = true;
        }

        System.out.println("\n\n\n Até mais :)");
    }

    public void em_sala(String nome_user, ArrayList<Address> participantes, String nome_sala)
    {
        System.out.println("\n\n======= BEM VINDO A SALA "+nome_sala.toUpperCase()+" =======");

        Scanner keyboard = new Scanner(System.in);

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

    private boolean create_item(String name, String description, Double value, String owner) throws Exception
    {
        Item new_item = new Item(name, description, value, owner);

        AppMessage create_item = new AppMessage(Requisition.VIEW_REQUEST_CREATE_ITEM, new_item,
                channelView.getAddress(), sequenceNumber);
        sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(create_item, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
            return false;

        int nop_counter = 0;

        for(Object control_resp : controlResponse)
        {
            AppMessage response = (AppMessage) control_resp;

            // caso a resposta seja do tipo resposta do controle a um pedido de criacao de usuario e seu conteudo seja false, retorna falha na criacao de novo usuario
            if(response.requisition == Requisition.CONTROL_RESPONSE_CREATE_ITEM && ((boolean) response.content == false))
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

    private ArrayList<Item> get_list_itens() throws Exception
    {
        AppMessage list_item = new AppMessage(Requisition.VIEW_REQUEST_LIST_ITEM, null,
                channelView.getAddress(), sequenceNumber);
        sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(list_item, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
            return null;

        int nop_counter = 0;
        int non_nop_index = -1;
        int count = -1;

        for(Object control_resp : controlResponse)
        {
            count++;
            AppMessage response = (AppMessage) control_resp;

            if (response.requisition == Requisition.NOP)
                nop_counter++;
            else
                non_nop_index = count;
        }

        // caso nenhuma acao foi tomada ao pedido de criacao de usuario, retorna que o usuario nao foi criado
        if(nop_counter == controlResponse.size())
            return null;

        // caso contrario, diz que o usuario foi criado com sucesso
        return (ArrayList<Item>) ((AppMessage) controlResponse.get(non_nop_index)).content;
    }

    private ArrayList<Item> get_itens_para_leilao() throws Exception
    {
        ArrayList<Item> todos_itens = get_list_itens();
        ArrayList<Item> ret = new ArrayList<>();

        for(Item i : todos_itens)
        {
            if(i.isEm_leilao() == false)
            {
                ret.add(i);
            }
        }
        return ret;
    }

    private Object create_room(Item item, String usuario) throws Exception
    {
        Object[] pedido = {item, channelView.getAddress(), usuario};

        AppMessage create_item = new AppMessage(Requisition.VIEW_REQUEST_CREATE_ROOM, pedido,
                channelView.getAddress(), sequenceNumber);
        sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(create_item, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
            return Boolean.FALSE;

        int nop_counter = 0;
        int non_nop_index = -1;
        int count = -1;

        for(Object control_resp : controlResponse)
        {
            count++;
            AppMessage response = (AppMessage) control_resp;

            // caso a resposta seja do tipo resposta do controle a um pedido de criacao de usuario e seu conteudo seja false, retorna falha na criacao de novo usuario
            if(response.requisition == Requisition.CONTROL_RESPONSE_CREATE_ROOM && response.content instanceof Boolean)
                return false;
                // se nao realizou nenhuma operacao, incrementa o contador de nop
            else if (response.requisition == Requisition.NOP)
                nop_counter++;
            else
                non_nop_index = count;
        }

        // caso nenhuma acao foi tomada ao pedido de criacao de usuario, retorna que o usuario nao foi criado
        if(nop_counter == controlResponse.size())
            return Boolean.FALSE;

        // caso contrario, diz que o usuario foi criado com sucesso
        return ((AppMessage) controlResponse.get(non_nop_index)).content;
    }

    private ArrayList<Sala> get_list_salas() throws Exception
    {
        AppMessage list_item = new AppMessage(Requisition.VIEW_REQUEST_LIST_ROOM, null,
                channelView.getAddress(), sequenceNumber);
        sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(list_item, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
        {
            System.out.println("Tamanho 0");
            return null;
        }

        int nop_counter = 0;
        int non_nop_index = -1;
        int count = -1;

        for(Object control_resp : controlResponse)
        {
            count++;
            AppMessage response = (AppMessage) control_resp;

            if (response.requisition == Requisition.NOP)
                nop_counter++;
            else
                non_nop_index = count;
        }

        // caso nenhuma acao foi tomada ao pedido de criacao de usuario, retorna que o usuario nao foi criado
        if(nop_counter == controlResponse.size())
        {
            System.out.println("So nop");
            return null;
        }

        // caso contrario, diz que o usuario foi criado com sucesso
        return (ArrayList<Sala>) ((AppMessage) controlResponse.get(non_nop_index)).content;
    }
}
