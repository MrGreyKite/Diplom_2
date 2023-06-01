import com.github.javafaker.Faker;
import data.OrderData;
import data.UserData;
import io.qameta.allure.Allure;
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

public class OrderCreationTest {
    static AuthClient authClient = new AuthClient();
    static OrdersClient ordersClient = new OrdersClient();
    static IngredientsClient ingredientsClient = new IngredientsClient();
    static UserData user;
    static List<String> ids;
    String hash1;
    String hash2;
    String reserveToken;

    @BeforeAll
    @Step("Создание тестового пользователя")
    public static void createTestUser() {
        user = new UserData(new Faker().internet().emailAddress(),
                new Faker().internet().password(),
                new Faker().name().username());
        authClient.registerUser(user).spec(authClient.getResponseSpec());

        ids = ingredientsClient.getAllIngredients().extract().jsonPath().getList("data._id");
    }

    @BeforeEach
    @Step("Выбор хэш-кодов ингридиентов из списка доступных")
    void generateHashes() {
        hash1 = ids.get(new Random().nextInt(ids.size()));
        do { hash2 = ids.get(new Random().nextInt(ids.size())); } while (hash2.equals(hash1));
    }

    @Test
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
    public void createOrderWithIngredientsWhenUnauthorizedTest() {
        //в ТЗ указано "Только авторизованные пользователи могут делать заказы." (раздел "Авторизация и регистрация")
        reserveToken = RestClient.getAuthToken();
        ordersClient.setAuthToken("");

        OrderData in = OrderData.builder().ingredients(new Object[]{hash1, hash2}).build();

        ValidatableResponse response = ordersClient.makeOrder(in).spec(ordersClient.getResponseSpec());

        Allure.step("Проверка корректности данных в ответе", () -> {
            assertAll("Приходит код ошибки и сообщение об отсутствии авторизации",
                    () -> assertEquals(UNAUTHORIZED.getCode(), response.extract().statusCode()),
                    () -> assertThat(response.extract().path("success"), is(false)),
                    () -> assertThat(response.extract().path("message"), is("You should be authorised"))
            );
        });
    }

    @Test
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
    public void createOrderWithoutIngredientsWhenUnauthorizedTest(){
        reserveToken = RestClient.getAuthToken();
        ordersClient.setAuthToken("");

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
    public void createOrderWithInvalidIngredientsTest() {
        String invalidHash = UUID.randomUUID().toString().replace("-", "");
        OrderData in = OrderData.builder().ingredients(new Object[]{invalidHash, hash2}).build();

        ValidatableResponse response = ordersClient.makeOrder(in);

        Allure.step("Проверка кода и сообщения об ошибке", () -> {
            assertEquals(INTERNAL_SERVER_ERROR.getCode(), response.extract().statusCode());
        });
    }



    @AfterEach
    @Step("Восстановление токена авторизации, если он был стерт")
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
