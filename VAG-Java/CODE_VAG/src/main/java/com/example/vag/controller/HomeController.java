package com.example.vag.controller;

import com.example.vag.model.Artwork;
import com.example.vag.model.Exhibition;
import com.example.vag.model.User;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.ExhibitionService;
import com.example.vag.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ArtworkService artworkService;
    private final ExhibitionService exhibitionService;
    private final UserService userService;

    public HomeController(ArtworkService artworkService, ExhibitionService exhibitionService, UserService userService) {
        this.artworkService = artworkService;
        this.exhibitionService = exhibitionService;
        this.userService = userService;
    }

    @GetMapping({"/", "/index"})
    @Transactional(readOnly = true)
    public String home(Authentication authentication, Model model) {
        List<Artwork> artworks = artworkService.findByStatus("APPROVED");
        Collections.shuffle(artworks);
        List<Artwork> randomArtworks = artworks.subList(0, Math.min(3, artworks.size()));
        randomArtworks.forEach(artwork -> {
            artwork.getUser().getUsername();
            artwork.getCategories().size();
        });
        model.addAttribute("randomArtworks", randomArtworks);

        List<Exhibition> exhibitions = exhibitionService.findAll();
        Collections.shuffle(exhibitions);
        List<Exhibition> randomExhibitions = exhibitions.subList(0, Math.min(3, exhibitions.size()));
        randomExhibitions.forEach(exhibition -> {
            exhibition.getUser().getUsername();
            if (!exhibition.getArtworks().isEmpty()) {
                exhibition.getArtworks().size();
                exhibition.getArtworks().forEach(artwork -> {
                    artwork.getStatus();
                });
            }
        });
        model.addAttribute("randomExhibitions", randomExhibitions);

        List<User> artists = userService.findAll();
        Collections.shuffle(artists);
        List<User> randomArtists = artists.subList(0, Math.min(3, artists.size()));
        randomArtists.forEach(artist -> {
            artist.getArtworks().size();
            artist.getExhibitions().size();
        });
        model.addAttribute("randomArtists", randomArtists);

        return "index";
    }
}