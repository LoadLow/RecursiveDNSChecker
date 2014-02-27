package dnschecker;

import com.appspot.trent.denis.DnsRequest;
import com.appspot.trent.denis.DnsResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author LoadLow
 */
public class DNSChecker {

    /**
     * @param args the command line arguments
     */
    public static volatile ConcurrentHashMap<String, Couple<Integer, Boolean>> CheckedReflectors = new ConcurrentHashMap<String, Couple<Integer, Boolean>>();

    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("addresses.list"));
            String line;

            ArrayList<String> lines = new ArrayList<String>();
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
            ArrayList<CheckingThread> threads = new ArrayList<CheckingThread>();
            for (int i = 0; i < lines.size(); i += 10000) {
                int iEnd = i + 10000;
                if (iEnd > lines.size() - 1) {
                    iEnd = lines.size() - 1;
                }
                CheckingThread current = new CheckingThread(lines.subList(i, iEnd));
                Thread t = new Thread(current);
                t.start();
                threads.add(current);
            }
            System.out.println("Threads started = " + threads.size());
            while (true) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                }
                System.out.println(CheckedReflectors.size() + "/" + lines.size() + " checked...");
                if (totalWorkFinished(threads)) {
                    break;
                }
            }

            System.out.println("===================");
            System.out.println("Reflectors Checked!");
            System.out.println(lines.size() + " lines checked!");
            System.out.println("Saving on file...");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("addresses.list.CHECKED", true)), false);
            int totalRealReflectors = 0;
            for (Entry<String, Couple<Integer, Boolean>> reflector : CheckedReflectors.entrySet()) {
                if (reflector.getValue().second) {
                    out.println(reflector.getKey() + " <resolver> " + reflector.getValue().first);
                    totalRealReflectors++;
                }
            }
            System.out.println(totalRealReflectors + "/" + CheckedReflectors.size() + " real reflectors found&saved!");
            out.flush();
            out.close();
            CheckedReflectors.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean totalWorkFinished(ArrayList<CheckingThread> threads) {
        for (CheckingThread thread : threads) {
            if (!thread.workFinish) {
                return false;
            }
        }
        return true;
    }

    public static boolean isRecursive(String host) throws Exception {

        DatagramSocket socket = new DatagramSocket();
        byte buffer[] = DnsRequest.constructPacket(1234, 0x100, "google.com");
        DatagramPacket p = new DatagramPacket(buffer, buffer.length);
        p.setAddress(Inet4Address.getByName(host));
        p.setPort(53);
        socket.send(p);
        byte recvBuffer[] = new byte[1024];
        p = new DatagramPacket(recvBuffer, recvBuffer.length);
        socket.setSoTimeout(1000);
        socket.receive(p);
        DnsResponse response = new DnsResponse(recvBuffer, p.getLength());
        short flags = response.getFlags();
        boolean isResponse = ((flags >> DNS.SHIFT_QUERY) & 1) != 0;
        if (!isResponse) {
            throw new IOException("Response flag not set");
        }
        return ((flags >> DNS.SHIFT_RECURSE_AVAILABLE) & 1) != 0;
    }
}
