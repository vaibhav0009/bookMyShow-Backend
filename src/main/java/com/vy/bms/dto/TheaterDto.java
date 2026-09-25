package com.vy.bms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheaterDto {
    private Long id;
    @NotBlank(message = "name is required")
    private String name;
    private String address;
    @NotBlank(message = "city is required")
    private String city;
    @Positive(message = "totalScreens must be greater than 0")
    private Integer totalScreens;
}