package org.example;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ServerSomthing extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ServerSomthing.class);
    private static final String FILENAME = "/file/forLogg";

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private String nick;

    public ServerSomthing(Socket socket) throws IOException {
        this.socket = socket;
        this.nick = nick;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        start();
    }
    @Override
    public void run() {
        String word;
        try {
            // первое сообщение отправленное сюда - это никнейм
            word = in.readLine();
            Server.users.addUser(word);
            try {
                out.write(word + "\n");
                this.nick = word;
                out.flush();
            } catch (IOException ignored) {logger.error("ignored nickname getting");}
            try {
                while (true) {
                    word = in.readLine();
                    if(word.equals("stop")) {
                        this.downService(); // харакири
                        break; // если пришла пустая строка - выходим из цикла прослушки
                    } else if(word.charAt(0) == '/') {
                        if(word.equals("/users")) {
                            for (ServerSomthing vr : Server.serverList) {
                                if (!vr.equals(this)) Server.users.printUsers(out);
                            }
                        } else localSend(word);
                    } else globalSend(word);
                }
            } catch (NullPointerException ignored) {logger.error("NullPointerException");}
        } catch (IOException e) {
            logger.error(String.valueOf(e));
            this.downService();
        }
    }

    private void localSend(String word) {
        logger.info("localEchoing:: " + word);
        int spaceIndex = word.indexOf(' ');
        String recipientNick = word.substring(1, spaceIndex);
        for (ServerSomthing vr : Server.serverList) {
            if (Objects.equals(vr.nick, recipientNick)) {
                vr.send(word.substring(spaceIndex + 1) + "   "); // отослать принятое сообщение с привязанного клиента всем остальным влючая его
            }
        }
    }

    private void globalSend(String word) {
        logger.info("Echoing:: " + word);
        for (ServerSomthing vr : Server.serverList) {
            if (!vr.equals(this)) {
                vr.send(word + "   "); // отослать принятое сообщение с привязанного клиента всем остальным влючая его
            }
        }
    }

    private void send(String msg) {
        try {
            out.write(msg + "\n");
            out.flush();
        } catch (IOException ignored) {}

    }

    private void downService() {
        try {
            if(!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (ServerSomthing vr : Server.serverList) {
                    if(vr.equals(this)) vr.interrupt();
                    Server.serverList.remove(this);
                }
            }
        } catch (IOException ignored) {logger.error("Ignored closing socket");}
    }
}


class Users {
    private final LinkedList<String> users = new LinkedList<>();

    public void addUser(String el) {
        users.add(el);
    }

    public void printUsers(BufferedWriter writer) {
        if(!users.isEmpty()) {
            try {
                writer.write("users:" + "\n");
                for (String vr : users) {
                    writer.write(vr + "\n");
                }
                writer.write("\n");
                writer.flush();
            } catch (IOException ignored) {}
        }
    }
}

public class Server {
    public static final int PORT = 8080;
    public static LinkedList<ServerSomthing> serverList = new LinkedList<>(); // список всех нитей - экземпляров
    // сервера, слушающих каждый своего клиента
    public static Users users;

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PORT)) {
            users = new Users();
            System.out.println("Server Started");
            while (true) {
                Socket socket = server.accept();
                try {
                    serverList.add(new ServerSomthing(socket)); // добавить новое соединенние в список
                } catch (IOException e) {
                    socket.close();
                }
            }
        }
    }
}