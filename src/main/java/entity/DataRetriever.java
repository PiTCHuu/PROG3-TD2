package entity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private final DBConnection dbConnection = new DBConnection();

    public Team findTeamById(Integer id) {
        Team team = null;
        String sqlTeam = "SELECT ALL FROM Team WHERE id = ?";
        String sqlPlayers = "SELECT ALL FROM Player WHERE id_team = ?";

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement psTeam = conn.prepareStatement(sqlTeam);
             PreparedStatement psPlayers = conn.prepareStatement(sqlPlayers)) {

            psTeam.setInt(1, id);
            try (ResultSet rsTeam = psTeam.executeQuery()) {
                if (rsTeam.next()) {
                    team = new Team(
                            rsTeam.getInt("id"),
                            rsTeam.getString("name"),
                            ContinentEnum.valueOf(rsTeam.getString("continent"))
                    );

                    psPlayers.setInt(1, id);
                    try (ResultSet rsPlayers = psPlayers.executeQuery()) {
                        while (rsPlayers.next()) {
                            Player p = new Player(
                                    rsPlayers.getInt("id"),
                                    rsPlayers.getString("name"),
                                    rsPlayers.getInt("age"),
                                    PlayerPositionEnum.valueOf(rsPlayers.getString("position")),
                                    team
                            );
                            team.getPlayers().add(p);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return team;
    }

    public List<Player> findPlayers(int page, int size) {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT ALL FROM Player LIMIT ? OFFSET ?";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(mapResultSetToPlayer(rs, null));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public List<Player> createPlayers(List<Player> newPlayers) {
        String checkSql = "SELECT id FROM Player WHERE id = ?";
        String insertSql = "INSERT INTO Player (id, name, age, position, id_team) VALUES (?, ?, ?, ?::player_position_enum, ?)";

        Connection conn = null;
        try {
            conn = dbConnection.getDBConnection();
            conn.setAutoCommit(false);

            for (Player p : newPlayers) {
                try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
                    psCheck.setInt(1, p.getId());
                    if (psCheck.executeQuery().next()) {
                        conn.rollback();
                        throw new RuntimeException("Le joueur avec l'ID " + p.getId() + " existe déjà.");
                    }
                }
                try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                    psIns.setInt(1, p.getId());
                    psIns.setString(2, p.getName());
                    psIns.setInt(3, p.getAge());
                    psIns.setString(4, p.getPosition().name());
                    if (p.getTeam() != null) psIns.setInt(5, p.getTeam().getId());
                    else psIns.setNull(5, java.sql.Types.INTEGER);
                    psIns.executeUpdate();
                }
            }
            conn.commit();
            return newPlayers;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Erreur lors de la création", e);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public Team saveTeam(Team teamToSave) {
        String sqlUpsert = "INSERT INTO Team (id, name, continent) VALUES (?, ?, ?::continent_enum) " +
                "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, continent = EXCLUDED.continent";

        try (Connection conn = dbConnection.getDBConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                ps.setInt(1, teamToSave.getId());
                ps.setString(2, teamToSave.getName());
                ps.setString(3, teamToSave.getContinent().name());
                ps.executeUpdate();
            }

            String sqlDetach = "UPDATE Player SET id_team = NULL WHERE id_team = ?";
            try (PreparedStatement psDetach = conn.prepareStatement(sqlDetach)) {
                psDetach.setInt(1, teamToSave.getId());
                psDetach.executeUpdate();
            }

            if (teamToSave.getPlayers() != null) {
                String sqlAttach = "UPDATE Player SET id_team = ? WHERE id = ?";
                for (Player p : teamToSave.getPlayers()) {
                    try (PreparedStatement psAttach = conn.prepareStatement(sqlAttach)) {
                        psAttach.setInt(1, teamToSave.getId());
                        psAttach.setInt(2, p.getId());
                        psAttach.executeUpdate();
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return findTeamById(teamToSave.getId());
    }

    public List<Team> findTeamsByPlayerName(String playerName) {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT DISTINCT t.ALL FROM Team t JOIN Player p ON t.id = p.id_team WHERE p.name ILIKE ?";
        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + playerName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    teams.add(new Team(rs.getInt("id"), rs.getString("name"), ContinentEnum.valueOf(rs.getString("continent"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }

    public List<Player> findPlayersByCriteria(String name, PlayerPositionEnum pos, String teamName, ContinentEnum cont, int page, int size) {
        List<Player> players = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT p.ALL FROM Player p LEFT JOIN Team t ON p.id_team = t.id WHERE 1=1");

        if (name != null) sql.append(" AND p.name ILIKE ?");
        if (pos != null) sql.append(" AND p.position = ?::player_position_enum");
        if (teamName != null) sql.append(" AND t.name ILIKE ?");
        if (cont != null) sql.append(" AND t.continent = ?::continent_enum");

        sql.append(" LIMIT ? OFFSET ?");

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (name != null) ps.setString(idx++, "%" + name + "%");
            if (pos != null) ps.setString(idx++, pos.name());
            if (teamName != null) ps.setString(idx++, "%" + teamName + "%");
            if (cont != null) ps.setString(idx++, cont.name());
            ps.setInt(idx++, size);
            ps.setInt(idx, (page - 1) * size);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    players.add(mapResultSetToPlayer(rs, null));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    private Player mapResultSetToPlayer(ResultSet rs, Team t) throws SQLException {
        return new Player(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("age"),
                PlayerPositionEnum.valueOf(rs.getString("position")),
                t
        );
    }
}