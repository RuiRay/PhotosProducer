package utils;

import java.io.*;

public class CmdUtil {

    // https://docs.oracle.com/javase/6/docs/api/java/lang/ProcessBuilder.html
    public static String execCmd(File cmdDir, boolean requestResult, String... cmds) {
        InputStream processInputStream = null;
        try {
            Process process = Runtime.getRuntime().exec(cmds, null, cmdDir);
            processInputStream = process.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(processInputStream));
            StringBuilder cmdSB = new StringBuilder();
            String cmdOutput;
            while ((cmdOutput = read.readLine()) != null) {
                if (!requestResult) {
                    printlnOutput(cmdOutput);
                    continue;
                }
                cmdSB.append(cmdOutput).append("\n");
            }
            return cmdSB.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileUtil.closeIO(processInputStream);
        }
        return "";
    }

    private static String lastOutput;

    private static void printlnOutput(String cmdOutput) {
//      System.out.print(cmdOutput + "\r");
        if (!cmdOutput.equals(lastOutput)) {
            System.out.println(cmdOutput);
        }
        lastOutput = cmdOutput;
    }

}
