import data.OrderData;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static data.StatusCodes.*;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.is;

@DisplayName("Тесты на создание заказа")
public class OrderCreationTest extends BaseTest {
    OrdersClient ordersClient = new OrdersClient();
    IngredientsClient ingredientsClient = new IngredientsClient();
    List<String> ids;
    String hash1;
    String hash2;

    @BeforeEach
    @Step("Выбор хэш-кодов ингридиентов из списка доступных")
    void generateHashes() {
        ids = ingredientsClient.getAllIngredients().extract().jsonPath().getList("data._id");
        hash1 = ids.get(new Random().nextInt(ids.size()));
        do { hash2 = ids.get(new Random().nextInt(ids.size())); } while (hash2.equals(hash1));
    }

    @Test
    @DisplayName("Создание заказа авторизованным пользователем")
    @Description("Проверяется успешность создания заказа, если пользователь авторизован и указаны правильные ингридиенты")
    public void createOrderWithIngredientsWhenAuthorizedTest() {
        OrderData in = OrderData.builder().ingredients(new Object[]{hash1, hash2}).build();
        int numberOfIngredients = in.getIngredients().length;

        ValidatableResponse response = ordersClient.makeOrder(in).spec(ordersClient.getResponseSpec());
        OrderData order = response.extract().body().jsonPath().getObject("order", OrderData.class);

        Allure.step("Проверка корректности создания заказа с ингридиентами", () -> {
            assertAll("Приходит правильный статус-код и корректная информация о созданном заказе",
                    () -> assertEquals(OK.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(true)),
                    () -> assertEquals(numberOfIngredients,  order.getIngredients().length),
                    () -> assertThat(order.getNumber(), is(notNullValue())),
                    () -> assertEquals(user.getName(), order.getOwner().getName()),
                    () -> assertEquals(user.getEmail(), order.getOwner().getEmail())
            );
        });
    }

    @Test
    @DisplayName("Создание нового заказа неавторизованным пользователем")
    @Description("Проверяется, что только авторизованные пользователи могут делать заказы")
    public void createOrderWithIngredientsWhenUnauthorizedTest() {
        //в ТЗ указано "Только авторизованные пользователи могут делать заказы." (раздел "Авторизация и регистрация")
        ordersClient.setAuthToken("");

        OrderData in = OrderData.builder().ingredients(new Object[]{hash1, hash2}).build();

        ValidatableResponse response = ordersClient.makeOrder(in).spec(ordersClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит код ошибки 403 и сообщение об отсутствии авторизации",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }

    @Test
    @DisplayName("Создание заказа без ингредиентов авторизованным пользователем")
    @Description("Проверяется невозможность создания заказа без ингредиентов")
    public void createOrderWithoutIngredientsWhenAuthorizedTest() {
        OrderData in = OrderData.builder().ingredients(new Object[]{}).build();
        ValidatableResponse response = ordersClient.makeOrder(in).spec(ordersClient.getResponseSpec());

        Allure.step("Проверка кода и сообщения об ошибке", () -> {
            assertAll("Приходит статус-код 400 и сообщение об отсутствии ингредиентов",
                    () -> assertEquals(BAD_REQUEST.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("Ingredient ids must be provided"))
            );
        });
    }

    @Test
    @DisplayName("Создание заказа без ингредиентов неавторизованным пользователем")
    @Description("Проверяется, что для неавторизованного пользователя в первую очередь выводится ошибка авторизации")
    public void createOrderWithoutIngredientsWhenUnauthorizedTest(){
        ordersClient.setAuthToken("");

        OrderData in = OrderData.builder().ingredients(new Object[]{}).build();
        ValidatableResponse response = ordersClient.makeOrder(in).spec(ordersClient.getResponseSpec());

        Allure.step("Проверка кода и сообщения об ошибке", () -> {
            assertAll("Приходит код ошибки 403 и сообщение об отсутствии авторизации",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }

    @Test
    @DisplayName("Создание заказа с некорректным хэшем ингредиентов")
    @Description("Проверяется, что при указании несуществующего ингредиента возвращается сообщение об ошибке")
    public void createOrderWithInvalidIngredientsTest() {
        String invalidHash = UUID.randomUUID().toString().replace("-", "");
        OrderData in = OrderData.builder().ingredients(new Object[]{invalidHash, hash2}).build();

        ValidatableResponse response = ordersClient.makeOrder(in);

        Allure.step("Проверка кода и сообщения об ошибке", () -> {
            assertEquals(INTERNAL_SERVER_ERROR.getCode(), response.extract().statusCode());
        });
    }

}
