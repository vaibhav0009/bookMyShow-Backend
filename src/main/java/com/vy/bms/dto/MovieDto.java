package com.vy.bms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {
    private Long id;
    @NotBlank(message = "title is required")
    private String title;
    private String description;
    @NotBlank(message = "language is required")
    private String language;
    @NotBlank(message = "genre is required")
    private String genre;
    @NotNull(message = "durationMins is required")
    @Positive(message = "durationMins must be greater than 0")
    private Integer durationMins;
    private String releaseDate;
    private String posterUrl;
}