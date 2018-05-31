import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;

import java.io.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/*
* TODO: Mudar a forma de não existir mensagem duplicadas vindas da visão (porque da forma que está o modelo necessita de dados vindo da visão)
*/

public class Model extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelModel;
    private RequestDispatcher dispatcherModel;

    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private HashMap<String, String> usersAndKeys;
    private HashMap<Address, Integer> seqs;

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
        this.usersAndKeys = new HashMap<>();
        this.seqs = new HashMap<>();

        this.channelModel = new JChannel("auction.xml");
        this.channelModel.setReceiver(this);
        this.dispatcherModel = new RequestDispatcher(this.channelModel, this);

        this.channelControl = new JChannel("auction.xml");
        this.channelControl.setReceiver(this);
        this.dispatcherControl = new RequestDispatcher(this.channelControl, this);

        this.channelModel.connect("AuctionModelCluster");
        this.channelControl.connect("AuctionControlCluster");

        load_clients();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new WriterTask(), 5000, 5000);

        eventLoop();
        this.channelModel.close();
    }

    private void eventLoop() throws Exception
    {
        while(true);
    }

    @Override
    public Object handle(Message message) throws Exception
    {
        if(message.getObject() instanceof AppMessage)
        {
            AppMessage messageReceived = (AppMessage) message.getObject();
            if(!seqs.containsKey(messageReceived.clientAddress) || seqs.get(messageReceived.clientAddress) < messageReceived.sequenceNumber)
            {
                if(!seqs.containsKey(messageReceived.clientAddress))
                    seqs.put(messageReceived.clientAddress, messageReceived.sequenceNumber);
                else
                {
                    seqs.remove(messageReceived.clientAddress);
                    seqs.put(messageReceived.clientAddress, messageReceived.sequenceNumber);
                }

                if (messageReceived.requisition == Requisition.CONTROL_REQUEST_LOGIN)
                {
                    String[] userLoginRequest = (String[]) messageReceived.content;
                    if (checkLogin(userLoginRequest[0], userLoginRequest[1]))
                        return new AppMessage(Requisition.MODEL_RESPONSE_LOGIN, true);
                    else
                        return new AppMessage(Requisition.MODEL_RESPONSE_LOGIN, false);
                }
                else if (messageReceived.requisition == Requisition.CONTROL_REQUEST_CREATE_USER)
                {
                    String[] userCreateRequest = (String[]) messageReceived.content;
                    if (createUser(userCreateRequest[0], userCreateRequest[1]))
                        return new AppMessage(Requisition.MODEL_RESPONSE_CREATE_USER, true);
                    else
                        return new AppMessage(Requisition.MODEL_RESPONSE_CREATE_USER, false);
                }
            }

            return new AppMessage(Requisition.NOP, null);
        }
        else
            return new AppMessage(Requisition.CLASS_ERROR, null);
    }

    private boolean checkLogin(String user, String key)
    {
        if(usersAndKeys.containsKey(user))
        {
            String keyInHash = usersAndKeys.get(user);
            return keyInHash.equals(key);
        }
        return false;
    }

    private boolean createUser(String user, String key) throws IOException
    {
        if(!usersAndKeys.containsKey(user))
        {
            usersAndKeys.put(user, key);
            write_users();
            return true;
        }
        return false;
    }

    private void write_users() throws IOException
    {
        File file = new File("data/clients.bin");
        if(!file.exists())
            file.createNewFile();

        ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file));
        writer.writeObject(this.usersAndKeys);
        writer.close();
    }

    private void load_clients() throws IOException, ClassNotFoundException
    {
        File file = new File("data/clients.bin");
        if(file.exists())
        {
            try
            {
                ObjectInputStream reader = new ObjectInputStream(new FileInputStream(file));
                this.usersAndKeys = (HashMap<String, String>) reader.readObject();
                reader.close();
            } catch (EOFException e)
            {
                this.usersAndKeys = new HashMap<>();
            }
        }
    }

    class WriterTask extends TimerTask
    {
        @Override
        public void run()
        {
            try
            {
                write_users();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
