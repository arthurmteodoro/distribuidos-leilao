import Misc.AppMessage;
import Misc.Item;
import Misc.LeilaoResultado;
import Misc.Requisition;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.util.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class Model extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelModel;
    private RequestDispatcher dispatcherModel;

    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private JChannel channelModelSync;

    private HashMap<String, String> usersAndKeys; //mapa para guardar os usuarios e suas senhas
    private HashMap<Address, Integer> seqs; //mapa para manter o numero de sequencia dos enderecos da visao

    private ArrayList<Item> items; // lista com todos os items existentes no sistema
    private ArrayList<LeilaoResultado> resultados;

    public static void main(String[] args)
    {
        try
        {
            new Model().start();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void start() throws Exception
    {
        // instancia os mapas
        this.usersAndKeys = new HashMap<>();
        this.seqs = new HashMap<>();

        this.items = new ArrayList<>();
        this.resultados = new ArrayList<>();

        // instancia o canal e o despachante do modelo
        this.channelModel = new JChannel("auction.xml");
        this.channelModel.setReceiver(this);
        this.dispatcherModel = new RequestDispatcher(this.channelModel, this);

        // instancia o canal e o despachante do controle
        this.channelControl = new JChannel("auction.xml");
        this.channelControl.setReceiver(this);
        this.dispatcherControl = new RequestDispatcher(this.channelControl, this);

        // conecta nos canais modelo e controle
        this.channelModel.connect("AuctionModelCluster");
        this.channelControl.connect("AuctionControlCluster");

        load_clients(); // carrega os usuarios no seu hd
        load_itens();
        load_history();

        this.channelModelSync = new JChannel("auction.xml");
        this.channelModelSync.setReceiver(this);
        this.channelModelSync.connect("AuctionModelSyncCluster");
        this.channelModelSync.getState(null, 10000);

        Timer timer = new Timer(); //cria uma nova tarefa que a cada 5 segundos vai escrever os mapas no hd
        timer.scheduleAtFixedRate(new WriterTask(), 5000, 5000);

        eventLoop();
        this.channelModel.close();
        this.channelControl.close();
    }

    private void eventLoop() throws Exception
    {
        while(true);
    }

    public void getState(OutputStream output) throws Exception {
        Object[] state = new Object[3];

        state[0] = this.usersAndKeys;
        state[1] = this.items;
        state[2] = this.resultados;

        Util.objectToStream(state, new DataOutputStream(output));
    }

    public void setState(InputStream input) throws Exception {

        Object[] state = (Object[]) Util.objectFromStream(new DataInputStream(input));

        this.usersAndKeys = (HashMap<String, String>) state[0];
        this.items = (ArrayList<Item>) state[1];
        this.resultados = (ArrayList<LeilaoResultado>) state[2];
    }

    // funcao de interrupcao quando uma nova mensagem e recebida
    @Override
    public Object handle(Message message) throws Exception
    {
        if(message.getObject() instanceof AppMessage) //caso for uma mensagem do tipo que o codigo executa
        {
            AppMessage messageReceived = (AppMessage) message.getObject(); //faz o cast da mensagem para o tipo apropriado
            System.out.println("Recebeu mensagem do tipo "+messageReceived.requisition);

            // se e a primeira mensagem daquele cliente ou seu numero de sequencia for menor que o que o modelo tem, deixa executar
            // isso garante que nao existe duplicatas caso mais de um processo do controle envie a mesma mensagem da visao
            if(!seqs.containsKey(messageReceived.clientAddress) || seqs.get(messageReceived.clientAddress) < messageReceived.sequenceNumber)
            {
                // caso o processo da visao nunca falou com o modelo, cria uma nova instancia no mapa, caso contrario, atualiza seu numero
                if(!seqs.containsKey(messageReceived.clientAddress))
                    seqs.put(messageReceived.clientAddress, messageReceived.sequenceNumber);
                else
                    seqs.replace(messageReceived.clientAddress, messageReceived.sequenceNumber);

                // caso o controle pediu um login
                if (messageReceived.requisition == Requisition.CONTROL_REQUEST_LOGIN)
                {
                    String[] userLoginRequest = (String[]) messageReceived.content; //pega dos dados do login da mensagem recebida e troca para o tipo correto
                    if (checkLogin(userLoginRequest[0], userLoginRequest[1]))
                        return new AppMessage(Requisition.MODEL_RESPONSE_LOGIN, true); // caso o login foi autorizado, envia a mensagem de resposta de login com conteudo verdadeiro
                    else
                        return new AppMessage(Requisition.MODEL_RESPONSE_LOGIN, false); // caso o login foi autorizado, envia a mensagem de resposta de login com conteudo falso
                }
                // caso o controle pediu a criacao de um novo cliente
                else if (messageReceived.requisition == Requisition.CONTROL_REQUEST_CREATE_USER)
                {
                    String[] userCreateRequest = (String[]) messageReceived.content;
                    if (createUser(userCreateRequest[0], userCreateRequest[1]))
                        return new AppMessage(Requisition.MODEL_RESPONSE_CREATE_USER, true);
                    else
                        return new AppMessage(Requisition.MODEL_RESPONSE_CREATE_USER, false);
                }
                else if(messageReceived.requisition == Requisition.CONTROL_REQUEST_CREATE_ITEM)
                {
                    Item itemCreateRequest = (Item) messageReceived.content;
                    if(createItem(itemCreateRequest))
                        return new AppMessage(Requisition.MODEL_RESPONSE_CREATE_ITEM, true);
                    else
                        return new AppMessage(Requisition.MODEL_RESPONSE_CREATE_ITEM, false);
                }
                else if(messageReceived.requisition == Requisition.CONTROL_REQUEST_LIST_ITEM)
                    return new AppMessage(Requisition.MODEL_RESPONSE_LIST_ITEM, this.items);
                else if(messageReceived.requisition == Requisition.CONTROL_REQUEST_CHANGE_ITEM_STATE)
                {
                    Item item_change = (Item) ((Object[])messageReceived.content)[0];
                    boolean valor = (boolean) ((Object[])messageReceived.content)[1];
                    if(change_item_status(item_change, valor))
                        return new AppMessage(Requisition.MODEL_RESPONSE_CHANGE_ITEM_STATE, true);
                    else
                        return new AppMessage(Requisition.MODEL_RESPONSE_CHANGE_ITEM_STATE, false);
                }
                else if(messageReceived.requisition == Requisition.CONTROL_REQUEST_SAVE_RESULT)
                {
                    LeilaoResultado res = (LeilaoResultado) messageReceived.content;
                    if (final_leilao(res))
                    {
                        return new AppMessage(Requisition.MODEL_RESPONSE_SAVE_RESULT, true);
                    }
                }
                else if(messageReceived.requisition == Requisition.CONTROL_GET_CONTROL_VIEW_ADDRESS)
                {
                    return new AppMessage(Requisition.RESPONSE_CONTROL_VIEW_ADDRESS, null);
                }
                else if(messageReceived.requisition == Requisition.CONTROL_ASK_CONTROL_PROCESS)
                {
                    return new AppMessage(Requisition.RESPONSE_CONTROL_PROCESS, null);
                }
                else if(messageReceived.requisition == Requisition.CONTROL_REQUEST_HISTORY)
                    return new AppMessage(Requisition.MODEL_RESPONSE_HISTORY, this.resultados);
            }
            // a operacao pedida nao seja para o modelo ou e uma mensagem duplicada, envia NOP (nenhuma operacao)
            return new AppMessage(Requisition.NOP, null);
        }
        else
            // caso a mensagem nao seja do tipo correto, envia um erro de classe
            return new AppMessage(Requisition.CLASS_ERROR, null);
    }

    //funcao para verificar o login
    private boolean checkLogin(String user, String key)
    {
        if(usersAndKeys.containsKey(user)) // caso o usuario estaja no mapa
        {
            String keyInHash = usersAndKeys.get(user); //pega seu valor de senha no mapa
            return keyInHash.equals(key); //retorna se a senha e igual a recebida
        }
        return false;
    }

    private boolean createUser(String user, String key) throws IOException
    {
        if(!usersAndKeys.containsKey(user)) //caso o usuario nao esteja no mapa
        {
            usersAndKeys.put(user, key); //coloca ele no mapa com sua senha
            write_users(); //chama a funcao para escrever os usuarios no hd
            return true;
        }
        return false;
    }

    private boolean createItem(Item item)
    {
        for(Item i : this.items)
        {
            if(i.getName().equals(item.getName()) && i.getProprietario().equals(item.getProprietario()))
                return false;
        }

        this.items.add(item);
        return true;
    }

    private boolean change_item_status(Item item, boolean valor)
    {
        int index = -1;
        for(int i = 0; i < this.items.size(); i++)
        {
            Item it = this.items.get(i);
            if(it.getName().equals(item.getName()) && it.getProprietario().equals(item.getProprietario()))
            {
                index = i;
                break;
            }
        }

        if(this.items.get(index).isEm_leilao() == valor)
            return false;
        else
        {
            this.items.get(index).setEm_leilao(valor);
            return true;
        }
    }

    private boolean final_leilao(LeilaoResultado res)
    {
        resultados.add(res);
        Item item_na_lista = null;

        for(Item i : items)
        {
            if(i.getProprietario().equals(res.item.getProprietario()) && i.getName().equals(res.item.getName()))
            {
                item_na_lista = i;
            }
        }
        if(item_na_lista != null)
        {
            item_na_lista.setEm_leilao(false);
            item_na_lista.setProprietario(res.usuario);
            return true;
        }
        return false;
    }

    // funcao para escrever os usuarios no hd
    private void write_users() throws IOException
    {
        File file = new File("data/clients.bin");
        if(!file.exists()) // verifica se o arquivo nao existe, caso nao existe, cria um novo
            file.createNewFile();

        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file)); // cria o escritor de arquivo
        writer.writeObject(this.usersAndKeys); // escreve o objeto no arquivo
        writer.close();
    }

    private void write_itens() throws IOException
    {
        File file = new File("data/itens.bin");
        if(!file.exists()) // verifica se o arquivo nao existe, caso nao existe, cria um novo
            file.createNewFile();

        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file)); // cria o escritor de arquivo
        writer.writeObject(this.items); // escreve o objeto no arquivo
        writer.close();
    }

    private void write_history() throws IOException
    {
        File file = new File("data/history.bin");
        if(!file.exists()) // verifica se o arquivo nao existe, caso nao existe, cria um novo
            file.createNewFile();

        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file)); // cria o escritor de arquivo
        writer.writeObject(this.resultados); // escreve o objeto no arquivo
        writer.close();
    }

    private void load_clients() throws IOException, ClassNotFoundException
    {
        File file = new File("data/clients.bin");
        if(file.exists()) //so caso o arquivo exite, le o arquivo
        {
            try
            {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(file)); // cria o leitor de arquivo
                this.usersAndKeys = (HashMap<String, String>) reader.readObject(); // le o objeto do arquivo e envia para o mapa
                reader.close();
            } catch (EOFException e)
            {
                this.usersAndKeys = new HashMap<>(); //caso deu erro na leitura do objeto, cria um limpo
            }
        }
        else
            this.usersAndKeys = new HashMap<>();
    }

    private void load_itens() throws IOException, ClassNotFoundException
    {
        File file = new File("data/itens.bin");
        if(file.exists()) //so caso o arquivo exite, le o arquivo
        {
            try
            {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(file)); // cria o leitor de arquivo
                this.items = (ArrayList<Item>) reader.readObject(); // le o objeto do arquivo e envia para o mapa
                reader.close();
            } catch (EOFException e)
            {
                this.items = new ArrayList<>(); //caso deu erro na leitura do objeto, cria um limpo
            }
        }
        else
            this.items = new ArrayList<>();
    }

    private void load_history() throws IOException, ClassNotFoundException
    {
        File file = new File("data/history.bin");
        if(file.exists()) //so caso o arquivo exite, le o arquivo
        {
            try
            {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(file)); // cria o leitor de arquivo
                this.resultados = (ArrayList<LeilaoResultado>) reader.readObject(); // le o objeto do arquivo e envia para o mapa
                reader.close();
            } catch (EOFException e)
            {
                this.resultados = new ArrayList<>(); //caso deu erro na leitura do objeto, cria um limpo
            }
        }
        else
            this.resultados = new ArrayList<>();
    }

    // classe do tipo TimerTask para ser usada repetidamente pelo timer - escreve os mapas no arquivo
    class WriterTask extends TimerTask
    {
        @Override
        public void run()
        {
            try
            {
                write_users();
                write_itens();
                write_history();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
