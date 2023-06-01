import com.github.javafaker.Faker;
import data.OrderData;
import data.UserData;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static data.StatusCodes.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrdersListTest {

    static AuthClient authClient = new AuthClient();
    static OrdersClient ordersClient = new OrdersClient();
    static IngredientsClient ingredientsClient = new IngredientsClient();
    static UserData user;
    static List<String> ids;
    static String hash1;
    static String hash2;
    static String hash3;
    String reserveToken;

    @BeforeAll
    @Step("Создание тестового пользователя и выбор ингредиентов")
    public static void createTestUser() {
        user = new UserData(new Faker().internet().emailAddress(),
                new Faker().internet().password(),
                new Faker().name().username());
        authClient.registerUser(user).spec(authClient.getResponseSpec());

        ids = ingredientsClient.getAllIngredients().extract().jsonPath().getList("data._id");
        hash1 = ids.get(new Random().nextInt(ids.size()));
        do { hash2 = ids.get(new Random().nextInt(ids.size())); } while (hash2.equals(hash1));
        ids.remove(hash1);
        ids.remove(hash2);
        hash3 = ids.get(new Random().nextInt(ids.size()));

    }

    @Test
    public void getOrdersByUserTest() {
        OrderData in1 = OrderData.builder().ingredients(new Object[]{hash1, hash2, hash3}).build();
        OrderData in2 = OrderData.builder().ingredients(new Object[]{hash1, hash2}).build();
        ordersClient.makeOrder(in1).spec(ordersClient.getResponseSpec());
        ordersClient.makeOrder(in2).spec(ordersClient.getResponseSpec());
        //извлечь номера заказов, сформировать лист
        //сравнить извлеченный лист с номерами заказов из ордерс
        //сравнить, что полное число заказов - равно размеру листа

        ValidatableResponse response = ordersClient.getOrdersList().spec(ordersClient.getResponseSpec());

        int totalOrders = response.extract().body().path("total");
        List<OrderData> orders = response.extract().body().jsonPath().getList("orders", OrderData.class);

    }

    @Test
    public void getOrdersUnauthorized() {
        reserveToken = RestClient.getAuthToken();
        ordersClient.setAuthToken("");

        OrderData in1 = OrderData.builder().ingredients(new Object[]{hash1}).build();
        OrderData in2 = OrderData.builder().ingredients(new Object[]{hash2}).build();
        ordersClient.makeOrder(in1).spec(ordersClient.getResponseSpec());
        ordersClient.makeOrder(in2).spec(ordersClient.getResponseSpec());

        ValidatableResponse response = ordersClient.getOrdersList().spec(ordersClient.getResponseSpec());

        Allure.step("Проверка кода и сообщения об ошибке", () -> {
            assertAll("Приходит статус-код 400 и сообщение об отсутствии ингредиентов",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }

    @AfterEach
    void restoreTokenIfNeeded() {
        if(RestClient.getAuthToken().isEmpty()) {
            authClient.setAuthToken(reserveToken);
        }
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
