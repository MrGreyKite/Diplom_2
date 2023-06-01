import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static data.StatusCodes.OK;
import static data.StatusCodes.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class UserAuthTest {

    static Faker faker = new Faker();
    static String email = faker.internet().emailAddress();
    static String username = faker.name().username();
    static String password = faker.internet().password();

    static AuthClient authClient = new AuthClient();
    private static UserData user;

    @BeforeAll
    @Step("Создание тестового пользователя для авторизации")
    public static void createTestUser() {
        user = new UserData(email, password, username);
        authClient.registerUser(user).spec(authClient.getResponseSpec());
    }


    @Test
    public void loginWithValidUserTest() {
        ValidatableResponse response = authClient.
                authorizeUser(UserData.builder().email(user.getEmail()).password(user.getPassword()).build()).
                spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и подтверждается авторизация нужного пользователя",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(true)),
                    () -> assertEquals(user.getName(), response.extract().path("user.name")),
                    () -> assertEquals(user.getEmail(), response.extract().path("user.email"))
            );
        });

    }


    static Stream<Arguments> incorrectLoginData() {
        return Stream.of(
                arguments(UserData.builder().email(user.getEmail()).password("").build()),
                arguments(UserData.builder().email(" ").password(user.getPassword()).build()),
                arguments(UserData.builder().email(user.getEmail()).password(new Faker().internet().password()).build()),
                arguments(UserData.builder().email(new Faker().internet().emailAddress()).password(user.getPassword()).build()),
                arguments(UserData.builder().email("").password("").build())
        );
    }

    @ParameterizedTest(name = "{index} - Авторизация пользователем {0} с некорректными данными")
    @MethodSource("incorrectLoginData")
    public void authorizeWithIncorrectDataTest(UserData userInfo) {
        ValidatableResponse response = authClient.
                authorizeUser(userInfo).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и ожидаемое сообщение об ошибке",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertEquals("email or password are incorrect",
                            response.extract().path("message"))
            );
        });
    }



    @AfterAll
    @Step("Очистка данных тестового пользователя")
    static void tearDown() {
        if (!RestClient.getAuthToken().isEmpty()) {
            authClient.deleteUser().statusCode(202);
            authClient.setAuthToken("");
        }
    }
}
