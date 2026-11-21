package com.inkwell.handwriting;

import com.inkwell.common.ApiException;
import com.inkwell.handwriting.HandwritingDtos.*;
import com.inkwell.ml.MlClient;
import com.inkwell.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/handwriting")
public class HandwritingController {
    private static final Pattern SHEETS = Pattern.compile("^(ru|en|he)(,(ru|en|he))*$");

    private final HandwritingService service;
    private final MlClient ml;

    public HandwritingController(HandwritingService service, MlClient ml) {
        this.service = service;
        this.ml = ml;
    }

    /** Printable sheet(s) to write the alphabet on. Public: you can print it before signing up. */
    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam(defaultValue = "ru,en,he") String sheets,
                                           @RequestParam(defaultValue = "pdf") String format) {
        if (!SHEETS.matcher(sheets).matches()) throw ApiException.badRequest("sheets must be a list of ru, en, he");
        if (!format.equals("pdf") && !format.equals("png")) throw ApiException.badRequest("format must be pdf or png");
        ResponseEntity<byte[]> upstream = ml.template(sheets, format);
        return ResponseEntity.ok()
                .contentType(format.equals("pdf") ? MediaType.APPLICATION_PDF : MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"inkwell-sheets." + format + "\"")
                .body(upstream.getBody());
    }

    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecognizeResult recognize(@RequestPart("image") MultipartFile image) { return service.recognize(image); }

    @GetMapping("/profiles")
    public List<ProfileDto> profiles(@AuthenticationPrincipal AuthUser me) { return service.list(me); }

    @PostMapping(value = "/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileCreated create(@RequestParam String name,
                                 @RequestParam boolean consent,
                                 @RequestPart("sheets") List<MultipartFile> sheets,
                                 @AuthenticationPrincipal AuthUser me) {
        return service.create(me, name, consent, sheets);
    }

    @DeleteMapping("/profiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { service.delete(me, id); }

    @PostMapping("/profiles/{id}/render")
    public RenderResult render(@PathVariable Long id, @Valid @RequestBody RenderRequest req,
                               @AuthenticationPrincipal AuthUser me) {
        return service.render(me, id, req);
    }
}
