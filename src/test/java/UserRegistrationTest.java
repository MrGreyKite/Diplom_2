import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


import java.util.stream.Stream;

import static data.StatusCodes.FORBIDDEN;
import static data.StatusCodes.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Тесты на регистрацию пользователя")
public class UserRegistrationTest {

    Faker faker = new Faker();
    String email = faker.internet().emailAddress();
    String username = faker.name().username();
    String password = faker.internet().password();

    static AuthClient authClient = new AuthClient();
    private UserData user;

    @Test
    public void uniqueUserRegistrationTest() {
        user = new UserData(email, password, username);
        ValidatableResponse response = authClient.registerUser(user).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и подтверждается регистрация пользователя",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(true)),
                    () -> assertEquals(user.getName(), response.extract().path("user.name")),
                    () -> assertEquals(user.getEmail(), response.extract().path("user.email"))
            );
        });

    }

    @Test
    public void userWithExistingUsernameRegistrationTest() {
        user = new UserData(email, password, username);
        UserData user2 = new UserData(faker.internet().emailAddress(), password, username);

        authClient.registerUser(user).spec(authClient.getResponseSpec());
        ValidatableResponse response = authClient.registerUser(user2).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и текст об ошибке регистрации",
                    () -> assertEquals(FORBIDDEN.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertEquals("User already exists",
                            response.extract().path("message"))
            );
        });

    }


    @Test
    public void userWithExistingEmailRegistrationTest() {
        user = new UserData(email, password, username);
        UserData user2 = new UserData(email, faker.name().username(), password);

        authClient.registerUser(user).spec(authClient.getResponseSpec());
        ValidatableResponse response = authClient.registerUser(user2).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и текст об ошибке регистрации",
                    () -> assertEquals(FORBIDDEN.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertEquals("User already exists",
                            response.extract().path("message"))
            );
        });
    }

    static Stream<Arguments> incorrectRegisterData() {
        return Stream.of(
                arguments(new UserData("", "","")),
                arguments(new UserData(new Faker().internet().emailAddress(), "","")),
                arguments(new UserData("", "testing", new Faker().name().username())),
                arguments(new UserData(new Faker().internet().emailAddress(), "test-test","")),
                arguments(new UserData("", "",new Faker().internet().password()))
        );
    }

    @ParameterizedTest(name = "{index} - Создание пользователя {0} без одного или нескольких обязательных полей приводит к ошибке")
    @MethodSource("incorrectRegisterData")
    @DisplayName("Попытка создания пользователя с некорректными данными - без одного или нескольких полей")
    public void userWithoutRequiredFieldsTest(UserData user) {
        ValidatableResponse response = authClient.registerUser(user).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и ожидаемое сообщение об ошибке",
                    () -> assertEquals(FORBIDDEN.getCode(), response.extract().statusCode()),
                    () -> assertEquals("Email, password and name are required fields",
                            response.extract().path("message"))
            );
        });

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
