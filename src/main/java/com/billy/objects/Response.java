package com.billy.objects;

public record Response(boolean ok, String error, Long id) {
    public static Response success() {
        return new Response(true, null, null);
    }

    public static Response success(Long id) {
        return new Response(true, null, id);
    }

    public static Response error(String errorMessage) {
        return new Response(false, errorMessage, null);
    }

    public static Response error(String errorMessage, Long id) {
        return new Response(false, errorMessage, id);
    }
}

