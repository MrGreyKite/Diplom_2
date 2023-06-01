import data.OrderData;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;

import static io.restassured.RestAssured.given;

public class OrdersClient extends RestClient {

    private static final String ORDERS_PATH = "/orders";

    @Step("Отправка запроса на создание нового заказа")
    public ValidatableResponse makeOrder(OrderData ingredients) {
        return given().
                spec(getBaseSpec()).
                body(ingredients).
                when().
                post(ORDERS_PATH).
                then();
    }

    @Step("Отправка запроса на получение всех заказов пользователя")
    public ValidatableResponse getOrdersList() {
        return given().
                spec(getBaseSpec()).
                when().
                get(ORDERS_PATH).
                then();
    }


}
