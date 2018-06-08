import java.io.Serializable;

public class Item implements Serializable
{
    private String name;
    private String descricao;
    private Double value;
    private String proprietario;
    private boolean em_leilao;

    public Item(String name, String descricao, Double value, String proprietario)
    {
        this.name = name;
        this.descricao = descricao;
        this.value = value;
        this.proprietario = proprietario;
        this.em_leilao = false;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescricao()
    {
        return descricao;
    }

    public void setDescricao(String descricao)
    {
        this.descricao = descricao;
    }

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }

    public String getProprietario()
    {
        return proprietario;
    }

    public void setProprietario(String proprietario)
    {
        this.proprietario = proprietario;
    }

    public boolean isEm_leilao()
    {
        return em_leilao;
    }

    public void setEm_leilao(boolean em_leilao)
    {
        this.em_leilao = em_leilao;
    }
}
