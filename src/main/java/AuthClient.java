import data.UserData;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;

import static io.restassured.RestAssured.given;

public class AuthClient extends RestClient {

    private static final String REGISTER_PATH = "/auth/register";
    private static final String LOGIN_PATH = "/auth/login";
    private static final String LOGOUT_PATH = "/auth/logout";
    private static final String USER_PATH = "/auth/user";


    @Step("Отправка запроса на регистрацию пользователя")
    public ValidatableResponse registerUser(UserData user) {
        ValidatableResponse response = given().
                spec(getBaseSpec()).
                body(user).
                when().
                post(REGISTER_PATH).
                then();

        if (response.extract().body().path("accessToken") != null) {
            setAuthToken(response.extract().body().path("accessToken"));
        }

        return response;
    }

    @Step("Отправка запроса на авторизацию пользователя")
    public ValidatableResponse authorizeUser(UserData user) {
        ValidatableResponse response = given().
                spec(getBaseSpec()).
                body(user).
                when().
                post(LOGIN_PATH).
                then();

        if (response.extract().body().path("accessToken") != null) {
            setAuthToken(response.extract().body().path("accessToken"));
        }

        return response;

    }

    @Step("Отправка запроса на удаление пользователя")
    public ValidatableResponse deleteUser() {
        return given().
                spec(getBaseSpec()).
                when().
                delete(USER_PATH).
                then();
    }

    @Step("Отправка запроса на получение данных пользователя")
    public ValidatableResponse getUserData() {
        return given().
                spec(getBaseSpec()).
                when().
                get(USER_PATH).
                then();
    }

    @Step("Отправка запроса на изменение данных пользователя")
    public ValidatableResponse modifyUserData(UserData userInfo) {
        return given().
                spec(getBaseSpec()).
                body(userInfo).
                when().
                patch(USER_PATH).
                then();
    }

    @Step("Отправка запроса на выход пользователя")
    public ValidatableResponse logoutUser(String refreshToken) {
        return given().
                spec(getBaseSpec()).
                body(refreshToken).
                when().
                post(LOGOUT_PATH).
                then();
    }


}
