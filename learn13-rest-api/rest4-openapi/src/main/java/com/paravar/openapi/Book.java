package com.paravar.openapi;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Bean Validation rules show up in the schema (required, pattern, minLength) for free
@Schema(description = "A book in the catalog")
public record Book(
        @Schema(description = "Server-assigned id", example = "1", accessMode = Schema.AccessMode.READ_ONLY) Long id,
        @NotBlank @Size(max = 120) @Schema(example = "Domain-Driven Design") String title,
        @NotBlank @Schema(example = "Eric Evans") String author,
        @NotBlank @Pattern(regexp = "\\d{13}") @Schema(description = "ISBN-13", example = "9780321125217") String isbn,
        @Positive @Schema(description = "Price in INR", example = "4500") long price,
        @Schema(description = "Shelf status") Status status) {

    public enum Status { AVAILABLE, BORROWED, LOST }
}
