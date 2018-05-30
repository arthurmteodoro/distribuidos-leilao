import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;

import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

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
        this.sequenceNumber = -1;

        this.channelView = new JChannel("auction.xml");
        this.channelView.setReceiver(this);
        this.dispatcherView = new RequestDispatcher(channelView, this);

        this.channelView.connect("AuctionViewCluster");
        eventloop();
        this.channelView.close();
    }

    private void eventloop() throws Exception
    {
        boolean exit = false;
        Scanner keyboard = new Scanner(System.in);
        String input = "";

        while(!exit)
        {
            System.out.print(">");
            input = keyboard.nextLine().toLowerCase();

            if(input.startsWith("exit"))
                System.exit(0);
            else
            {
                if(input.equals("create user"))
                {
                    System.out.print("User: ");
                    String user = keyboard.nextLine().toLowerCase();
                    System.out.print("Password: ");
                    String password = keyboard.nextLine().toLowerCase();

                    if(createUser(user, password))
                        System.out.println("user created successfully");
                    else
                        System.out.println("user creation failure");
                }
                else if(input.equals("login"))
                {
                    System.out.print("User: ");
                    String user = keyboard.nextLine().toLowerCase();
                    System.out.print("Password: ");
                    String password = keyboard.nextLine().toLowerCase();

                    if(login(user, password))
                        System.out.println("Login ok");
                    else
                        System.out.println("Login error");
                }
            }
        }
    }

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

    private boolean login(String user, String password) throws Exception
    {
        String[] content = {user, password};
        AppMessage loginMessage = new AppMessage(Requisition.VIEW_REQUEST_LOGIN, content,
                channelView.getAddress(), sequenceNumber);
        this.sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(loginMessage, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
            return false;

        for(Object value : controlResponse)
        {
            AppMessage response = (AppMessage) value;
            if(response.requisition == Requisition.CONTROL_RESPONSE_LOGIN_ERROR)
                return false;
        }
        return true;
    }

    private boolean createUser(String user, String password) throws Exception
    {
        String[] content = {user, password};
        AppMessage createUser = new AppMessage(Requisition.VIEW_REQUEST_CREATE_USER, content,
                channelView.getAddress(), sequenceNumber);
        this.sequenceNumber++;

        List controlResponse = dispatcherView.sendRequestMulticast(createUser, ResponseMode.GET_ALL, channelView.getAddress()).getResults();

        if(controlResponse.size() == 0)
            return false;

        for(Object value : controlResponse)
        {
            AppMessage response = (AppMessage) value;
            if(response.requisition == Requisition.CONTROL_RESPONSE_CREATE_USER_ERROR)
                return false;
        }
        return true;
    }
}
