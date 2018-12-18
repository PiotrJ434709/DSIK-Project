import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;


public class client {

    public static void CloseConnection(DataOutputStream dos)
    {
        try {
            dos.writeByte('3');
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void DownloadFile(DataInputStream dis, DataOutputStream dos) {

        byte[] buffer = new byte[1024];
        try {

            dos.writeByte('1');

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Podaj sciezke: ");
            String path = input.readLine();

            System.out.println("Wysylam sciezke: " + path);
            dos.write(path.getBytes(), 0, path.length());

            long fileSize = dis.readLong();
	    fileSize = Long.reverseBytes(fileSize);

            if(fileSize == 0){
                System.out.println("Serwer: *** Plik nie istnieje *** \n");
                return;
            }

            System.out.println("Plik ma dlugosc: " + fileSize);
            System.out.println("Pobieram plik...");

            File f = new File(path);
            String fileName = f.getName();
            FileOutputStream fos = new FileOutputStream(fileName);

            int received;
            long allReceived = 0;

            while (allReceived < fileSize) {
                received = dis.read(buffer, 0, 1024);
                if (received < 0)
                    break;
                allReceived += received;
                fos.write(buffer, 0, received);
            }

            System.out.println("Odebrano lacznie: " + allReceived);
            fos.close();

            if (allReceived != fileSize)
                System.out.println("*** Blad w odbiorze pliku ***\n");
            else
                System.out.println("*** Plik odebrany poprawnie ***\n");
        }

        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void SendFile(DataInputStream dis, DataOutputStream dos) {

        byte[] buffer = new byte[1024];

        try {

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Podaj sciezke: ");
            String path = input.readLine();

            File f = new File(path);
            if(!f.exists()) {
                System.out.println("Zla sciezka, taki plik nie istnieje\n");
                return;
            }

            dos.writeByte('2');

            System.out.println("Wysylam sciezke: " + path);
            System.out.println("Dlugosc sciezki: " + path.length());

            path.trim();
            //System.out.println("Bajty sciezki: " + Arrays.toString(path.getBytes()));
            dos.write(path.getBytes(), 0, path.length());

            long fileSize = f.length();

            System.out.println("Plik ma rozmiar: " + fileSize);
            System.out.println("Wysylam rozmiar pliku...");

	    //fileSize = Long.reverseBytes(fileSize);
	    //dos.writeLong(fileSize);
	    
            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putLong(fileSize);

            dos.write(buf.array(), 0, Long.BYTES);

            System.out.println("Wysylam plik...");

            int read;
            long allRead = 0;

            FileInputStream fis = new FileInputStream(f);

            while (allRead < fileSize)
            {
                read = fis.read(buffer, 0, 1024);
                dos.write(buffer, 0 ,read);
                allRead += read;
            }
            System.out.println("Przeczytano (i wyslano): " + allRead + " bajtow");

            fis.close();

            if (allRead == fileSize)
                System.out.println("*** Plik wyslany poprawnie ***\n");
            else
                System.out.println("*** Blad przy wysylaniu pliku ***\n");
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }



    public static void Menu(DataInputStream dis, DataOutputStream dos) {

        while (true) {

            System.out.println("1. Pobierz plik z serwera");
            System.out.println("2. Wyslij plik na serwer");
            System.out.println("3. Koniec");

            try {

                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

                System.out.print("Wybor: ");
                int option = Integer.parseInt(input.readLine());

                switch(option) {
                    case 1:
                        DownloadFile(dis, dos);
                        break;
                    case 2:
                        SendFile(dis, dos);
                        break;
                    case 3:
                        CloseConnection(dos);
                        return;
                    default:
                        System.out.println("Nie ma takiej opcji w menu\n");
                        break;
                }
            }
            catch (Exception e) {
                System.out.println(" *** Menu error *** ");
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {


        if (args.length < 2) {
            System.out.println("Podaj IP/nazwe serwera i port");
            return;
        }
        try {

            InetAddress addr = InetAddress.getByName(args[0]);
            int port = Integer.parseInt(args[1]);

            Socket socket = new Socket(addr, port);

            DataInputStream dis = new DataInputStream(
                    socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(
                    socket.getOutputStream());

            Menu(dis, dos);
            socket.close();
            dos.close();
            dis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Klient zakonczyl dzialanie");
    }
}


