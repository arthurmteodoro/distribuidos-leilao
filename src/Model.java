import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;

import java.util.HashMap;

public class Model extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelModel;

    private RequestDispatcher dispatcherModel;

    private HashMap<String, String> usersAndKeys;

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
        usersAndKeys.put("asd", "qwe");

        this.channelModel = new JChannel("auction.xml");
        this.channelModel.setReceiver(this);

        this.dispatcherModel = new RequestDispatcher(this.channelModel, this);

        this.channelModel.connect("AuctionModelCluster");
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
            System.out.println("\t[DEBUG] Modelo: Recebeu a mensagem com id "+messageReceived.identifier+" enviada de "+message.getSrc());

            if(messageReceived.identifier == AppMessage.MODEL_LOGIN)
            {
                String[] userAndKey = (String[]) messageReceived.content;
                System.out.println("\t[DEBUG] Modelo: Chave: "+userAndKey[0]+" Valor Recebido: "+userAndKey[1]);
                if(usersAndKeys.containsKey(userAndKey[0]))
                {
                    String value = usersAndKeys.get(userAndKey[0]);
                    System.out.println("\t[DEBUG] Modelo: Chave: "+userAndKey[0]+" Valor Recebido: "+userAndKey[1]+" Valor pegado: "+value);
                    if(value.equals(userAndKey[1]))
                    {
                        System.out.println("show");
                        return new AppMessage(AppMessage.MODEL_LOGIN_OK, null);
                    }
                    else
                    {
                        System.out.println("nao show");
                        return new AppMessage(AppMessage.MODEL_LOGIN_ERROR, null);
                    }
                }
                else
                {
                    System.out.println("nao show");
                    return new AppMessage(AppMessage.MODEL_LOGIN_ERROR, null);
                }
            }
            else if(messageReceived.identifier == AppMessage.MODEL_CREATE_USER)
            {
                String[] userAndKey = (String[]) messageReceived.content;
                if(usersAndKeys.containsKey(userAndKey[0]))
                    return new AppMessage(AppMessage.MODEL_CREATE_USER_ERROR, "User already exist");
                else
                {
                    usersAndKeys.put(userAndKey[0], userAndKey[1]);
                    return new AppMessage(AppMessage.MODEL_CREATE_USER_OK, "User created with success");
                }
            }
        }

        return new AppMessage(AppMessage.CLASS_ERROR, null);
    }
}
