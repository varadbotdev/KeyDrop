package com.p2pportal.p2p_portal.service;

import com.p2pportal.p2p_portal.model.*;
import com.p2pportal.p2p_portal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class ShareService {

    public static final Logger logger = LoggerFactory.getLogger(ShareService.class);

    @Autowired
    private SharedContentRepository sharedContentRepository;

    @Value("${share.code.length:8}")
    private int codeLength;

    @Value("${share.default.expiry.hours:24}")
    private int defaultExpiryHours;

    public String generateShareCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < codeLength; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        String code = sb.toString();

        // Check if code already exists, regenerate if necessary
        if (sharedContentRepository.existsByShareCode(code)) {
            return generateShareCode();
        }

        return code;
    }

    public SharedContent createSharedContent(String textContent, MultipartFile file, Integer maxViews, Integer expiryHours) {
        logger.info("Creating shared content");

        SharedContent sharedContent = new SharedContent();

        // Generate unique share code
        sharedContent.setShareCode(generateShareCode());

        // Set text content if provided
        if (textContent != null && !textContent.trim().isEmpty()) {
            sharedContent.setContentText(textContent.trim());
            logger.debug("Text content set: {} characters", textContent.length());
        }

        // Set file data if provided
        if (file != null && !file.isEmpty()) {
            try {
                sharedContent.setFileName(file.getOriginalFilename());
                sharedContent.setFileData(file.getBytes());
                sharedContent.setFileContentType(file.getContentType());
                sharedContent.setFileSize(file.getSize());
                logger.debug("File set: name={}, size={}, content-type={}",
                        file.getOriginalFilename(), file.getSize(), file.getContentType());
            } catch (Exception e) {
                logger.error("Failed to store file: " + e.getMessage(), e);
                throw new RuntimeException("Failed to store file: " + e.getMessage());
            }
        }

        // Set metadata
        sharedContent.setCreatedAt(LocalDateTime.now());

        if (expiryHours != null && expiryHours > 0) {
            sharedContent.setExpiresAt(LocalDateTime.now().plusHours(expiryHours));
        } else {
            sharedContent.setExpiresAt(LocalDateTime.now().plusHours(defaultExpiryHours));
        }

        sharedContent.setMaxViews(maxViews);
        sharedContent.setViewCount(0);
        sharedContent.setActive(true);

        SharedContent savedContent = sharedContentRepository.save(sharedContent);
        logger.info("Content saved with ID: {}, Share Code: {}, Has File: {}",
                savedContent.getId(), savedContent.getShareCode(),
                savedContent.getFileData() != null);

        return savedContent;
    }

    public Optional<SharedContent> getSharedContent(String shareCode) {
        logger.info("Retrieving shared content for code: {}", shareCode);

        Optional<SharedContent> sharedContent = sharedContentRepository.findByShareCode(shareCode);

        if (sharedContent.isPresent()) {
            SharedContent content = sharedContent.get();

            // Check if content is still active
            if (!content.getActive()) {
                logger.warn("Content is not active for code: {}", shareCode);
                return Optional.empty();
            }

            // Check expiration
            if (content.getExpiresAt() != null && content.getExpiresAt().isBefore(LocalDateTime.now())) {
                logger.warn("Content has expired for code: {}", shareCode);
                content.setActive(false);
                sharedContentRepository.save(content);
                return Optional.empty();
            }

            // Check view count
            if (content.getMaxViews() != null && content.getViewCount() >= content.getMaxViews()) {
                logger.warn("Content has reached max views for code: {}", shareCode);
                content.setActive(false);
                sharedContentRepository.save(content);
                return Optional.empty();
            }

            // Increment view count
            content.setViewCount(content.getViewCount() + 1);
            sharedContentRepository.save(content);

            logger.info("Content retrieved successfully for code: {}, View count: {}, Has File: {}",
                    shareCode, content.getViewCount(), content.getFileData() != null);

            return Optional.of(content);
        } else {
            logger.warn("Content not found for code: {}", shareCode);
            return Optional.empty();
        }
    }

    public boolean deleteSharedContent(String shareCode) {
        logger.info("Deleting shared content for code: {}", shareCode);

        Optional<SharedContent> sharedContent = sharedContentRepository.findByShareCode(shareCode);

        if (sharedContent.isPresent()) {
            sharedContentRepository.delete(sharedContent.get());
            logger.info("Content deleted successfully for code: {}", shareCode);
            return true;
        }

        logger.warn("Content not found for deletion with code: {}", shareCode);
        return false;
    }
}