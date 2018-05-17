import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.util.Set;

public class Control extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private JChannel channelModel;
    private RequestDispatcher dispatcherModel;

    private boolean connectedInModel;

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
        this.connectedInModel = false;

        this.channelControl = new JChannel("auction.xml");
        this.channelControl.setReceiver(this);
        this.dispatcherControl = new RequestDispatcher(this.channelControl, this);

        this.channelModel = new JChannel("auction.xml");
        this.channelModel.setReceiver(this);
        this.dispatcherModel = new RequestDispatcher(this.channelModel, this);

        this.channelModel.connect("AuctionModelCluster");

        this.channelControl.connect("AuctionControlCluster");
        eventloop();
        this.channelControl.close();
    }

    private void eventloop() throws Exception
    {
        while(true)
        {
            /*if(!connectedInModel)
            {
                this.channelModel = new JChannel("cast.xml");
                this.channelModel.setReceiver(this);

                this.channelModel.connect("AuctionModelCluster");
                if(this.channelModel.getView().size() <= 2)
                {
                    connectedInModel = true;
                    this.dispatcherControl = new RequestDispatcher(channelModel, this);
                }
                else
                {
                    this.channelModel.close();
                }
            }

            Util.sleep(500000);*/
        }
    }

    @Override
    public Object handle(Message message) throws Exception
    {
        if(message.getObject() instanceof AppMessage)
        {
            AppMessage messageReceived = (AppMessage) message.getObject();

            // Caso tal processo esteja conectado tanto na visao como no modelo, este deve enviar a mensagem
            // ao modelo, funcionando como um no relay
            if(channelModel.isConnected())
            {
                if (messageReceived.identifier == AppMessage.CONTROL_LOGIN)
                {
                    System.out.println("ASD");
                    String[] userAndKey = (String[]) messageReceived.content;
                    AppMessage loginMessage = new AppMessage(AppMessage.MODEL_LOGIN, userAndKey);

                    RspList modelMessage = dispatcherModel.sendRequestMulticast(loginMessage, ResponseMode.GET_FIRST, channelModel.getAddress());
                    AppMessage modelResponse = (AppMessage) modelMessage.getFirst();

                    if (modelResponse.identifier == AppMessage.MODEL_LOGIN_OK)
                        return new AppMessage(AppMessage.CONTROL_LOGIN_OK, modelResponse.content);
                    else
                        return new AppMessage(AppMessage.CONTROL_LOGIN_ERROR, modelResponse.content);
                }
                else if(messageReceived.identifier == AppMessage.CONTROL_CREATE_USER)
                {
                    System.out.println("\t[DEBUG] Controle: Pedido para criar um novo usuario");
                    System.out.println("\t[DEBUG] Controle: Visao do modelo: "+channelModel.getView());
                    String[] userAndKey = (String[]) messageReceived.content;
                    AppMessage registerMessage = new AppMessage(AppMessage.MODEL_CREATE_USER, userAndKey);

                    System.out.println("\t[DEBUG] Controle: Vai pedir para o modelo - todos do modelo exceto eu");
                    RspList modelMessages = dispatcherModel.sendRequestMulticast(registerMessage, ResponseMode.GET_ALL, channelModel.getAddress());
                    System.out.println("\t[DEBUG] Controle: Resultado do modelo: "+((AppMessage)modelMessages.getFirst()).identifier);

                    Set keySetModel = modelMessages.keySet();
                    for(Object add : keySetModel)
                    {
                        AppMessage modelMessage = (AppMessage) modelMessages.get(add).getValue();
                        if(modelMessage.identifier == AppMessage.MODEL_CREATE_USER_ERROR)
                            return new AppMessage(AppMessage.CONTROL_CREATE_USER_ERROR, modelMessage.content);
                    }

                    System.out.println("\t[DEBUG] Controle: Vai enviar que deu bom");
                    return new AppMessage(AppMessage.CONTROL_CREATE_USER_OK, "User created with success");
                }
            }
        }

        return new AppMessage(AppMessage.CLASS_ERROR, null);
    }
}
