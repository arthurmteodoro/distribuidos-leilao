import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class Utils
{
    // funcao para transformar um vetor de bytes em uma string em hexa
    private static String byteArray2Hex(final byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    // funcao para gerar uma hash do tipo sha 256 do objeto
    public static String gerarSHA256(byte[] convertme) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return byteArray2Hex(md.digest(convertme));
    }
}
