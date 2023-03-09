package japitest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import javax.swing.JFileChooser;

/**
 *
 * @author Slam
 */
public class JAPItest {

    static Writer writer;
    private static ServerSocket serverConnect;
    private static final int PORT = 8080;
    static int id = 0;

    private static void serverSocketCreate() {
        try {
            serverConnect = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
    }

    private static void logFileCreate() {
        File logFile = new File("log.txt");
        boolean result = false;
        try {
            result = Files.deleteIfExists(logFile.toPath());
            if (result) {
                logFile.createNewFile();
            }
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        serverSocketCreate();
        logFileCreate();
        while (true) {
            Socket s = serverConnect.accept();
            WorkerThread wt = new WorkerThread(s);
            Thread t = new Thread(wt);
            t.start();
            System.out.println("Thread number is: " + id);
        }
    }
}

class WorkerThread implements Runnable {

    private Socket s;
    private BufferedReader in;
    private DataOutputStream out;

    private static String MIME_TYPE;
    private static final String SUCCESS_HEADER = "HTTP/1.1 200 OK\r\n";
    private static final String ERROR_HEADER = "HTTP/1.1 404 Not Found\r\n";
    private static final String OUTPUT_HEADERS = "Content-Type: " + MIME_TYPE + "\r\nContent-Length: ";
    private static final String OUTPUT_END_OF_HEADERS = "\r\n\r\n";
    private static final String FILE_NOT_FOUND = "<html>\n<head>\n<title>\nError\n</title>\n</head>\n<body>\n<p>\n<h1>404-File Not Found</h1>\n</p>\n</body>\n</html>";

    public WorkerThread(Socket s) {
        this.s = s;
        try {
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            out = new DataOutputStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        JAPItest.id++;
    }

    private void closeConnection() throws IOException {        
        out.flush();
        out.close();
        in.close();
        s.close();
        //jAPItest.writer.close();
    }

    private void sendBytesAsData(byte[] data) throws IOException {
        if (data != null) {
            out.writeBytes(SUCCESS_HEADER);
            out.writeBytes(OUTPUT_HEADERS);
            out.write(data.length);
            out.writeBytes(OUTPUT_END_OF_HEADERS);
            out.write(data);
        }
    }

    private void sendStringAsData(String sData) throws IOException {
        if (!sData.isEmpty()) {
            out.writeBytes(SUCCESS_HEADER);
            out.writeBytes(OUTPUT_HEADERS);
            out.write(sData.length());
            out.writeBytes(OUTPUT_END_OF_HEADERS);
            out.writeBytes(sData);
        }
    }

    private void sendErrorMessage() throws IOException {
        out.writeBytes(ERROR_HEADER);
        out.writeBytes(OUTPUT_HEADERS);
        out.write(FILE_NOT_FOUND.length());
        out.writeBytes(OUTPUT_END_OF_HEADERS);
        out.writeBytes(FILE_NOT_FOUND);
    }

    private void setMimeType(String fileType) {
        if (fileType.equals("html")) {
            MIME_TYPE = "text/html";
        } else if (fileType.equals("mp4")) {
            MIME_TYPE = "video/mp4";
        } else if (fileType.equals("mpg")) {
            MIME_TYPE = "application/video";
        } else if (fileType.equals("mp3")) {
            MIME_TYPE = "application/sound";
        } else if (fileType.equals("ogg")) {
            MIME_TYPE = "application/sound";
        } else if (fileType.equals("png")) {
            MIME_TYPE = "image/png";
        } else if (fileType.equals("pdf")) {
            MIME_TYPE = "application/pdf";
        } else if (fileType.equals("jpg")) {
            MIME_TYPE = "image/jpg";
        } else if (fileType.equals("jpeg")) {
            MIME_TYPE = "image/jpeg";
        } else if (fileType.equals("bmp")) {
            MIME_TYPE = "image/bmp";
        } else if (fileType.equals("tiff")) {
            MIME_TYPE = "image/tiff";
        } else if (fileType.equals("tif")) {
            MIME_TYPE = "image/tiff";
        } else if (fileType.equals("default")) {
            MIME_TYPE = "text/html";
        }
    }

    private byte[] readFileIntoByteArray(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        FileInputStream fileInputStream = new FileInputStream(file);
        fileInputStream.read(data);
        fileInputStream.close();
        return data;
    }

    private void writeToLogFile(String message, String statusCode, int fileSize) throws IOException {
        JAPItest.writer.write(InetAddress.getByName("localHost").getHostAddress() + "--" + "[" + new Date().toString() + "] \"" + message + "\" " + statusCode + " " + fileSize);
        JAPItest.writer.flush();
    }

    private void addNewLineToLogFile() throws IOException {
        JAPItest.writer.write("\r\n");
        JAPItest.writer.flush();
    }

    private String readRequest() throws IOException {
        return in.readLine();
    }

    private int contentLength() throws IOException {
        String str;
        int postDataI = -1;
        while ((str = readRequest()) != null) {
            if (str.isEmpty()) {
                break;
            }
            final String contentHeader = "Content-Length: ";
            if (str.contains(contentHeader)) {
                postDataI = Integer.parseInt(str.substring(contentHeader.length()));
            }
        }
        return postDataI;
    }

    private String userName(int postDataI) throws IOException {
        String USER_DATA = null;
        char[] charArray = new char[postDataI];
        in.read(charArray);
        USER_DATA = new String(charArray);
        return USER_DATA;
    }

    private String modifyUserName(String USER_DATA) {
        USER_DATA = USER_DATA.replaceAll("\\+", " ");
        USER_DATA = USER_DATA.substring(USER_DATA.indexOf("=") + 1);
        return USER_DATA;
    }

    @Override
    public void run() {
        try {
            String index = "<html>\n"
                    + "<head><title>Socket Web</title></head>\n"
                    + "<body>\n"
                    + "<p><h1>WebSocket Main Page</h1></p>\n"
                    + "</body>\n"
                    + "</html>";
            String message = readRequest();
            System.out.println(message);
            if (message.equals("GET / HTTP/1.1")) {
                writeToLogFile(message, "200", message.length());
                setMimeType("text/plain");
                sendStringAsData(index);
            } else if (message.equals("GET /video.mp4 HTTP/1.1")) {
                JFileChooser chooser = new JFileChooser("D:\\Peliculas");
                int opc = chooser.showOpenDialog(null);
                if (opc == JFileChooser.APPROVE_OPTION) {
                    File upFile = chooser.getSelectedFile();
                    byte[] dataFile = readFileIntoByteArray(upFile);
                    sendBytesAsData(dataFile);
                }

            } else {
                String response = "HTTP/1.1 501 Not Implemented\n"
                        + "Server: " + InetAddress.getLocalHost().getHostAddress() + "\n"
                        + "Date: " + new Date() + "\n"
                        + "Content-type: \"text/plain\"\n"
                        + "Content-Length: 0";
                writeToLogFile(response, "501", response.length());
                sendStringAsData(response);
            }
            addNewLineToLogFile();
            closeConnection();            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
