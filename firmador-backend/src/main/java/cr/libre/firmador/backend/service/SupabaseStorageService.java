package cr.libre.firmador.backend.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    public String uploadDocument(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = originalFilename == null || originalFilename.isBlank()
            ? "document.bin"
            : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

        return UUID.randomUUID() + "-" + safeFilename;
    }

    public Resource getDocument(String filePath) {
        try {
            return new ByteArrayResource(new byte[0]) {
                @Override
                public String getFilename() {
                    return filePath;
                }
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load document: " + filePath, ex);
        }
    }

    public byte[] readMultipartFile(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to read uploaded file", ex);
        }
    }
}

// Made with Bob
