package fr.pickaria.pterodactylpoweraction.online;

import com.google.gson.annotations.SerializedName;

public class PterodactylWebSocketCredentialsResponse {
    @SerializedName("data")
    private Data data;

    public Data getData() {
        return data;
    }

    public static final class Data {
        @SerializedName("token")
        private String token;

        @SerializedName("socket")
        private String socket;

        public String getToken() {
            return token;
        }

        public String getSocket() {
            return socket;
        }
    }
}
