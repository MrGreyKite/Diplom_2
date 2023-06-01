import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserModificationTest {

    Faker faker = new Faker();
    String email = faker.internet().emailAddress();
    String username = faker.name().username();
    String password = faker.internet().password();

    static AuthClient authClient = new AuthClient();
    private UserData user;
    String reserveToken;
    String refreshToken;


    @BeforeEach
    @Step("Создание тестового пользователя")
    public void createTestUser() {
        user = new UserData(email, password, username);
        authClient.registerUser(user).spec(authClient.getResponseSpec());
    }

    @Test
    public void changePasswordTest() {

    }

    @Test
    public void changeUsernameTest() {

    }

    @Test
    public void changeEmailTest() {

    }



    @AfterEach
    void restoreTokenIfNeeded() {
        if(RestClient.getAuthToken().isEmpty()) {
            authClient.setAuthToken(reserveToken);
        }
    }

    @AfterEach
    @Step("Очистка данных тестового пользователя")
    void tearDown() {
        if (!RestClient.getAuthToken().isEmpty()) {
            authClient.deleteUser().statusCode(202);
            authClient.setAuthToken("");
        }
    }

}
