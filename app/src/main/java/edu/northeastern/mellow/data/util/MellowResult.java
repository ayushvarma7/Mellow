package edu.northeastern.mellow.data.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic wrapper for async operation results.
 * All repository callbacks return MellowResult<T>.
 */
public class MellowResult<T> {

    public enum Status { SUCCESS, ERROR, LOADING }

    private final Status status;
    @Nullable private final T data;
    @Nullable private final Throwable error;
    @Nullable private final String message;

    private MellowResult(Status status, @Nullable T data,
                         @Nullable Throwable error, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.message = message;
    }

    public static <T> MellowResult<T> success(@Nullable T data) {
        return new MellowResult<>(Status.SUCCESS, data, null, null);
    }

    public static <T> MellowResult<T> error(@NonNull Throwable error, @NonNull String message) {
        return new MellowResult<>(Status.ERROR, null, error, message);
    }

    public static <T> MellowResult<T> loading() {
        return new MellowResult<>(Status.LOADING, null, null, null);
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError()   { return status == Status.ERROR; }
    public boolean isLoading() { return status == Status.LOADING; }

    @NonNull  public Status    getStatus()  { return status; }
    @Nullable public T         getData()    { return data; }
    @Nullable public Throwable getError()   { return error; }
    @Nullable public String    getMessage() { return message; }
}
