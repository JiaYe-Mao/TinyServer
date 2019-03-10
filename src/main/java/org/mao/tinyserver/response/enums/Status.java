package org.mao.tinyserver.response.enums;


public enum Status {
    SUCCESS_200(200, "OK"),
    NOT_MODIFIED_304(304, "NOT MODIFIED"),
    NOT_FOUND_404(404, "NOT FOUND"),
    BAD_REQUEST_400(400, "BAD REQUEST"),
    METHOD_NOT_ALLOWED_405(405, "METHOD NOT ALLOWED"),
    INTERNAL_SERVER_ERROR_500(500, "INTERNAL SERVER ERROR");
    private int requestStatus;
    private String message;

    Status(int requestStatus, String msg) {
        this.requestStatus = requestStatus;
        message = msg;
    }

    public int getRequestStatus() {
        return requestStatus;
    }

    public String getMessage() {
        return message;
    }
}