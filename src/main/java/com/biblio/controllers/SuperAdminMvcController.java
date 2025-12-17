package com.biblio.controllers;

import com.biblio.dao.PretDAO;
import com.biblio.dao.RessourceDAO;
import com.biblio.dao.UserDAO;
import com.biblio.entities.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminMvcController {
    private final UserDAO userDAO;
    private final PretDAO pretDAO;
    private final RessourceDAO ressourceDAO;

    public SuperAdminMvcController(UserDAO userDAO, PretDAO pretDAO, RessourceDAO ressourceDAO) {
        this.userDAO = userDAO;
        this.pretDAO = pretDAO;
        this.ressourceDAO = ressourceDAO;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userDAO.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        model.addAttribute("user", user);

        // Fetch analytics data
        List<Object[]> pretsByCategorie = pretDAO.countPretsByCategorie();
        List<Object[]> pretsByBibliotheque = pretDAO.countPretsByBibliotheque();
        long totalPrets = pretDAO.count();
        Integer totalStock = ressourceDAO.sumTotalExemplaires();
        
        // Calculate rotation rate
        double rotationRate = 0.0;
        if (totalStock != null && totalStock > 0) {
            rotationRate = (double) totalPrets / totalStock;
        }

        // Prepare data for charts
        List<String> catLabels = new ArrayList<>();
        List<Long> catData = new ArrayList<>();
        for (Object[] row : pretsByCategorie) {
            catLabels.add(row[0].toString());
            catData.add((Long) row[1]);
        }

        List<String> bibLabels = new ArrayList<>();
        List<Long> bibData = new ArrayList<>();
        for (Object[] row : pretsByBibliotheque) {
            bibLabels.add(row[0].toString());
            bibData.add((Long) row[1]);
        }

        model.addAttribute("catLabels", catLabels);
        model.addAttribute("catData", catData);
        model.addAttribute("bibLabels", bibLabels);
        model.addAttribute("bibData", bibData);
        model.addAttribute("rotationRate", String.format("%.2f", rotationRate));

        return "dashboard-admin";
    }
}
