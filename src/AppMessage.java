import org.jgroups.Address;

import java.io.Serializable;

public class AppMessage implements Serializable
{
    public Requisition requisition; // tipo da requisicao
    public Object content; // conteudo da mensagem
    public Address clientAddress; // endereco do cliente da visao que vez a primeira requisicao (no canal de visao) (usada controle de duplicatas)
    public int sequenceNumber; // numero de sequencia do cliente da visao que vez a requisicao no canal da visao (usada para controle de duplicatas)

    public AppMessage(Requisition requisition, Object content)
    {
        this.requisition = requisition;
        this.content = content;
    }

    public AppMessage(Requisition requisition, Object content, Address src, int sequence)
    {
        this.requisition = requisition;
        this.content = content;
        this.clientAddress = src;
        this.sequenceNumber = sequence;
    }
}
