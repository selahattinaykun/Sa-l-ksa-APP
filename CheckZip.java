import java.util.zip.ZipFile;
import java.io.File;

public class CheckZip {
    public static void main(String[] args) {
        try {
            File f = new File(args[0]);
            ZipFile zf = new ZipFile(f);
            zf.close();
            System.out.println("Valid zip: " + f.getName());
        } catch (Exception e) {
            System.out.println("Invalid zip " + args[0] + ":");
            e.printStackTrace();
        }
    }
}
