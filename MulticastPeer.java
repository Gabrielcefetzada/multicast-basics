
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Scanner;

public class MulticastPeer {

    private String username;
    private InetAddress group;
    private MulticastSocket socket;
    private int port = 6789;
    private boolean running = true;

    public MulticastPeer(String username, String groupAddress) throws IOException {
        this.username = username;
        this.group = InetAddress.getByName(groupAddress);
        this.socket = new MulticastSocket(port);
    }

    @SuppressWarnings("deprecation")
    public boolean enterRoom() {
        try {
            socket.joinGroup(group);
            sendMessage("Usuário " + username + " entrou na sala.");
            return true;
        } catch (IOException e) {
            System.err.println("Não foi possível entrar na sala: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public boolean leaveRoom() {
        try {
            sendMessage("Usuário " + username + " saiu da sala.");
            socket.leaveGroup(group);
            socket.close();
            return true;
        } catch (IOException e) {
            System.err.println("Não foi possível sair da sala: " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            String fullMessage = username + ": " + message;
            byte[] buffer = fullMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    public void receiveMessages() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(message);
                }

            } catch (SocketException e) {
                System.err.println("Socket fechado.");
            } catch (IOException e) {
                System.err.println("Erro ao receber mensagem: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java ChatApp <usuario> <endereço_multicast>");
            return;
        }

        String username = args[0];
        String groupAddress = args[1];

        try {
            MulticastPeer chatApp = new MulticastPeer(username, groupAddress);
            if (!chatApp.enterRoom()) {
                return;
            }

            chatApp.receiveMessages();

            Scanner scanner = new Scanner(System.in);
            while (chatApp.running) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("sair")) {
                    chatApp.running = false;
                    chatApp.leaveRoom();
                } else {
                    chatApp.sendMessage(message);
                }
            }

            scanner.close();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o chat: " + e.getMessage());
        }
    }
}