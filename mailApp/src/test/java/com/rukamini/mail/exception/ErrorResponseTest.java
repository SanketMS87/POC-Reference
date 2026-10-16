package com.rukamini.mail.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ErrorResponse}.
 * Covers all constructors, getters, setters, and the {@code addDetail} helper.
 */
@DisplayName("ErrorResponse")
class ErrorResponseTest {

    // ── constructors ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("no-arg constructor sets a non-null timestamp and leaves other fields at defaults")
    void noArgConstructor_setsTimestampAndDefaults() {
        ErrorResponse r = new ErrorResponse();

        assertThat(r.getTimestamp()).isNotNull();
        assertThat(r.getStatus()).isZero();
        assertThat(r.getError()).isNull();
        assertThat(r.getMessage()).isNull();
        assertThat(r.getPath()).isNull();
        assertThat(r.getDetails()).isNull();
    }

    @Test
    @DisplayName(
            "parameterised constructor sets status, error, message, path and initialises timestamp")
    void parameterisedConstructor_setsAllFields() {
        ErrorResponse r = new ErrorResponse(400, "Bad Request", "validation failed", "/api/test");

        assertThat(r.getStatus()).isEqualTo(400);
        assertThat(r.getError()).isEqualTo("Bad Request");
        assertThat(r.getMessage()).isEqualTo("validation failed");
        assertThat(r.getPath()).isEqualTo("/api/test");
        assertThat(r.getTimestamp()).isNotNull();
        assertThat(r.getDetails()).isNull(); // not set by this constructor
    }

    // ── setters / getters ─────────────────────────────────────────────────────

    @Test
    @DisplayName("setters update each field independently")
    void setters_updateEachField() {
        ErrorResponse r = new ErrorResponse();

        r.setStatus(500);
        r.setError("Internal Server Error");
        r.setMessage("something broke");
        r.setPath("/api/other");
        r.setDetails(List.of("detail-a", "detail-b"));

        assertThat(r.getStatus()).isEqualTo(500);
        assertThat(r.getError()).isEqualTo("Internal Server Error");
        assertThat(r.getMessage()).isEqualTo("something broke");
        assertThat(r.getPath()).isEqualTo("/api/other");
        assertThat(r.getDetails()).containsExactly("detail-a", "detail-b");
    }

    @Test
    @DisplayName("setTimestamp replaces the auto-set timestamp")
    void setTimestamp_replacesField() {
        ErrorResponse r = new ErrorResponse();
        OffsetDateTime custom = OffsetDateTime.now().minusDays(1);
        r.setTimestamp(custom);

        assertThat(r.getTimestamp()).isEqualTo(custom);
    }

    // ── addDetail ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName(
            "addDetail on a fresh instance initialises the details list and adds the first entry")
    void addDetail_firstCall_initialisesListAndAdds() {
        ErrorResponse r = new ErrorResponse();
        assertThat(r.getDetails()).isNull(); // precondition

        r.addDetail("first");

        assertThat(r.getDetails()).containsExactly("first");
    }

    @Test
    @DisplayName("addDetail appends successive entries to the same list")
    void addDetail_multipleCalls_appendsInOrder() {
        ErrorResponse r = new ErrorResponse();

        r.addDetail("a");
        r.addDetail("b");
        r.addDetail("c");

        assertThat(r.getDetails()).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("addDetail after setDetails appends to the existing list")
    void addDetail_afterSetDetails_appendsToExistingList() {
        ErrorResponse r = new ErrorResponse();
        r.setDetails(new java.util.ArrayList<>(List.of("existing")));

        r.addDetail("new");

        assertThat(r.getDetails()).containsExactly("existing", "new");
    }
}
