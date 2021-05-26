package objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor

public class TestObject {
    @Getter
    @Setter
    private final int x;
    @Getter
    @Setter
    private final String y;
}
