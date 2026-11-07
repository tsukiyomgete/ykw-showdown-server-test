package ykw.testServer;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Game {
    public record Actions(String joueur, String type, String valeur) {};

    String p1;
    String coupP1;
    int scoreP1 = 0;

    String p2;
    String coupP2;
    int scoreP2 = 0;

    BlockingQueue<Actions> actions;
    private volatile boolean isGameFinished;

    Game(String p1, String p2)
    {
        this.p1 = p1;
        this.p2 = p2;
        actions = new LinkedBlockingDeque<>();
    }


    public void receive(String player, String content, String value)
    {
        actions.add(new Actions(player, content, value));
    }

    public void startGame()
    {
        Thread.startVirtualThread(() -> loop());
    }

    public void loop()
    {
        try {
            while (!isGameFinished)
            {
                Actions a = actions.take();
                if(a.type().equals("abandon"))
                {
                    if(a.joueur().equals(p1))
                    {
                        scoreP2 = 2;
                        isGameFinished = true;
                    }
                    if(a.joueur().equals(p2))
                    {
                        scoreP1 = 2;
                        isGameFinished = true;
                    }
                }
                if (a.joueur().equals(p1))
                {
                    coupP1 = a.valeur();
                } else
                {
                    coupP2 = a.valeur();
                }

                if (coupP1 != null && coupP2 != null)
                {
                    int gagnant = gagnantManche(coupP1, coupP2);
                    if (gagnant == 1) {
                        scoreP1++;
                    } else if (gagnant == 2) {
                        scoreP2++;
                    }

                    String resultat = "RESULTAT/" + p1 + "/" + coupP1 + "/" + p2 + "/" + coupP2
                            + "/score/" + scoreP1 + "-" + scoreP2;
                    ServerChat.sendTo(p1, resultat);
                    ServerChat.sendTo(p2, resultat);

                    
                    coupP1 = null;
                    coupP2 = null;


                    if (scoreP1 == 2 || scoreP2 == 2) {
                        isGameFinished = true;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            isGameFinished = true;
        }

        String vainqueur = (scoreP1 > scoreP2) ? p1 : p2;
        ServerChat.sendTo(p1, "FIN/gagnant/" + vainqueur);
        ServerChat.sendTo(p2, "FIN/gagnant/" + vainqueur);
        ServerChat.gameEnd(p1, p2);
    }


    private int gagnantManche(String c1, String c2)
    {
        if (c1.equals(c2)) {
            return 0;
        }
        if ((c1.equals("pierre") && c2.equals("ciseaux"))
                || (c1.equals("feuille") && c2.equals("pierre"))
                || (c1.equals("ciseaux") && c2.equals("feuille"))) {
            return 1;
        }
        return 2;
    }
}
