import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static data.StatusCodes.FORBIDDEN;
import static data.StatusCodes.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тесты на регистрацию пользователя")
public class UserRegistrationTest extends BaseTest {
    Faker faker = new Faker();
    String email = faker.internet().emailAddress();
    String username = faker.name().username();
    String password = faker.internet().password();

    @Test
    @DisplayName("Регистрация нового пользователя")
    @Description("Проверяется регистрация пользователем, которого раньше не существовало")
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
    @DisplayName("Регистрация нового пользователя с повторяющимся именем")
    @Description("Проверяется, что нельзя зарегистрировать пользователя с таким же именем, как у уже существующего")
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
    @DisplayName("Регистрация нового пользователя с повторяющимся email")
    @Description("Проверяется, что нельзя зарегистрировать пользователя с таким же email, как у уже существующего")
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

    static Stream<UserData> incorrectRegisterData() {
        return Stream.of(
                new UserData("", "",""),
                new UserData(new Faker().internet().emailAddress(), "",""),
                new UserData("", "testing", new Faker().name().username()),
                new UserData(new Faker().internet().emailAddress(), "test-test",""),
                new UserData("", "",new Faker().internet().password())
        );
    }

    @ParameterizedTest(name = "{index} - Создание пользователя {0} без одного или нескольких обязательных полей приводит к ошибке")
    @MethodSource("incorrectRegisterData")
    @DisplayName("Попытка создания пользователя с некорректными данными - ")
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

}
