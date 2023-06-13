import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

public class RestClient {

    private static final String BASE_URI = "https://stellarburgers.nomoreparties.site";

    protected RequestSpecification getBaseSpec() {
        return new RequestSpecBuilder().
                setBaseUri(BASE_URI).
                setBasePath("/api").
                setContentType(ContentType.JSON).
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
