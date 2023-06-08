import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static data.StatusCodes.ACCEPTED;

public class BaseTest {
    AuthClient authClient = new AuthClient();
    static UserData user;

    @BeforeEach
    @Step("Создание тестового пользователя")
    public void createTestUser() {
        if (!this.getClass().equals(UserRegistrationTest.class)) {
            Faker faker = new Faker();
            String email = faker.internet().emailAddress();
            String username = faker.name().username();
            String password = faker.internet().password();

            user = new UserData(email, password, username);
            authClient.registerUser(user).spec(authClient.getResponseSpec());
        }
    }

    @AfterEach
    @Step("Очистка данных тестового пользователя")
    void tearDown() {
        if (!RestClient.getAuthToken().isEmpty()) {
            authClient.deleteUser().statusCode(ACCEPTED.getCode());
            authClient.setAuthToken("");
        }
    }
}
