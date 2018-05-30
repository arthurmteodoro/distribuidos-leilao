import org.jgroups.Address;

import java.io.Serializable;

public class AppMessage implements Serializable
{
    public Requisition requisition;
    public Object content;
    public Address clientAddress;
    public int sequenceNumber;

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
