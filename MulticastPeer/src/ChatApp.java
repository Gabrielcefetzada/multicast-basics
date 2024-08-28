
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.Scanner;
import java.net.NetworkInterface;

public class ChatApp {

    private final int port = 6789;
    private boolean running = true;
    private final NetworkInterface netIf;
    private final String username;
    private final InetAddress group;
    private final MulticastSocket socket;

    public ChatApp(String username, String groupAddress, NetworkInterface netIf) throws IOException {
        this.username = username;
        this.group = InetAddress.getByName(groupAddress);
        this.socket = new MulticastSocket(port);
        this.netIf = netIf;
    }

    public boolean enterRoom() {
        try {
            socket.joinGroup(new java.net.InetSocketAddress(group, 0), netIf);
            sendMessage("Usuário " + username + " entrou na sala.");
            return true;
        } catch (IOException e) {
            System.err.println("Não foi possível entrar na sala: " + e.getMessage());
            return false;
        } catch (Exception ex) {
            System.err.println("Erro: " + ex.getMessage());
            return false;
        }
    }

    public void leaveRoom() {
        try {
            sendMessage("Usuário " + username + " saiu da sala.");
            socket.leaveGroup(new java.net.InetSocketAddress(group, 0), netIf);
            socket.close();
        } catch (IOException e) {
            System.err.println("Não foi possível sair da sala: " + e.getMessage());
        } catch (Exception ex) {
            System.err.println("Erro: " + ex.getMessage());
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
        } catch (Exception ex) {
            System.err.println("Erro: " + ex.getMessage());
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
            } catch (Exception ex) {
                System.err.println("Erro: " + ex.getMessage());
            }
        }).start();
    }

        public static void main(String[] args) {
            if (args.length != 2) {
                System.out.println("Você deve passar 2 argumentos: <usuario> <endereço_multicast>");
                return;
            }

            String username = args[0];
            String groupAddress = args[1];

            try {
                NetworkInterface netIf = NetworkInterface.getByName("eth0");

                ChatApp multicastChannel = new ChatApp(username, groupAddress, netIf);

                if (!multicastChannel.enterRoom()) {
                    System.out.println("Não foi possível entrar na sala.");
                    return;
                }

                multicastChannel.receiveMessages();

                Scanner scanner = new Scanner(System.in);
                System.out.println("Digite suas mensagens. Para sair, digite 'sair'.");

                while (multicastChannel.running) {
                    String message = scanner.nextLine();
                    if (message.equalsIgnoreCase("sair")) {
                        multicastChannel.running = false;
                        multicastChannel.leaveRoom();
                        break;
                    } else {
                        multicastChannel.sendMessage(message);
                    }
                }

                scanner.close();
            } catch (IOException e) {
                System.err.println("IOException - Erro ao iniciar o chat: " + e.getMessage());
            } catch (Exception ex) {
                System.err.println("Erro: " + ex.getMessage());
            }
        }
}