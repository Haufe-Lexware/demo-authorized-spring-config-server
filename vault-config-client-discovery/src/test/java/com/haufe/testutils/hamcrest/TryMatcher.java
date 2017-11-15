package com.haufe.testutils.hamcrest;

import javaslang.control.Try;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static java.util.Objects.requireNonNull;

/**
 * A Hamcrest Matcher that verifies whether a {@link Try} control stores a success or a failure and then
 * checks the {@link Try#get() success value} or the {@link Try#getCause() failure cause}, respectively,
 * matches additional conditions.
 *
 * @param <T> {@link Try}'s value type in case of success
 */
public class TryMatcher<T> extends TypeSafeDiagnosingMatcher<Try<T>> {

    public enum SuccesOrFailure {
        SUCCESS("a Try.success(v) "),
        FAILURE("a Try.failure(v) ");

        private final String descriptor;

        SuccesOrFailure(String descriptor) {
            this.descriptor = descriptor;
        }

        public static SuccesOrFailure succesOrFailure(Try<?> item) {
            return item.isSuccess() ? SUCCESS : FAILURE;
        }

        public void describeTo(Description description) {
            description.appendText(descriptor);
        }

        public boolean matches(Try<?> item) {
            requireNonNull(item, "item must not be null");
            return item.isSuccess() == (this == SUCCESS);
        }
    }

    private final SuccesOrFailure succesOrFailure;
    private final Matcher<? super T> valueMatcher;

    private TryMatcher(SuccesOrFailure succesOrFailure, Matcher<? super T> valueMatcher) {
        this.succesOrFailure = requireNonNull(succesOrFailure, "successOrFailure must not be null");
        this.valueMatcher = requireNonNull(valueMatcher, "valueMatcher must not be null");
    }

    @Override
    public void describeTo(Description description) {
        requireNonNull(description, "decription must not be null");
        succesOrFailure.describeTo(description);
        description.appendText("where v is ");
        valueMatcher.describeTo(description);
    }

    @Override
    protected boolean matchesSafely(Try<T> item, Description mismatchDescription) {
        requireNonNull(item, "item must not be null");
        requireNonNull(mismatchDescription, "mistmatchDescription must not be null");

        Object valueOrCause = item.isSuccess() ? item.get() : item.getCause();
        mismatchDescription.appendText(" was ");
        SuccesOrFailure.succesOrFailure(item).describeTo(mismatchDescription);
        mismatchDescription.appendText("where v ");
        valueMatcher.describeMismatch(valueOrCause, mismatchDescription);

        return succesOrFailure.matches(item) && valueMatcher.matches(valueOrCause);
    }

    /**
     * Hamcrest static factory method that produces a matcher that verifies an item is
     * a {@link Try.Success} whose {@link Try#get() value} satisfies the matcher specified here.
     *
     * @param valueMatcher the matcher the {@link Try.Success}'s {@link Try#get() value} is
     *                     verified against, must not be {@code null}
     * @param <T> {@link Try}'s value type in case of success
     * @return a matcher, never {@code null}
     */
    @Factory
    public static <T> TryMatcher<T> trySucceededAndValueMatches(Matcher<? super T> valueMatcher) {
        return new TryMatcher<T>(SuccesOrFailure.SUCCESS, valueMatcher);
    }

    /**
     * Hamcrest static factory method that produces a matcher that verifies an item is
     * a {@link Try.Failure} whose {@link Try#getCause() cause} satisfies the matcher specified here.
     *
     * @param valueMatcher the matcher the {@link Try.Failure}'s {@link Try#getCause() cause} is
     *                     verified against, must not be {@code null}
     * @param <T> {@link Try}'s value type in case of success
     * @return a matcher, never {@code null}
     */
    @Factory
    public static <T> TryMatcher<T> tryFailedAndCauseMatches(Matcher<? super T> valueMatcher) {
        return new TryMatcher<T>(SuccesOrFailure.FAILURE, valueMatcher);
    }
}
