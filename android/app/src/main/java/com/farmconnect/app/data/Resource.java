package com.farmconnect.app.data;

public class Resource<T> {
    public enum Status { LOADING, SUCCESS, ERROR }

    public final Status status;
    public final T data;
    public final String message;

    private Resource(Status s, T d, String m) { status = s; data = d; message = m; }

    public static <T> Resource<T> loading() { return new Resource<>(Status.LOADING, null, null); }
    public static <T> Resource<T> success(T d) { return new Resource<>(Status.SUCCESS, d, null); }
    public static <T> Resource<T> error(String m) { return new Resource<>(Status.ERROR, null, m); }
}
