package Misc;

import java.io.Serializable;

public class Lance implements Serializable
{
    private String user;
    private Double value;
    public int sala;

    public Lance(String user, Double value, int sala)
    {
        this.user = user;
        this.value = value;
        this.sala = sala;
    }

    @Override
    public String toString()
    {
        return "Usuario: "+user+" Valor: "+value;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }


}
