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
        this.channelControl = new JChannel("auction.xml");
        this.channelControl.setReceiver(this);
        this.dispatcherControl = new RequestDispatcher(this.channelControl, this);

        this.channelView = new JChannel("auction.xml");
        this.channelView.setReceiver(this);
        this.dispatcherView = new RequestDispatcher(this.channelView, this);

        this.channelControl.connect("AuctionControlCluster");
        this.channelView.connect("AuctionViewCluster");
        eventloop();
        this.channelControl.close();
    }

    private void eventloop() throws Exception
    {
        while(true);
    }

    @Override
    public Object handle(Message message) throws Exception
    {
        if(message.getObject() instanceof AppMessage)
        {
            AppMessage messageReceived = (AppMessage) message.getObject();
            if(messageReceived.requisition == Requisition.VIEW_REQUEST_LOGIN)
                return login(messageReceived);
            else if(messageReceived.requisition == Requisition.VIEW_REQUEST_CREATE_USER)
                return create_user(messageReceived);

            return new AppMessage(Requisition.NOP, null);
        }
        else
        {
            System.err.println("[ERROR-MODEL] Enviou classe nao reconhecida");
            System.err.flush();
            return new AppMessage(Requisition.CLASS_ERROR, null);
        }
    }

    private Object login(AppMessage messageReceived) throws Exception
    {
        String[] userLoginRequest = (String[]) messageReceived.content;

        AppMessage controlLogin = new AppMessage(Requisition.CONTROL_REQUEST_LOGIN, userLoginRequest,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        List responses = dispatcherControl.sendRequestMulticast(controlLogin, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();
        if(responses.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_LOGIN, false);

        int nop_counter = 0;

        for(Object response : responses)
        {
            AppMessage msg = (AppMessage) response;
            if(msg.requisition == Requisition.MODEL_RESPONSE_LOGIN && ((boolean) msg.content) == false)
                return new AppMessage(Requisition.CONTROL_RESPONSE_LOGIN, false);
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        if(nop_counter == responses.size())
            return new AppMessage(Requisition.NOP, null);

        return new AppMessage(Requisition.CONTROL_RESPONSE_LOGIN, true);
    }

    private Object create_user(AppMessage messageReceived) throws Exception
    {
        String[] userCreateRequest = (String[]) messageReceived.content;
        AppMessage controlCreate = new AppMessage(Requisition.CONTROL_REQUEST_CREATE_USER, userCreateRequest,
                messageReceived.clientAddress, messageReceived.sequenceNumber);

        List responsesCreateUser = dispatcherControl.sendRequestMulticast(controlCreate, ResponseMode.GET_ALL, channelControl.getAddress()).getResults();
        if(responsesCreateUser.size() == 0)
            return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_USER, false);

        int nop_counter = 0;

        for(Object response : responsesCreateUser)
        {
            AppMessage msg = (AppMessage) response;
            if(msg.requisition == Requisition.MODEL_RESPONSE_CREATE_USER && ((boolean) msg.content == false))
                return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_USER, false);
            else if(msg.requisition == Requisition.NOP)
                nop_counter++;
        }

        if(nop_counter == responsesCreateUser.size())
            return new AppMessage(Requisition.NOP, null);
        return new AppMessage(Requisition.CONTROL_RESPONSE_CREATE_USER, true);
    }
}
