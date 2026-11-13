package com.leul.havilor.alert.api;

import com.leul.havilor.alert.api.request.CreateAlertRequest;
import com.leul.havilor.alert.service.AlertIntakeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Manual intake (multipart/form-data): "text" (pasted email or raw headers) and/or up to 5 "files" parts (.eml,
// image, PDF), all read together in one AI call. The answer is a draft for POST /api/v1/alerts; nothing is saved.
@RestController
public class AlertIntakeController {

    private final AlertIntakeService alertIntakeService;

    public AlertIntakeController(AlertIntakeService alertIntakeService) {
        this.alertIntakeService = alertIntakeService;
    }

    @PostMapping(path = "/api/v1/alerts/manual/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CreateAlertRequest extract(@RequestParam(required = false) String text,
                                      @RequestParam(name = "files", required = false) List<MultipartFile> files) {
        return alertIntakeService.extract(text, files);
    }
}
