package ykw.testServer;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerChat {
    private static final int PORT = 5001;
    private static final List<PrintWriter> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serveur = new ServerSocket(PORT);
        System.out.println("Serveur en attente sur le port " + PORT);

        while (true) {
            Socket socket = serveur.accept();
            Thread.startVirtualThread(() -> gererClient(socket));
        }
    }

    // traite une requete et renvoie le pseudo du client (rempli au login)
    private static String manageRequestClient(String request, PrintWriter wr, String pseudo)
    {
        if (!request.startsWith("/")) {
            wr.println("ERREUR requete invalide");
            return pseudo;
        }

        // limite 5 : la derniere case garde tout le reste, meme s'il contient des "/"
        String[] arrayRequest = request.substring(1).split("/", 5);

        if (arrayRequest[0].equals("login"))
        {
            // /login/user/xxxx/passwd/yyyy
            if (arrayRequest.length < 5) {
                wr.println("ERREUR login incomplet");
                return pseudo;
            }
            String user = arrayRequest[2];
            wr.println("OK bienvenue " + user);
            diffuser(user + " a rejoint le jeu !");
            return user;
        }
        else if (arrayRequest[0].equals("matchmaking"))
        {
            wr.println("Matchmaking pas encore disponible");
        }
        else if (arrayRequest[0].equals("msg"))
        {
            // /msg/user/xxxx/content/yyyy
            if (arrayRequest.length < 5) {
                wr.println("ERREUR message incomplet");
            } else if (pseudo == null) {
                wr.println("ERREUR connecte-toi d'abord");
            } else if (!arrayRequest[2].equals(pseudo)) {
                wr.println("ERREUR tu ne peux pas parler au nom de " + arrayRequest[2]);
            } else {
                String user = arrayRequest[2];
                String contentMsg = arrayRequest[4];
                diffuser(user + " : " + contentMsg);
            }
        }
        else
        {
            wr.println("ERREUR commande inconnue");
        }
        return pseudo;
    }

    private static void gererClient(Socket socket) {
        try (socket) {
            BufferedReader lecteur = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter ecrivain = new PrintWriter(socket.getOutputStream(), true);

            clients.add(ecrivain);
            String pseudo = null;

            try {
                String request;
                while ((request = lecteur.readLine()) != null) {
                    pseudo = manageRequestClient(request, ecrivain, pseudo);
                }
            } finally {
                clients.remove(ecrivain);
                if (pseudo != null) {
                    diffuser(pseudo + " est parti");
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur client : " + e.getMessage());
        }
    }

    private static void diffuser(String message) {
        System.out.println(message);
        for (PrintWriter client : clients) {
            client.println(message);
        }
    }
}