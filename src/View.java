import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.Console;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

public class View extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelView;
    private RequestDispatcher dispatcherView;

    private int sequenceNumber;
    private Sala sala_que_esta = null;
    private String user_logado;
    private boolean acabou_leilao;

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
        user_logado = usuario;
        Scanner keyboard = new Scanner(System.in);

        boolean logout = false;

        while(!logout)
        {
            System.out.println("\n\n\n=============== BEM VINDO USUARIO "+usuario.toUpperCase()+" ===============");
            System.out.println("1. Criar item");
            System.out.println("2. Listar itens");
            System.out.println("3. Criar Sala");
            System.out.println("4. Listar salas existentes");
            System.out.println("5. Entrar em uma sala");
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
                        System.out.println("\t{Nome: "+i.getName()+" Descrição: "+i.getDescricao()+" Valor mínimo: "+i.getValue()+" Dono: "+i.getProprietario()+"},");
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
                    System.out.println("\t["+cont+"]{Nome: "+i.getName()+" Descrição: "+i.getDescricao()+" Valor mínimo: "+i.getValue()+" Dono: "+i.getProprietario()+"},");
                }

                System.out.print("Digite o indice do item que deseja leiloar: ");
                int index = Integer.parseInt(keyboard.nextLine());

                Object valor = create_room(itens.get(index), usuario_atual);
                if(valor instanceof Boolean)
                    System.out.println("Erro na criacao de sala");
                else
                {
                    System.out.println("Sala criada com sucesso");
                    System.out.println("\n\nATENCAO LEILOEIRO, COM GRANDES PODERES VEM GRANDES RESPONSABILIDADES");
                    System.out.println("VOCÊ É O RESPONSÁVEL POR TERMINAR COM O LEILÃO");
                    System.out.println("Para isso, dê um lance do valor -1");
                    Sala sala = (Sala) valor;
                    em_sala(usuario_atual, sala);
                }
            }
            else if(Integer.valueOf(input) == 4)
            {
                ArrayList<Sala> salas = get_list_salas();
                System.out.println(salas);
            }
            else if(Integer.valueOf(input) == 5)
            {
                ArrayList<Sala> salas_existentes = get_list_salas();
                System.out.println("Quantidades de salas: "+salas_existentes.size());
                for(int i = 0; i < salas_existentes.size(); i++)
                {
                    System.out.println("\t["+i+"] "+salas_existentes.get(i));
                }
                System.out.print("Digite a sala escolhida: ");
                int index_sala = Integer.parseInt(keyboard.nextLine());

                if(index_sala == -1)
                    continue;

                Address leiloeiro = get_leiloeiro_add(salas_existentes.get(index_sala));

                Sala sala = pede_para_entrar_na_sala(leiloeiro);
                apresenta_para_sala(sala, usuario_atual);
                em_sala(usuario_atual, sala);
            }
            else if(Integer.valueOf(input) == 0)
                logout = true;
        }

        System.out.println("\n\n\n Até mais :)");
    }

    public void em_sala(String nome_user, Sala sala) throws Exception
    {
        System.out.println("\n======= BEM VINDO A SALA "+sala+" =======");
        Scanner keyboard = new Scanner(System.in);

        this.sala_que_esta = sala;

        this.acabou_leilao = false;
        while(!this.acabou_leilao)
        {
            System.out.print("Digite seu lance: ");
            Double valor_lance = Double.valueOf(keyboard.nextLine());
            if(valor_lance != -1)
            {
                Lance lance = new Lance(nome_user, valor_lance, sala.id);
                //System.out.println(sala_que_esta.getUsers_addr());
                if (da_lance(sala_que_esta, lance))
                {
                    sala_que_esta.insert_lance(lance);
                    System.out.println("\nLANCE ACEITO!!");
                }
                else
                    System.out.println("\nLANCE INVALIDO");
            }
            else
            {
                if(this.sala_que_esta.getLeiloeiro().toString().equals(channelView.getAddress().toString()))
                {
                    System.out.println("ATENÇÃO: VOCÊ DESEJA MESMO FECHAR ESTE LEILAO? (Y/N)");
                    if(keyboard.nextLine().toLowerCase().equals("y"))
                    {
                        fecha_leilao(sala_que_esta);
                    }
                    else
                        continue;
                }
                else
                {
                    this.acabou_leilao = true;
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
            if(sala_que_esta != null)
            {
                if (messageReceived.requisition == Requisition.VIEW_REQUEST_ENTER_ROOM)
                    return new AppMessage(Requisition.VIEW_RESPONSE_ENTER_ROOM, sala_que_esta);
                else if (messageReceived.requisition == Requisition.BONJOUR)
                {
                    Object[] content = (Object[]) messageReceived.content;
                    sala_que_esta.insert_user((String)content[0], message.getSrc());
                    System.out.println("USUARIO "+(String) content[0]+" ENTROU NA SALA");
                    return new AppMessage(Requisition.SALUT, null);
                }
                else if(messageReceived.requisition == Requisition.VIEW_REQUEST_NEW_BID)
                    return recebe_lance(messageReceived);
                else if(messageReceived.requisition == Requisition.AU_REVOIR)
                    return processa_final(messageReceived);
            }

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
        return (ArrayList<Sala>) ((AppMessage) controlResponse.get(non_nop_index)).content;
    }

    private Address get_leiloeiro_add(Sala sala) throws Exception
    {
        AppMessage list_item = new AppMessage(Requisition.VIEW_REQUEST_AUCTIONEER, sala,
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
        return (Address) ((AppMessage) controlResponse.get(non_nop_index)).content;
    }

    private Sala pede_para_entrar_na_sala(Address leiloeiro) throws Exception
    {
        AppMessage pedido = new AppMessage(Requisition.VIEW_REQUEST_ENTER_ROOM, null);
        sequenceNumber++;

        Object response = dispatcherView.sendRequestUnicast(leiloeiro, pedido, ResponseMode.GET_ALL);
        AppMessage msg = (AppMessage) response;

        return (Sala) msg.content;
    }

    private void apresenta_para_sala(Sala sala, String user) throws Exception
    {
        Object[] content = {user, sala.id};
        AppMessage ola = new AppMessage(Requisition.BONJOUR, content);
        sequenceNumber++;

        List response = dispatcherView.sendRequestAnycast(sala.getUsers_addr(), ola, ResponseMode.GET_ALL, channelView.getAddress()).getResults();
    }

    private boolean da_lance(Sala sala, Lance lance) throws Exception
    {
        AppMessage novo_lance = new AppMessage(Requisition.VIEW_REQUEST_NEW_BID, lance);
        sequenceNumber++;

        List response = dispatcherView.sendRequestAnycast(sala.getUsers_addr(), novo_lance, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(response.size() == 0)
        {
            return false;
        }

        for(Object control_resp : response)
        {
            AppMessage msg = (AppMessage) control_resp;

            if(msg.requisition == Requisition.VIEW_RESPONSE_NEW_BID && ((boolean) msg.content == false))
            {
                return false;
            }
        }

        return true;
    }

    private Object recebe_lance(AppMessage message) throws Exception
    {
        Lance lance = (Lance) message.content;

        if(sala_que_esta.getLances().size() > 0)
        {
            Lance ultimo = sala_que_esta.getLances().lastElement();

            if (lance.getValue() > ultimo.getValue())
            {
                sala_que_esta.insert_lance(lance);
                System.out.println("\n[NOVO LANCE]: " + lance);
                return new AppMessage(Requisition.VIEW_RESPONSE_NEW_BID, true);
            }
            return new AppMessage(Requisition.VIEW_RESPONSE_NEW_BID, false);
        }
        else
        {
            sala_que_esta.insert_lance(lance);
            System.out.println("\n[NOVO LANCE]: " + lance);
            return new AppMessage(Requisition.VIEW_RESPONSE_NEW_BID, true);
        }
    }

    private void notifica_que_acabou(Sala sala) throws Exception
    {
        AppMessage adeus = new AppMessage(Requisition.AU_REVOIR, sala.getLances().lastElement());
        sequenceNumber++;

        List response = dispatcherView.sendRequestAnycast(sala.getUsers_addr(), adeus, ResponseMode.GET_ALL).getResults();
    }


    private Object processa_final(AppMessage messageReceived)
    {
        Lance ganhador = (Lance) messageReceived.content;

        if(ganhador.getUser().equals(this.user_logado))
        {
            System.out.println("\n\nPARABENS!!!!!!!!!");
            System.out.println("Voce comprou o item por "+ganhador.getValue());
            System.out.println("Pressione -1 para sair");
        }
        else
        {
            System.out.println("O usuario " + ganhador.getUser() + " comprou o item por " + ganhador.getValue());
            System.out.println("Pressione -1 para sair");
        }
        this.acabou_leilao = true;
        return new AppMessage(Requisition.BYE, null);
    }

    private boolean fecha_leilao(Sala sala) throws Exception
    {
        notifica_que_acabou(sala);

        AppMessage close = new AppMessage(Requisition.VIEW_REQUEST_CLOSE_ROOM, sala.id,
                channelView.getAddress(), sequenceNumber);
        sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(close, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
            return false;

        int nop_counter = 0;
        int non_nop_index = -1;
        int count = -1;

        for(Object control_resp : controlResponse)
        {
            count++;
            AppMessage response = (AppMessage) control_resp;

            if(response.requisition == Requisition.CONTROL_RESPONSE_CLOSE_ROOM && !((boolean) response.content))
                return false;
            else if (response.requisition == Requisition.NOP)
                nop_counter++;
            else
                non_nop_index = count;
        }

        // caso nenhuma acao foi tomada ao pedido de criacao de usuario, retorna que o usuario nao foi criado
        if(nop_counter == controlResponse.size())
            return false;

        this.acabou_leilao = true;
        System.out.println("LEILAO TERMINADO!!!!!");
        return true;
    }
}
