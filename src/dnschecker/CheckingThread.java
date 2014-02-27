package dnschecker;

import java.util.List;

/**
 *
 * @author LoadLow
 */
public class CheckingThread implements Runnable {

    private List<String> linesToCheck;

    public CheckingThread(List<String> lines) {
        this.linesToCheck = lines;
    }
    public boolean workFinish = false;

    @Override
    public void run() {
        int i = 0;
        for (String line : linesToCheck) {
            try {
                boolean recursive = false;

                if (DNSChecker.CheckedReflectors.containsKey(line)) {
                    continue;
                }
                int size = Integer.parseInt(line.split(" ")[2]);
                try {
                    recursive = DNSChecker.isRecursive(line);
                    if (recursive) {
                        i++;
                        if (i > 100) {
                            System.out.println("+100 Reflectors!");
                            i=0;
                        }
                    }
                } catch (Exception e) {
                }
                DNSChecker.CheckedReflectors.put(line, new Couple(size, recursive));
            } catch (Exception e) {
            }
        }
        workFinish = true;
    }
}