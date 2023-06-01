package data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderData {
    private Object[] ingredients;
    private String _id;
    private Owner owner;
    private int number;
    private String status;

    @Data
    @Builder
    @AllArgsConstructor
    public static class Owner {
        private String name;
        private String email;
    }
}
