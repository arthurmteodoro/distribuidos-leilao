package Misc;

import Misc.Item;
import Misc.Lance;

import java.io.Serializable;
import java.util.List;

public class LeilaoResultado implements Serializable
{
    public String usuario;
    public Item item;
    public Double valor;
    public List<Lance> historico_lances;

    public LeilaoResultado(String usuario, Item item, Double valor, List<Lance> historico)
    {
        this.usuario = usuario;
        this.item = item;
        this.valor = valor;
        this.historico_lances = historico;
    }

    @Override
    public String toString()
    {
        return "Ganhador: "+this.usuario+"\tMisc.Item: "+this.item.getName()+"\tValor: "+this.valor;
    }
}
