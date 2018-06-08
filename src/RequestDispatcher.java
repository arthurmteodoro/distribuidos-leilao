import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;

import java.util.Collection;

public class RequestDispatcher
{
    private MessageDispatcher dispatcher;

    public RequestDispatcher(JChannel channel, RequestHandler requestHandler)
    {
        // cria o dispachante somente o canal passado nao seja nulo
        if(channel != null)
        {
            this.dispatcher = new MessageDispatcher(channel, null, null, requestHandler);
        }
    }

    // funcao para enviar as respostas via multicast
    public RspList sendRequestMulticast(Object value, ResponseMode responseMode) throws Exception
    {
        Message message = new Message(null, value); // cria uma nova mensagem sem destinatarios

        // cria as opcoes de envio e coloca os modos de resposta e coloca que nao e uma mensagem anycast
        RequestOptions options = new RequestOptions();
        options.setMode(responseMode);
        options.setAnycasting(false);

        // retorna o resultado do dispachante do envio da mensagem sem destinatario (multicast)
        return this.dispatcher.castMessage(null, message, options);
    }

    // funcao para envio de mensagem multicast, porem excluindo alguem
    public RspList sendRequestMulticast(Object value, ResponseMode responseMode, Address removeAdd) throws Exception
    {
        Message message = new Message(null, value);

        // coloca que e uma mensagem multicast e que nao deve ser enviada para o endereco informado
        RequestOptions options = new RequestOptions();
        options.setMode(responseMode);
        options.setAnycasting(false);
        options.setExclusionList(removeAdd);

        return this.dispatcher.castMessage(null, message, options);
    }

    // funcao para enviar a mensagem de forma anycast
    public RspList sendRequestAnycast(Collection<Address> cluster, Object value, ResponseMode responseMode) throws Exception
    {
        Message message = new Message(null, value);

        RequestOptions options = new RequestOptions();
        options.setMode(responseMode);
        options.setAnycasting(true);

        return this.dispatcher.castMessage(cluster, message, options);
    }

    public RspList sendRequestAnycast(Collection<Address> cluster, Object value, ResponseMode responseMode, Address remove) throws Exception
    {
        Message message = new Message(null, value);

        RequestOptions options = new RequestOptions();
        options.setMode(responseMode);
        options.setAnycasting(true);
        options.setExclusionList(remove);

        return this.dispatcher.castMessage(cluster, message, options);
    }

    // funcao para enviar uma mensagem de forma unicast
    public Object sendRequestUnicast(Address receiver, Object value, ResponseMode responseMode) throws Exception
    {
        Message message = new Message(receiver, value);

        RequestOptions options = new RequestOptions();
        options.setMode(responseMode);

        return this.dispatcher.sendMessage(message, options);
    }
}
