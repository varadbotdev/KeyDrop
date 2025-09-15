package com.p2pportal.p2p_portal.controller;


import com.p2pportal.p2p_portal.model.*;
import com.p2pportal.p2p_portal.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.util.Optional;

import static com.p2pportal.p2p_portal.service.ShareService.logger;

@Controller
public class ShareController {

    @Autowired
    private ShareService shareService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/share")
    public String showSharePage(Model model) {
        logger.info("Accessing share page");
        return "share";
    }

    @PostMapping("/share")
    public String createShare(
            @RequestParam(value = "text", required = false) String textContent,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "maxViews", required = false) Integer maxViews,
            @RequestParam(value = "expiryHours", required = false) Integer expiryHours,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Validate that at least text or file is provided
        if ((textContent == null || textContent.trim().isEmpty()) && (file == null || file.isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "Please provide either text content or a file to share.");
            return "redirect:/share";
        }

        try {
            SharedContent sharedContent = shareService.createSharedContent(textContent, file, maxViews, expiryHours);
            redirectAttributes.addFlashAttribute("success", "Content shared successfully!");
            redirectAttributes.addFlashAttribute("shareCode", sharedContent.getShareCode());
            return "redirect:/share/success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to share content: " + e.getMessage());
            return "redirect:/share";
        }
    }

    @GetMapping("/share/success")
    public String shareSuccess() {
        return "share";
    }

    @GetMapping("/view/{shareCode}")
    public String viewContent(@PathVariable String shareCode, Model model) {
        Optional<SharedContent> sharedContent = shareService.getSharedContent(shareCode);

        if (sharedContent.isPresent()) {
            model.addAttribute("content", sharedContent.get());
            return "view";
        } else {
            model.addAttribute("error", "Content not found or expired.");
            return "error";
        }
    }

    @GetMapping("/download/{shareCode}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String shareCode) {
        logger.info("=== Downloading File for Share Code: {} ===", shareCode);
        Optional<SharedContent> sharedContent = shareService.getSharedContent(shareCode);

        if (sharedContent.isPresent() && sharedContent.get().getFileData() != null) {
            SharedContent content = sharedContent.get();

            logger.info("File details - Name: {}, Size: {} bytes, Type: {}",
                    content.getFileName(),
                    content.getFileData().length,
                    content.getFileContentType());

            ByteArrayResource resource = new ByteArrayResource(content.getFileData());

            // Build the Content-Disposition header with proper filename encoding
            String fileName = content.getFileName() != null ? content.getFileName() : "downloaded-file";
            String encodedFileName = UriUtils.encode(fileName, "UTF-8");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            content.getFileContentType() != null ?
                                    content.getFileContentType() :
                                    MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .contentLength(content.getFileData().length)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } else {
            logger.warn("No file data found for share code: {}", shareCode);
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/enter-code")
    public String showCodeEntryPage() {
        logger.info("Showing code entry page");
        return "enter-code";
    }
    @GetMapping("/quick-view")
    public String quickView(@RequestParam("code") String shareCode, Model model) {
        logger.info("Quick view requested for code: {}", shareCode);

        // Validate the code
        if (shareCode == null || shareCode.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a valid share code");
            return "enter-code";
        }

        // Redirect to the actual view page
        return "redirect:/view/" + shareCode.trim();
    }
    @PostMapping("/delete/{shareCode}")
    public String deleteContent(@PathVariable String shareCode, RedirectAttributes redirectAttributes) {
        boolean deleted = shareService.deleteSharedContent(shareCode);

        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "Content deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Content not found or already deleted.");
        }

        return "redirect:/";
    }
}