import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static data.StatusCodes.ACCEPTED;

public class BaseTest {
    AuthClient authClient = new AuthClient();
    static UserData user;
    String authToken;

    @BeforeEach
    @Step("Создание тестового пользователя")
    public void createTestUser() {
        if (!this.getClass().equals(UserRegistrationTest.class)) {
            Faker faker = new Faker();
            String email = faker.internet().emailAddress();
            String username = faker.name().username();
            String password = faker.internet().password();

            user = new UserData(email, password, username);

            ValidatableResponse authResp = authClient.registerUser(user).spec(authClient.getResponseSpec());
            if (authResp.extract().body().path("accessToken") != null) {
                authToken = authResp.extract().body().path("accessToken");
            }
        }
    }

    @AfterEach
    @Step("Очистка данных тестового пользователя")
    void tearDown() {
        if (authToken != null && !authToken.isEmpty() && !authToken.isBlank()) {
            authClient.deleteUser(authToken).statusCode(ACCEPTED.getCode());
            authToken = "";
        }
    }
}
