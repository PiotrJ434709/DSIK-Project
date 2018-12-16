import java.io.*;
import java.net.*;
import java.nio.*;

public class client {

    public static void CloseConnection(DataOutputStream dos)
    {
        String option = "3";

        try {
            dos.write(option.getBytes(), 0, option.length());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void DownloadFile(DataInputStream dis, DataOutputStream dos) {

        byte[] buffer = new byte[1024];
        String option = "1";

        try {
	    
            dos.write(option.getBytes(), 0, option.length());

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Podaj sciezke: ");
            String path = input.readLine();

            System.out.println("Wysylam sciezke: " + path);
            dos.write(path.getBytes(), 0, path.length());

            int fileSize = dis.readInt();
            dis.skipBytes(4);

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
            int allReceived = 0;

            while (allReceived < fileSize) {                              //odczytuje plik
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
        }
    }

    public static void SendFile(DataInputStream dis, DataOutputStream dos) {

        byte[] buffer = new byte[1024];
        String option = "2";

        try {

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("Podaj sciezke: ");
            String path = input.readLine();

            File f = new File(path);
            if(!f.exists()) {
                System.out.println("Zla sciezka, taki plik nie istnieje\n");
                return;
            }

	    dos.write(option.getBytes(), 0, option.length());

            long fileSize = f.length();

            System.out.println("Plik ma rozmiar: " + fileSize);
            System.out.println("Wysylam rozmiar pliku...");

            ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putLong(fileSize);

            dos.write(buf.array(), 0, Long.BYTES);

            System.out.println("Wysylam sciezke: " + path);
            dos.write(path.getBytes(), 0, path.length());

            System.out.println("Wysylam plik...");

            int read;
            int allRead = 0;

            FileInputStream fis = new FileInputStream(f);

            while (allRead < fileSize)							                    //wysylaj plik
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
                System.out.println("*menu error*\n");
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

