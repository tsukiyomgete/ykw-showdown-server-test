package ykw.testServer;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public class ServerChat {
    private static final int PORT = 5001;

    // pseudo -> ecrivain du client (seulement les clients connectes avec /login)
    private static final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    // file des joueurs qui cherchent un match (dans l'ordre d'arrivee)
    private static final BlockingQueue<String> fileAttente = new LinkedBlockingDeque<>();
    // joueurs en attente, de l'inscription jusqu'a la creation de la partie
    private static final Set<String> enAttente = ConcurrentHashMap.newKeySet();
    // pseudo -> sa partie en cours
    private static final Map<String, Game> gameOf = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serveur = new ServerSocket(PORT);
        System.out.println("Serveur en attente sur le port " + PORT);

        Thread.startVirtualThread(() -> gererMatchMaking());
        while (true) {
            Socket socket = serveur.accept();
            Thread.startVirtualThread(() -> gererClient(socket));
        }
    }

    // renvoie le pseudo du client après avoir fini la requete
    private static String manageRequestClient(String request, PrintWriter wr, String pseudo)
    {
        if (!request.startsWith("/")) {
            wr.println("ERREUR requete invalide");
            return pseudo;
        }

        String[] arrayRequest = request.substring(1).split("/", 5);

        if (arrayRequest[0].equals("login"))
        {
            // /login/user/xxxx/passwd/yyyy
            if (pseudo != null) {
                wr.println("ERREUR deja connecte");
                return pseudo;
            }
            if (arrayRequest.length < 5) {
                wr.println("ERREUR login incomplet");
                return pseudo;
            }
            String user = arrayRequest[2];
            if (clients.putIfAbsent(user, wr) != null) {
                wr.println("ERREUR pseudo deja connecte");
                return pseudo;
            }
            wr.println("OK bienvenue " + user);
            diffuser(user + " a rejoint le jeu !");
            return user;
        }
        else if (arrayRequest[0].equals("matchmaking"))
        {
            if (pseudo == null) {
                wr.println("Connecte-toi d'abord");
            } else if (gameOf.containsKey(pseudo)) {
                wr.println("Deja en partie");
            } else if (!enAttente.add(pseudo)) {
                // add renvoie false si le pseudo y etait deja
                wr.println("Deja en attente");
            } else {
                fileAttente.add(pseudo);
                wr.println("En attente de joueur...");
            }
        }
        else if (arrayRequest[0].equals("coup"))
        {
            // /coup/pierre
            if (arrayRequest.length < 2) {
                wr.println("Coup incomplet");
                return pseudo;
            }
            Game game1 = gameOf.get(pseudo);
            if (game1 == null) {
                wr.println("Not in game");
            } else {
                game1.receive(pseudo, "coup", arrayRequest[1]);
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

            String pseudo = null;

            try {
                String request;
                while ((request = lecteur.readLine()) != null) {
                    pseudo = manageRequestClient(request, ecrivain, pseudo);
                }
            } finally {
                if (pseudo != null) {
                    clients.remove(pseudo);
                    enAttente.remove(pseudo);
                    fileAttente.remove(pseudo);
                    Game game = gameOf.get(pseudo);
                    if (game != null) {
                        game.receive(pseudo, "abandon", "");
                    }
                    diffuser(pseudo + " est parti");
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur client : " + e.getMessage());
        }
    }

    private static void gererMatchMaking()
    {
        try {
            while (true)
            {
                String j1 = fileAttente.take();
                String j2 = fileAttente.take();


                if (j1.equals(j2)) {
                    fileAttente.add(j1);
                    continue;
                }

                boolean j1Present = clients.containsKey(j1);
                boolean j2Present = clients.containsKey(j2);


                if (!j1Present || !j2Present) {
                    if (j1Present) {
                        fileAttente.add(j1);
                    } else {
                        enAttente.remove(j1);
                    }
                    if (j2Present) {
                        fileAttente.add(j2);
                    } else {
                        enAttente.remove(j2);
                    }
                    continue;
                }

                Game gameP1P2 = new Game(j1, j2);
                gameOf.put(j1, gameP1P2);
                gameOf.put(j2, gameP1P2);
                enAttente.remove(j1);
                enAttente.remove(j2);
                sendTo(j1, "MATCH/adversaire/" + j2);
                sendTo(j2, "MATCH/adversaire/" + j1);
                gameP1P2.startGame();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public static void gameEnd(String j1, String j2)
    {
        gameOf.remove(j1);
        gameOf.remove(j2);
    }


    private static void diffuser(String message) {
        System.out.println(message);
        for (PrintWriter client : clients.values()) {
            client.println(message);
        }
    }


    public static void sendTo(String pseudo, String message) {
        PrintWriter w = clients.get(pseudo);
        if (w != null) {
            w.println(message);
        }
    }
}