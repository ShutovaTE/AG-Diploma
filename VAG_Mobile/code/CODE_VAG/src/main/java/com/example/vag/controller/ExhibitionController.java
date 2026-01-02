package com.example.vag.controller;

import com.example.vag.model.Artwork;
import com.example.vag.model.Exhibition;
import com.example.vag.model.User;
import com.example.vag.service.ArtworkService;
import com.example.vag.service.ExhibitionService;
import com.example.vag.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/exhibition")
public class ExhibitionController {

    private final ExhibitionService exhibitionService;
    private final ArtworkService artworkService;
    private final UserService userService;


    public ExhibitionController(ExhibitionService exhibitionService,
                                ArtworkService artworkService,
                                UserService userService) {
        this.exhibitionService = exhibitionService;
        this.artworkService = artworkService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public String listExhibitions(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "12") int size,
                                  Model model) {
        page = Math.max(0, page);
        
        Page<Exhibition> exhibitionPage = exhibitionService.findPaginatedExhibitions(page, size);
        
        exhibitionPage.getContent().forEach(exhibition -> {
            model.addAttribute("approvedArtworksCount_" + exhibition.getId(), 
                exhibitionService.countApprovedArtworksInExhibition(exhibition.getId()));
            model.addAttribute("firstApprovedArtwork_" + exhibition.getId(), 
                exhibitionService.getFirstApprovedArtworkInExhibition(exhibition.getId()));
        });
        
        model.addAttribute("exhibitions", exhibitionPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", exhibitionPage.getTotalPages());
        return "exhibition/list";
    }

    @GetMapping("/details/{id}")
    public String viewExhibition(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        Exhibition exhibition = exhibitionService.findById(id).orElseThrow();
        Pageable pageable = PageRequest.of(page, size);
        Page<Artwork> artworkPage = artworkService.findByExhibitionId(id, pageable);

        model.addAttribute("exhibition", exhibition);
        model.addAttribute("artworks", artworkPage);
        model.addAttribute("approvedArtworksCount", exhibitionService.countApprovedArtworksInExhibition(id));
        model.addAttribute("firstApprovedArtwork", exhibitionService.getFirstApprovedArtworkInExhibition(id));
        return "exhibition/details";
    }

    @GetMapping("/create")
    public String showCreateExhibition(Model model) {
        model.addAttribute("exhibition", new Exhibition());
        return "exhibition/create";
    }

    @PostMapping("/create")
    public String createExhibition(@Valid @ModelAttribute("exhibition") Exhibition exhibition,
                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "exhibition/create";
        }

        User user = userService.getCurrentUser();
        exhibitionService.create(exhibition, user);

        return "redirect:/user/profile?created";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Exhibition existingExhibition = exhibitionService.findById(id).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!existingExhibition.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        model.addAttribute("exhibition", existingExhibition);
        return "exhibition/edit";
    }

    @PostMapping("/edit")
    public String updateExhibition(@Valid @ModelAttribute("exhibition") Exhibition exhibition,
                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "exhibition/edit";
        }

        exhibitionService.update(exhibition);

        return "redirect:/user/profile?updated";
    }

    @PostMapping("/delete/{id}")
    public String deleteExhibition(@PathVariable Long id) {
        Exhibition exhibition = exhibitionService.findById(id).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!exhibition.getUser().getId().equals(currentUser.getId()) &&
                !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        exhibitionService.delete(exhibition);
        return "redirect:/user/profile?deleted";
    }

    @PostMapping("/add-artwork/{exhibitionId}/{artworkId}")
    public String addArtworkToExhibition(@PathVariable Long exhibitionId,
                                         @PathVariable Long artworkId) {
        Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
        Artwork artwork = artworkService.findById(artworkId).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (exhibition.isAuthorOnly() && !exhibition.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/auth/access-denied";
        }

        exhibition.getArtworks().add(artwork);
        artwork.getExhibitions().add(exhibition);
        exhibitionService.save(exhibition);
        artworkService.save(artwork);

        return "redirect:/exhibition/details/" + exhibitionId;
    }

    @PostMapping("/remove-artwork/{exhibitionId}/{artworkId}")
    public String removeArtworkFromExhibition(@PathVariable Long exhibitionId,
                                              @PathVariable Long artworkId) {
        Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
        Artwork artwork = artworkService.findById(artworkId).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!exhibition.getUser().getId().equals(currentUser.getId()) && 
            !artwork.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        exhibitionService.removeArtworkFromExhibition(exhibitionId, artworkId);

        return "redirect:/exhibition/details/" + exhibitionId;
    }

    @GetMapping("/exhibitions")
    public String showExhibitions(Model model, @RequestParam(defaultValue = "0") int page) {
        page = Math.max(0, page);
        
        int pageSize = 6;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());
        Page<Exhibition> exhibitionPage = exhibitionService.getPublicExhibitions(pageable);
        
        if (page > 0 && exhibitionPage.getContent().isEmpty()) {
            return "redirect:/exhibition/exhibitions?page=" + (page - 1);
        }
        
        exhibitionPage.getContent().forEach(exhibition -> {
            model.addAttribute("approvedArtworksCount_" + exhibition.getId(), 
                exhibitionService.countApprovedArtworksInExhibition(exhibition.getId()));
            model.addAttribute("firstApprovedArtwork_" + exhibition.getId(), 
                exhibitionService.getFirstApprovedArtworkInExhibition(exhibition.getId()));
        });
        
        model.addAttribute("exhibitions", exhibitionPage);
        return "exhibition/list";
    }

    @GetMapping("/add-exist-artwork/{exhibitionId}")
    @Transactional(readOnly = true)
    public String addExistArtwork(@PathVariable Long exhibitionId, Model model) {
        Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
        User currentUser = userService.getCurrentUser();
        
        List<Artwork> userArtworks = artworkService.findByUserWithDetails(currentUser);
        
        Set<Artwork> exhibitionArtworks = exhibition.getArtworks();
        
        model.addAttribute("exhibition", exhibition);
        model.addAttribute("artworks", userArtworks);
        model.addAttribute("exhibitionArtworks", exhibitionArtworks);
        return "exhibition/add-exist-artwork";
    }

    @PostMapping("/add-exist-artwork/{exhibitionId}/{artworkId}")
    @Transactional
    public String addExistArtwork(@PathVariable Long exhibitionId, @PathVariable Long artworkId) {
        Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
        Artwork artwork = artworkService.findByIdWithCategories(artworkId).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (exhibition.isAuthorOnly() && !exhibition.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/auth/access-denied";
        }

        if (!artwork.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/auth/access-denied";
        }

        exhibition.getArtworks().add(artwork);
        artwork.getExhibitions().add(exhibition);
        exhibitionService.save(exhibition);
        artworkService.save(artwork);

        return "redirect:/exhibition/details/" + exhibitionId;
    }

    @PostMapping("/remove-exist-artwork/{exhibitionId}/{artworkId}")
    public String removeExistArtwork(@PathVariable Long exhibitionId, @PathVariable Long artworkId) {
        Exhibition exhibition = exhibitionService.findById(exhibitionId).orElseThrow();
        Artwork artwork = artworkService.findById(artworkId).orElseThrow();
        User currentUser = userService.getCurrentUser();

        if (!exhibition.getUser().getId().equals(currentUser.getId()) &&
            !artwork.getUser().getId().equals(currentUser.getId()) &&
            !currentUser.hasRole("ADMIN")) {
            return "redirect:/auth/access-denied";
        }

        exhibitionService.removeArtworkFromExhibition(exhibitionId, artworkId);

        return "redirect:/exhibition/add-exist-artwork/" + exhibitionId;
    }
}