import org.jgroups.Address;

import java.io.Serializable;
import java.util.ArrayList;

public class Sala implements Serializable
{
    private Item item;
    private ArrayList<Lance> lances;
    private ArrayList<String> users;
    private ArrayList<Address> users_addr;
    private String leiloeiro_nome;
    private Address leiloeiro;

    public Sala(Item item, Address leiloeiro, String leiloeiro_nome)
    {
        this.item = item;
        this.lances = new ArrayList<>();
        this.users = new ArrayList<>();
        this.users_addr = new ArrayList<>();
        this.leiloeiro = leiloeiro;
        this.leiloeiro_nome = leiloeiro_nome;
    }

    public void insert_lance(Lance lance)
    {
        this.lances.add(lance);
    }

    public void insert_user(String nome, Address addr)
    {
        this.users.add(nome);
        this.users_addr.add(addr);
    }

    public ArrayList<Address> users_addr()
    {
        return this.users_addr;
    }

    @Override
    public String toString()
    {
        return this.leiloeiro_nome+"/"+this.item.getName();
    }
}
