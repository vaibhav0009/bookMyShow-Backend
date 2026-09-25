package com.vy.bms.controller;

import com.vy.bms.dto.TheaterDto;
import com.vy.bms.service.TheaterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theaters")
public class TheaterController {

    @Autowired
    private TheaterService theaterService;

    @PostMapping
    public ResponseEntity<TheaterDto> createTheater(@Valid @RequestBody TheaterDto theaterDto)
    {
        return new ResponseEntity<>(theaterService.createTheater(theaterDto), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TheaterDto> getTheaterById(@PathVariable Long id)
    {
        return ResponseEntity.ok(theaterService.getTheaterById(id));
    }

    @GetMapping
    public ResponseEntity<List<TheaterDto>> getTheaters(@RequestParam(required = false) String city)
    {
        if (city == null || city.isBlank()) {
            return ResponseEntity.ok(theaterService.getAllTheaters());
        }
        return ResponseEntity.ok(theaterService.getAllTheaterByCity(city));
    }
}