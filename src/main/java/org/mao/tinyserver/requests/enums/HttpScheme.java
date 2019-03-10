package org.mao.tinyserver.requests.enums;



public enum HttpScheme {

    HTTP("http"),
    HTTPS("https");

    private final String content;

    HttpScheme(String s) {
        this.content = s;
    }

    public static HttpScheme parseScheme(String s) {
        String[] split = s.split("/");
        for (HttpScheme httpScheme : HttpScheme.values()) {
            if (httpScheme.toString().equals(split[0].toLowerCase())) {
                return httpScheme;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return content;
    }


}
