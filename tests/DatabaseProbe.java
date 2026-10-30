import com.dao.Dao;
import com.util.PasswordUtil;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/** Verifies storage in, or removes, only the runner's disposable test database. */
public class DatabaseProbe {
    public static void main(String[] args) throws Exception {
        try (Connection connection = Dao.getConnection();
                Statement statement = connection.createStatement()) {
            String database = connection.getCatalog();
            if (!database.matches("campus_events_qa_[0-9]+")) {
                throw new IllegalArgumentException("Refusing to use a non-test database.");
            }
            if (args.length == 1 && "cleanup".equals(args[0])) {
                statement.executeUpdate("DROP DATABASE `" + database + "`");
                System.out.println("Disposable test database removed.");
                return;
            }
            int checked = 0;
            try (ResultSet users = statement.executeQuery(
                    "SELECT roll_no, password_hash, role FROM users WHERE roll_no LIKE 'QATEST%'");) {
                while (users.next()) {
                    String hash = users.getString("password_hash");
                    String password = "QATEST004".equals(users.getString("roll_no"))
                            ? " Password@123 " : "Password@123";
                    if (!hash.startsWith("$2a$12$") || !PasswordUtil.verify(password, hash)
                            || !"PARTICIPANT".equals(users.getString("role"))) {
                        throw new AssertionError("Password storage or participant role is invalid.");
                    }
                    checked++;
                }
            }
            if (checked != 4) {
                throw new AssertionError("Expected four test participants; found " + checked);
            }
            System.out.println("PASS: All participant passwords use BCrypt cost 12; roles correct.");
        }
    }
}
