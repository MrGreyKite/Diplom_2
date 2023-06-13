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

import static data.StatusCodes.OK;
import static data.StatusCodes.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тесты на изменение данных пользователя")
public class UserModificationTest extends BaseTest {
    Faker faker = new Faker();
    String refreshToken;

    @Test
    @DisplayName("Изменение пароля пользователя")
    @Description("Проверка, что можно успешно изменить пароль и потом без ошибки авторизоваться с новым паролем")
    public void changePasswordTest() {
        ValidatableResponse auth = authClient.authorizeUser(user);
        refreshToken = auth.extract().path("refreshToken");

        String newPassword = faker.internet().password();

        ValidatableResponse responseMod = authClient.modifyUserData(UserData.builder().password(newPassword).build(),
                authToken);

        Allure.step("Проверка успешности авторизации", () -> {
            assertAll("Приходит правильный статус-код и результат",
                    () -> assertEquals(OK.getCode(), responseMod.extract().statusCode()),
                    () -> assertThat(responseMod.extract().path("success"), is(true))
            );
        });

        authClient.logoutUser(refreshToken);

        ValidatableResponse response = authClient.
                authorizeUser(UserData.builder().email(user.getEmail()).password(newPassword).build());
        authToken = response.extract().body().path("accessToken");
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
    @DisplayName("Изменение имени пользователя")
    @Description("Проверка, что можно успешно изменить имя пользователя и изменения сохраняются")
    public void changeUsernameTest() {
        String newName = "Super New Name";
        authClient.modifyUserData(UserData.builder().name(newName).build(), authToken).
                spec(authClient.getResponseSpec());

        ValidatableResponse response = authClient.getUserData(authToken).spec(authClient.getResponseSpec());
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
    @DisplayName("Изменение email пользователя")
    @Description("Проверка, что можно успешно изменить email пользователя и изменения сохраняются")
    public void changeEmailTest() {
        String newEmail = new Faker().internet().emailAddress();
        authClient.modifyUserData(UserData.builder().email(newEmail).build(), authToken).
                spec(authClient.getResponseSpec());

        ValidatableResponse response = authClient.getUserData(authToken).spec(authClient.getResponseSpec());
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

    @ParameterizedTest(name = "{index} - попытка изменить данные на {0}")
    @MethodSource("modifyUserData")
    @DisplayName("Отправка запроса на изменение данных неавторизованного пользователя")
    @Description("Проверяется, что недоступно изменение данных без токена авторизации")
    public void changeProfileFieldUnauthorized(UserData userInfo) {
        ValidatableResponse response = authClient.modifyUserData(userInfo).spec(authClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит код ошибки и сообщение об отсутствии авторизации",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }

    static Stream<UserData> modifyUserData() {
        return Stream.of(
                UserData.builder().email("new@new.com").build(),
                UserData.builder().name("ТестовыйЮзер").build(),
                UserData.builder().password("qsefthuko08642").build()
        );
    }


}
