package Misc;

import org.jgroups.Address;

import java.io.Serializable;
import java.util.Vector;

public class Sala implements Serializable
{
    private Item item;
    private Vector<Lance> lances;
    private Vector<String> users;
    private Vector<Address> users_addr;
    private String leiloeiro_nome;
    private Address leiloeiro;
    public int id;

    public Sala(Item item, Address leiloeiro, String leiloeiro_nome, int id)
    {
        this.id = id;
        this.item = item;
        this.lances = new Vector<>();
        this.users = new Vector<>();
        this.users_addr = new Vector<>();
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

    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    public Vector<Lance> getLances()
    {
        return lances;
    }

    public void setLances(Vector<Lance> lances)
    {
        this.lances = lances;
    }

    public Vector<String> getUsers()
    {
        return users;
    }

    public void setUsers(Vector<String> users)
    {
        this.users = users;
    }

    public Vector<Address> getUsers_addr()
    {
        return users_addr;
    }

    public void setUsers_addr(Vector<Address> users_addr)
    {
        this.users_addr = users_addr;
    }

    public String getLeiloeiro_nome()
    {
        return leiloeiro_nome;
    }

    public void setLeiloeiro_nome(String leiloeiro_nome)
    {
        this.leiloeiro_nome = leiloeiro_nome;
    }

    public Address getLeiloeiro()
    {
        return leiloeiro;
    }

    public void setLeiloeiro(Address leiloeiro)
    {
        this.leiloeiro = leiloeiro;
    }

    public Vector<Address> users_addr()
    {
        return this.users_addr;
    }

    @Override
    public String toString()
    {
        return this.leiloeiro_nome+"/"+this.item.getName();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Sala)) return false;

        Sala sala = (Sala) o;

        if (!item.equals(sala.item)) return false;
        if (!lances.equals(sala.lances)) return false;
        if (!users.equals(sala.users)) return false;
        if (!users_addr.equals(sala.users_addr)) return false;
        if (!leiloeiro_nome.equals(sala.leiloeiro_nome)) return false;
        return leiloeiro.equals(sala.leiloeiro);
    }

    @Override
    public int hashCode()
    {
        int result = item.hashCode();
        result = 31 * result + lances.hashCode();
        result = 31 * result + users.hashCode();
        result = 31 * result + users_addr.hashCode();
        result = 31 * result + leiloeiro_nome.hashCode();
        result = 31 * result + leiloeiro.hashCode();
        return result;
    }
}
