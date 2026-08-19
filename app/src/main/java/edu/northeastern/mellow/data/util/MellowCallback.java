package edu.northeastern.mellow.data.util;

/**
 * Callback for one-shot async operations that don't need LiveData observation.
 */
@FunctionalInterface
public interface MellowCallback<T> {
    void onResult(MellowResult<T> result);
}
