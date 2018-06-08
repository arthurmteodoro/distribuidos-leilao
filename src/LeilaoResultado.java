import java.io.Serializable;

public class LeilaoResultado implements Serializable
{
    public String usuario;
    public Item item;
    public Double valor;

    public LeilaoResultado(String usuario, Item item, Double valor)
    {
        this.usuario = usuario;
        this.item = item;
        this.valor = valor;
    }
}
