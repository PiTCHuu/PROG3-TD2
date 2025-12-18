package entity;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Main app = new Main();
        app.runTests();
    }

    public void runTests() {
        DataRetriever dr = new DataRetriever();

        System.out.println("--- Démarrage des tests ---");

        Team t1 = dr.findTeamById(1);
        if(t1 != null) System.out.println("Test a): " + t1.getName() + " - Joueurs: " + t1.getPlayersCount());

        List<Player> pPage1 = dr.findPlayers(1, 2);
        System.out.println("Test c): " + pPage1.size() + " joueurs trouvés");

        try {
            List<Player> conflictList = new ArrayList<>();
            conflictList.add(new Player(6, "Jude Bellingham", 23, PlayerPositionEnum.STR, null));
            dr.createPlayers(conflictList);
        } catch (RuntimeException e) {
            System.out.println("Test g) Réussi: Exception capturée comme prévu.");
        }

        Team real = dr.findTeamById(1);
        Player vini = new Player(6, "Vini", 25, PlayerPositionEnum.STR, real);
        real.getPlayers().add(vini);
        dr.saveTeam(real);
        System.out.println("Test i) effectué.");

        System.out.println("--- Tests terminés ---");
    }
}