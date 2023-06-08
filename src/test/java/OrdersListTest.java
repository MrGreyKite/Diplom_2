import data.OrderData;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static data.StatusCodes.OK;
import static data.StatusCodes.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@DisplayName("Тесты на список заказов пользователя")
public class OrdersListTest extends BaseTest {
    OrdersClient ordersClient = new OrdersClient();
    IngredientsClient ingredientsClient = new IngredientsClient();
    List<String> ids;
    String hash1;
    String hash2;
    String hash3;

    @BeforeEach
    @Step("Создание тестового пользователя и выбор ингредиентов")
    public void createTestUser() {
        super.createTestUser();

        ids = ingredientsClient.getAllIngredients().extract().jsonPath().getList("data._id");
        hash1 = ids.get(new Random().nextInt(ids.size()));
        do { hash2 = ids.get(new Random().nextInt(ids.size())); } while (hash2.equals(hash1));
        ids.remove(hash1);
        ids.remove(hash2);
        hash3 = ids.get(new Random().nextInt(ids.size()));
    }

    @Test
    @DisplayName("Получение списка заказов, сделанных пользователем")
    @Description("Проверяется, что авторизованный пользователь может получить актуальный список своих заказов " +
            "- правильный состав и количество")
    public void getOrdersByUserTest() {
        OrderData in1 = OrderData.builder().ingredients(new Object[]{hash1, hash2, hash3}).build();
        OrderData in2 = OrderData.builder().ingredients(new Object[]{hash1, hash2}).build();
        ValidatableResponse firstOrder = ordersClient.makeOrder(in1).spec(ordersClient.getResponseSpec());
        ValidatableResponse secondOrder = ordersClient.makeOrder(in2).spec(ordersClient.getResponseSpec());

        List<String> idsOfIndividualOrders = new ArrayList<>();
        idsOfIndividualOrders.add(firstOrder.extract().path("order._id"));
        idsOfIndividualOrders.add(secondOrder.extract().path("order._id"));

        List<Integer> numbersOfIndividualOrders = new ArrayList<>();
        numbersOfIndividualOrders.add(firstOrder.extract().path("order.number"));
        numbersOfIndividualOrders.add(secondOrder.extract().path("order.number"));

        ValidatableResponse response = ordersClient.getOrdersList().spec(ordersClient.getResponseSpec());

        int totalOrders = response.extract().body().path("total");
        List<OrderData> orders = response.extract().body().jsonPath().getList("orders", OrderData.class);

        List<String> idsOfOrdersInList = orders.stream().map(OrderData::get_id).collect(Collectors.toList());
        List<Integer> numbersOfOrdersInList = orders.stream().map(OrderData::getNumber).collect(Collectors.toList());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит информация о количестве и составе заказов",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertIterableEquals(idsOfIndividualOrders, idsOfOrdersInList),
                    () -> assertIterableEquals(numbersOfIndividualOrders, numbersOfOrdersInList),
                    () -> assertEquals(numbersOfOrdersInList.size(), totalOrders)
            );
        });
    }

    @Test
    @DisplayName("Запрос списка заказов неавторизованным пользователем")
    @Description("Проверяется, что только авторизованный пользователь может получить список своих заказов")
    public void getOrdersUnauthorized() {
        ordersClient.setAuthToken("");

        OrderData in1 = OrderData.builder().ingredients(new Object[]{hash1}).build();
        OrderData in2 = OrderData.builder().ingredients(new Object[]{hash2}).build();
        ordersClient.makeOrder(in1).spec(ordersClient.getResponseSpec());
        ordersClient.makeOrder(in2).spec(ordersClient.getResponseSpec());

        ValidatableResponse response = ordersClient.getOrdersList().spec(ordersClient.getResponseSpec());

        Allure.step("Проверка кода и сообщения об ошибке", () -> {
            assertAll("Приходит статус-код 403 и сообщение об отсутствии авторизации",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }


}
