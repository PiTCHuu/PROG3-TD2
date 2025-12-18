package entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {

    private final DBConnection dbConnection;
    public DataRetriever(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    Team findTeamById(Integer id)  throws SQLException {
        String teamSql = "SELECT ALL FROM team WHERE id = ?";
        String playerSql = "SELECT ALL FROM player WHERE id_team = ?";

        try (Connection con= dbConnection.getDBConnection()){
            Team team = null;

            try (PreparedStatement ps = con.prepareStatement(teamSql)){
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    team = new Team(
                            rs.getInt("id"),
                            rs.getString("name"),
                            ContinentEnum.valueOf(rs.getString("continent"))
                    );
                }
            }
            if (team == null){
                return null;
            }
            List<Player> players = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(playerSql)){
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    players.add(new Player(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("age"),
                            PlayerPositionEnum.valueOf(rs.getString("position")),
                            team
                    ));
                }
            }
            team.setPlayers(players);
            return team;
        }
    }
}
