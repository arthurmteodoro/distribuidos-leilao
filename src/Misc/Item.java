package Misc;

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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;

        Item item = (Item) o;

        if (em_leilao != item.em_leilao) return false;
        if (!name.equals(item.name)) return false;
        if (!descricao.equals(item.descricao)) return false;
        if (!value.equals(item.value)) return false;
        return proprietario.equals(item.proprietario);
    }

    @Override
    public int hashCode()
    {
        int result = name.hashCode();
        result = 31 * result + descricao.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + proprietario.hashCode();
        result = 31 * result + (em_leilao ? 1 : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "Misc.Item{" +
                "name='" + name + '\'' +
                ", descricao='" + descricao + '\'' +
                ", value=" + value +
                ", proprietario='" + proprietario + '\'' +
                ", em_leilao=" + em_leilao +
                '}';
    }
}
