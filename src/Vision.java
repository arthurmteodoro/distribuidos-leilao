import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;

import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class Vision extends ReceiverAdapter implements RequestHandler
{
    private JChannel channelVision;
    private RequestDispatcher dispatcherVision;

    private JChannel channelControl;
    private RequestDispatcher dispatcherControl;

    private boolean connectedInControl;
    private Timer timer;

    public static void main(String[] args)
    {
        try
        {
            new Vision().start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void start() throws Exception
    {
        this.connectedInControl = false;

        this.channelVision = new JChannel("auction.xml");
        this.channelVision.setReceiver(this);

        this.channelVision.connect("AuctionVisionCluster");
        this.dispatcherVision = new RequestDispatcher(channelVision, this);

        this.channelControl = new JChannel("auction.xml");
        this.channelControl.setReceiver(this);

        this.channelControl.connect("AuctionControlCluster");
        this.dispatcherControl = new RequestDispatcher(channelControl, this);

        /*this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                if(!connectedInControl)
                {
                    try
                    {
                        channelControl = new JChannel("cast.xml");
                        channelControl.setReceiver(Vision.this);

                        channelControl.connect("AuctionControlCluster");

                        if(channelControl.getView().size() <= 2)
                        {
                            connectedInControl = true;
                            dispatcherControl = new RequestDispatcher(channelControl, Vision.this);
                        }
                        else
                        {
                            channelControl.disconnect();
                            channelControl.close();
                        }
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        }, 0, 500000);*/

        eventloop();
        this.channelVision.close();
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

                    String[] userAndPassword = {user, password};
                    AppMessage loginMessage = new AppMessage(AppMessage.VISION_CREATE_USER, userAndPassword);
                    AppMessage controlResponse = (AppMessage) dispatcherVision.sendRequestMulticast(loginMessage, ResponseMode.GET_FIRST, channelControl.getAddress()).getFirst();

                    if(controlResponse.identifier == AppMessage.VISION_CREATE_USER_OK)
                        System.out.println("user created successfully");
                    else
                        System.out.println("user creation failure");
                }
                else if(input.equals("login"))
                {
                    System.out.println(channelControl.getView());

                    System.out.print("User: ");
                    String user = keyboard.nextLine().toLowerCase();
                    System.out.print("Password: ");
                    String password = keyboard.nextLine().toLowerCase();

                    String[] userAndPassword = {user, password};
                    AppMessage loginMessage = new AppMessage(AppMessage.VISION_LOGIN, userAndPassword);
                    AppMessage controlResponse = (AppMessage) dispatcherVision.sendRequestMulticast(loginMessage, ResponseMode.GET_FIRST, channelControl.getAddress()).getFirst();
                    if(controlResponse.identifier == AppMessage.VISION_LOGIN_OK)
                        System.out.println("User successfully logged in");
                    else
                        System.out.println("Error during user login");

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
            if(channelControl.isConnected()) //envia as mensagens da visao para o controle so se estiver conectado
            {
                if(messageReceived.identifier == AppMessage.VISION_LOGIN)//pedido de login da visao
                {
                    String[] userAndKey = (String[]) messageReceived.content;
                    AppMessage loginMessage = new AppMessage(AppMessage.CONTROL_LOGIN, userAndKey);

                    RspList controlMessage = dispatcherControl.sendRequestMulticast(loginMessage, ResponseMode.GET_FIRST, channelControl.getAddress());
                    AppMessage controlResponse = (AppMessage) controlMessage.getFirst();

                    if (controlResponse.identifier == AppMessage.CONTROL_LOGIN_OK)
                        return new AppMessage(AppMessage.VISION_LOGIN_OK, controlResponse.content);
                    else
                        return new AppMessage(AppMessage.VISION_LOGIN_ERROR, controlResponse.content);
                }
                else if(messageReceived.identifier == AppMessage.VISION_CREATE_USER)
                {
                    String[] userAndKey = (String[]) messageReceived.content;
                    AppMessage registerMessage = new AppMessage(AppMessage.CONTROL_CREATE_USER, userAndKey);

                    RspList controlMessage = dispatcherControl.sendRequestMulticast(registerMessage, ResponseMode.GET_FIRST, channelControl.getAddress());
                    AppMessage controlResponse = (AppMessage) controlMessage.getFirst();

                    if (controlResponse.identifier == AppMessage.CONTROL_CREATE_USER_OK)
                        return new AppMessage(AppMessage.VISION_CREATE_USER_OK, controlResponse.content);
                    else
                        return new AppMessage(AppMessage.VISION_CREATE_USER_ERROR, controlResponse.content);
                }
            }
        }

        return new AppMessage(AppMessage.CLASS_ERROR, null);
    }
}
