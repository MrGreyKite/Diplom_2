import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public class RestClient {

    private static final String BASE_URI = "https://stellarburgers.nomoreparties.site";

    private static String authToken = "";

    public void setAuthToken(String authToken) {
        RestClient.authToken = authToken;
    }

    public static String getAuthToken() {
        return authToken;
    }

    protected RequestSpecification getBaseSpec() {
        return new RequestSpecBuilder().
                setBaseUri(BASE_URI).
                setBasePath("/api").
                setContentType(ContentType.JSON).
                addHeader("authorization", getAuthToken()).
                log(LogDetail.ALL).
                build();
    }

    protected ResponseSpecification getResponseSpec() {
        return new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .log(LogDetail.BODY)
                .build();
    }


}
