package com.inkwell.handwriting;

import com.inkwell.common.ApiException;
import com.inkwell.handwriting.HandwritingDtos.*;
import com.inkwell.ml.MlClient;
import com.inkwell.ml.MlClient.ExtractResponse;
import com.inkwell.ml.MlClient.NamedBytes;
import com.inkwell.ml.MlClient.RenderResponse;
import com.inkwell.security.AuthUser;
import com.inkwell.user.UserRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HandwritingService {
    private static final int MAX_PROFILES_PER_USER = 5;

    private final HandwritingProfileRepository profiles;
    private final UserRepository users;
    private final MlClient ml;

    public HandwritingService(HandwritingProfileRepository profiles, UserRepository users, MlClient ml) {
        this.profiles = profiles;
        this.users = users;
        this.ml = ml;
    }

    public RecognizeResult recognize(MultipartFile image) {
        byte[] data = readImage(image);
        var r = ml.recognize(data, image.getOriginalFilename());
        return new RecognizeResult(r.text(), r.language(), r.engine());
    }

    @Transactional(readOnly = true)
    public List<ProfileDto> list(AuthUser me) { return profiles.findProfiles(me.id()); }

    @Transactional
    public ProfileCreated create(AuthUser me, String name, boolean consent, List<MultipartFile> sheets) {
        if (!consent) throw ApiException.badRequest("Consent is required: your handwriting sample is personal data");
        if (name == null || name.isBlank() || name.length() > 80) throw ApiException.badRequest("Give the profile a name (up to 80 characters)");
        if (sheets == null || sheets.isEmpty() || sheets.size() > 6) throw ApiException.badRequest("Upload between 1 and 6 filled sheets");
        if (profiles.countByOwnerId(me.id()) >= MAX_PROFILES_PER_USER) {
            throw ApiException.conflict("You can keep up to " + MAX_PROFILES_PER_USER + " handwriting profiles. Delete one first.");
        }
        List<NamedBytes> files = new ArrayList<>();
        for (MultipartFile f : sheets) files.add(new NamedBytes(readImage(f), f.getOriginalFilename()));

        ExtractResponse extracted = ml.extractGlyphs(files);
        HandwritingProfile p = new HandwritingProfile();
        p.setOwner(users.getReferenceById(me.id()));
        p.setName(name.trim());
        p.setCoveredChars(extracted.covered());
        p.setMissingChars(extracted.missing() == null ? "" : extracted.missing());
        p.setGlyphPack(Base64.getDecoder().decode(extracted.packB64()));
        p.setConsentAt(Instant.now());
        profiles.save(p);
        return new ProfileCreated(new ProfileDto(p.getId(), p.getName(), p.getCoveredChars(), p.getMissingChars(),
                p.getCreatedAt()), extracted.sheets());
    }

    @Transactional
    public void delete(AuthUser me, Long id) {
        HandwritingProfile p = profiles.findByIdAndOwnerId(id, me.id()).orElseThrow(() -> ApiException.notFound("Profile"));
        profiles.delete(p);
    }

    /** Rendering is limited to the profile's owner: nobody can generate text in someone else's handwriting. */
    @Transactional(readOnly = true)
    public RenderResult render(AuthUser me, Long id, RenderRequest req) {
        HandwritingProfile p = profiles.findByIdAndOwnerId(id, me.id()).orElseThrow(() -> ApiException.notFound("Profile"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("packB64", Base64.getEncoder().encodeToString(p.getGlyphPack()));
        body.put("text", req.text());
        body.put("paper", req.paper() == null ? "ruled" : req.paper());
        body.put("inkColor", req.inkColor() == null ? "#1b2a6b" : req.inkColor());
        body.put("size", req.size() == null ? 64 : req.size());
        body.put("width", req.width() == null ? 1200 : req.width());
        if (req.seed() != null) body.put("seed", req.seed());
        RenderResponse r = ml.render(body);
        return new RenderResult("data:image/png;base64," + r.imageB64(), r.width(), r.height(), r.missing());
    }

    private static byte[] readImage(MultipartFile f) {
        if (f == null || f.isEmpty()) throw ApiException.badRequest("Empty file");
        String type = f.getContentType();
        if (type == null || !type.startsWith("image/")) throw ApiException.badRequest("Only image files are accepted (JPEG, PNG, WebP, HEIC)");
        try {
            return f.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
    }
}
