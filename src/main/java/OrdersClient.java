import data.OrderData;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;

import static io.restassured.RestAssured.given;

public class OrdersClient extends RestClient {

    private static final String ORDERS_PATH = "/orders";

    @Step("Отправка запроса на создание нового заказа без авторизации")
    public ValidatableResponse makeOrder(OrderData ingredients) {
        return given().
                spec(getBaseSpec()).
                body(ingredients).
                when().
                post(ORDERS_PATH).
                then();
    }

    @Step("Отправка запроса на создание заказа с авторизацией")
    public ValidatableResponse makeOrder(OrderData ingredients, String authToken) {
        return given().
                spec(getBaseSpec()).
                header("authorization", authToken).
                body(ingredients).
                when().
                post(ORDERS_PATH).
                then();
    }

    @Step("Отправка запроса на получение всех заказов пользователя без авторизации")
    public ValidatableResponse getOrdersList() {
        return given().
                spec(getBaseSpec()).
                when().
                get(ORDERS_PATH).
                then();
    }

    @Step("Отправка запроса на получение всех заказов пользователя с авторизацией")
    public ValidatableResponse getOrdersList(String authToken) {
        return given().
                spec(getBaseSpec()).
                header("authorization", authToken).
                when().
                get(ORDERS_PATH).
                then();
    }


}
