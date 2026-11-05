
import java.io.*;
import java.net.*;

public static void main(String[] args){
    try (ServerSocket serveur = new ServerSocket(5001)) {
        System.out.println("En attente sur le port " + serveur.getLocalPort() + " IP : " + serveur.getInetAddress());
        while (true) {
            try (Socket client = serveur.accept()){
                 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out   = new PrintWriter(client.getOutputStream(), true);
                 System.out.println("Visite de " + client.getInetAddress());
                 System.out.println("Bienvenue au Yo-kai Showdown !");

                 String ligne = in.readLine();
                 if(ligne == null)
                 {
                     client.close();
                 }

                 if(ligne.equals("ping"))
                 {
                     out.println("pong");
                 } else {
                     System.out.println("Message reçu de " + client.getInetAddress());
                     System.out.println(ligne);
                     out.println("Salut");
                 }
            } catch (IOException e)
            {
                System.out.println("Erreur : " +e.getMessage());
            }
        }
    }catch(IOException e)
    {
        System.out.println("Erreur : " +e.getMessage());
    }
}