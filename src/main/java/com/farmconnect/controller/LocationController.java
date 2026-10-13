package com.farmconnect.controller;

import com.farmconnect.service.Locations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The counties and sub-counties the app covers. Any logged-in user. */
@RestController
@RequestMapping("/api/locations")
public class LocationController {
    @GetMapping
    public List<Locations.County> all() { return Locations.all(); }
}
