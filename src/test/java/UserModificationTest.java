import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
        refreshToken = authClient.authorizeUser(user).extract().path("refreshToken");
        String newPassword = faker.internet().password();

        ValidatableResponse responseMod = authClient.modifyUserData(UserData.builder().password(newPassword).build());

        Allure.step("Проверка успешности запроса", () -> {
            assertAll("Приходит правильный статус-код и результат",
                    () -> assertEquals(OK.getCode(), responseMod.extract().statusCode()),
                    () -> assertThat(responseMod.extract().path("success"), is(true))
            );
        });

        authClient.logoutUser(refreshToken);

        ValidatableResponse response = authClient.
                authorizeUser(UserData.builder().email(user.getEmail()).password(newPassword).build());

        UserData userData = response.extract().body().jsonPath().getObject("user", UserData.class);

        Allure.step("Проверка успешной авторизации с новым паролем", () -> {
            assertAll("Приходит правильный статус-код и пользователь авторизован",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(responseMod.extract().path("success"), is(true)),
                    () -> assertEquals(user.getName(), userData.getName()),
                    () -> assertEquals(user.getEmail(), userData.getEmail())
            );
        });

    }

    @Test
    public void changeUsernameTest() {
        String newName = "Super New Name";
        authClient.modifyUserData(UserData.builder().name(newName).build()).
                spec(authClient.getResponseSpec());

        ValidatableResponse response = authClient.getUserData().spec(authClient.getResponseSpec());

        UserData userData = response.extract().body().jsonPath().getObject("user", UserData.class);

        Allure.step("Проверка корректности измененных данных", () -> {
            assertAll("Приходит правильный статус-код и поменялись нужные данные",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(true)),
                    () -> assertEquals(newName, userData.getName()),
                    () -> assertEquals(user.getEmail(), userData.getEmail())
            );
        });
    }

    @Test
    public void changeEmailTest() {
        String newEmail = new Faker().internet().emailAddress();
        authClient.modifyUserData(UserData.builder().name(newEmail).build()).
                spec(authClient.getResponseSpec());

        ValidatableResponse response = authClient.getUserData().spec(authClient.getResponseSpec());

        UserData userData = response.extract().body().jsonPath().getObject("user", UserData.class);

        Allure.step("Проверка корректности измененных данных", () -> {
            assertAll("Приходит правильный статус-код и поменялись нужные данные",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(true)),
                    () -> assertEquals(user.getName(), userData.getName()),
                    () -> assertEquals(newEmail, userData.getEmail())
            );
        });

    }

    @ParameterizedTest(name = "{index} - Попытка изменить данные неавторизованного пользователя {0}")
    @MethodSource("modifyUserData")
    public void changeProfileFieldUnauthorized(UserData userInfo) {
        reserveToken = RestClient.getAuthToken();
        authClient.setAuthToken("");
        ValidatableResponse response = authClient.modifyUserData(userInfo).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит код ошибки и сообщение об отсутствии авторизации",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }

    static Stream<Arguments> modifyUserData() {
        return Stream.of(
                arguments(UserData.builder().email("new@new.com").build()),
                arguments(UserData.builder().name("ТестовыйЮзер").build()),
                arguments(UserData.builder().password("qsefthuko08642").build())
        );
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
