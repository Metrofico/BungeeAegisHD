package net.md_5.bungee.connection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class LoginResult {
    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private Property[] properties;

    @Data
    @AllArgsConstructor
    public static class Property {

        private String name;
        private String value;
        private String signature;
    }
}
