import com.github.javafaker.Faker;
import data.UserData;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static data.StatusCodes.OK;
import static data.StatusCodes.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тесты на авторизацию пользователя")
public class UserAuthTest extends BaseTest {

    @Test
    @DisplayName("Авторизация с корректными и полными данными")
    public void loginWithValidUserTest() {
        ValidatableResponse response = authClient.
                authorizeUser(UserData.builder().email(user.getEmail()).password(user.getPassword()).build()).
                spec(authClient.getResponseSpec());

        Allure.step("Проверка данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и подтверждается авторизация нужного пользователя",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(true)),
                    () -> assertEquals(user.getName(), response.extract().path("user.name")),
                    () -> assertEquals(user.getEmail(), response.extract().path("user.email"))
            );
        });
    }

    static Stream<UserData> incorrectLoginData() {
        return Stream.of(
                UserData.builder().email(user.getEmail()).password("").build(),
                UserData.builder().email(" ").password(user.getPassword()).build(),
                UserData.builder().email(user.getEmail()).password(new Faker().internet().password()).build(),
                UserData.builder().email(new Faker().internet().emailAddress()).password(user.getPassword()).build(),
                UserData.builder().email("").password("").build()
        );
    }

    @ParameterizedTest(name = "{index} - пользователь {0}")
    @MethodSource("incorrectLoginData")
    @DisplayName("Авторизация с некорректными данными")
    @Description("Проверяется, что при авторизации с несуществующими или неполными данными возвращается сообщение об ошибке")
    public void authorizeWithIncorrectDataTest(UserData userInfo) {
        ValidatableResponse response = authClient.
                authorizeUser(userInfo).spec(authClient.getResponseSpec());

        Allure.step("Проверка данных в ответе", () -> {
            assertAll("Приходит правильный статус-код и ожидаемое сообщение об ошибке",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertEquals("email or password are incorrect",
                            response.extract().path("message"))
            );
        });
    }

}
