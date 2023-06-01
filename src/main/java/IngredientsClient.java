import io.qameta.allure.Step;
import io.restassured.response.ValidatableResponse;
import static io.restassured.RestAssured.given;

public class IngredientsClient extends RestClient {

    private static final String INGREDIENTS_PATH = "/ingredients";

    @Step("Получение полного списка ингредиентов")
    public ValidatableResponse getAllIngredients() {
        return given().
                spec(getBaseSpec()).
                when().
                get(INGREDIENTS_PATH).
                then();
    }

    //потом пернести в код теста
    //List<String> ids = getAllIngredients().extract().jsonPath().getList("data._id");
}
