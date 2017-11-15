package com.haufe.testutils.hamcrest;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;

import java.util.function.Predicate;

/**
 * Hamcrest matcher based on the CharSequence predicates found inApache Commons Lang3's  {@link StringUtils}.
 */
public class StringUtilsPredicateMatcher extends DiagnosingMatcher<CharSequence> {

    private final Predicate<CharSequence> predicate;
    private final String expectationText;
    private final String mismatchText;

    private StringUtilsPredicateMatcher(Predicate<CharSequence> predicate,
                                        String expectationText, String mismatchText) {
        this.predicate = predicate;
        this.expectationText = expectationText;
        this.mismatchText = mismatchText;
    }

    /**
     * Creates a matcher that succeeds if and only if {@link StringUtils#isNotBlank(CharSequence)} is {@literal true},
     * (i.e., if the examined {@link CharSequence} is not {@literal null} and contains at least one
     * non-whitespace character).
     *
     * @return a matcher based on {@link StringUtils#isNotBlank(CharSequence)}
     */
    @Factory
    public static StringUtilsPredicateMatcher isNotBlank() {
        return new StringUtilsPredicateMatcher(StringUtils::isNotBlank,
                                               "a non-blank CharSequence", " is blank");
    }

    /**
     * Creates a matcher that succeeds if and only if {@link StringUtils#isBlank(CharSequence)} is {@literal true},
     * (i.e., if the examined {@link CharSequence} is {@literal null} or contains whitespace characters only, if any).
     *
     * @return a matcher based on {@link StringUtils#isBlank(CharSequence)}
     */
    @Factory
    public static StringUtilsPredicateMatcher isBlank() {
        return new StringUtilsPredicateMatcher(StringUtils::isBlank,
                                               "a blank CharSequence", " is not blank");
    }

    /**
     * Creates a matcher that succeeds if and only if {@link StringUtils#isNotEmpty(CharSequence)} is {@literal true},
     * (i.e., if the examined {@link CharSequence} is not {@literal null} and contains at least one
     * character).
     *
     * @return a matcher based on {@link StringUtils#isNotEmpty(CharSequence)}
     */
    @Factory
    public static StringUtilsPredicateMatcher isNotEmpty() {
        return new StringUtilsPredicateMatcher(StringUtils::isNotEmpty,
                                               "a non-empty CharSequence", " is empty");
    }

    /**
     * Creates a matcher that succeeds if and only if {@link StringUtils#isEmpty(CharSequence)} is {@literal true},
     * (i.e., if the examined {@link CharSequence} is {@literal null} or has
     * {@link CharSequence#length() length} {@literal 0}).
     *
     * @return a matcher based on {@link StringUtils#isEmpty(CharSequence)}
     */
    @Factory
    public static StringUtilsPredicateMatcher isEmpty() {
        return new StringUtilsPredicateMatcher(StringUtils::isEmpty,
                                               "an empty CharSequence", " is not empty");
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        if(!(item == null || item instanceof CharSequence)) {
            mismatchDescription.appendValue(item).appendText(" is not a CharSequence");
            return false;
        }
        CharSequence charSequence = (CharSequence)item;
        boolean result = predicate.test(charSequence);
        if(!result) {
            mismatchDescription.appendValue(item).appendText(mismatchText);
        }
        return result;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expectationText);
    }
}
