package com.ccut.service.Impl;

import com.ccut.dto.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    @TempDir
    Path uploadDir;

    @Test
    void saveFileWrapsStoredUploadAsAttachment() throws Exception {
        UploadStorageServiceImpl storageService = new UploadStorageServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(storageService, "uploadDir", uploadDir.toString());

        FileStorageServiceImpl service = new FileStorageServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "uploadStorageService", storageService);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Lesson.PDF",
                "application/pdf",
                "content".getBytes()
        );

        Attachment attachment = service.saveFile(file);

        assertEquals("document", attachment.type());
        assertEquals("Lesson.PDF", attachment.filename());
        assertEquals(7L, attachment.size());
        assertTrue(attachment.url().startsWith("/uploads/" + LocalDate.now() + "/"));
        assertTrue(attachment.url().endsWith(".pdf"));

        Path savedPath = uploadDir.resolve(attachment.url().replaceFirst("^/uploads/", ""));
        assertTrue(Files.exists(savedPath));
        assertEquals("content", Files.readString(savedPath));
    }

    @Test
    void deleteFileDelegatesToStorageBoundary() throws Exception {
        UploadStorageServiceImpl storageService = new UploadStorageServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(storageService, "uploadDir", uploadDir.toString());

        FileStorageServiceImpl service = new FileStorageServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "uploadStorageService", storageService);

        Path datedDir = uploadDir.resolve("2026-04-21");
        Files.createDirectories(datedDir);
        Path file = datedDir.resolve("note.txt");
        Files.writeString(file, "delete me");

        assertTrue(service.deleteFile("/uploads/2026-04-21/note.txt"));
        assertFalse(Files.exists(file));

        Path outside = Files.createTempFile("outside", ".txt");
        try {
            assertFalse(service.deleteFile("/uploads/../" + outside.getFileName()));
            assertTrue(Files.exists(outside));
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void saveFilesSkipsEmptyFiles() {
        FileStorageServiceImpl service = new FileStorageServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "uploadStorageService", mock(com.ccut.service.UploadStorageService.class));

        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertTrue(service.saveFiles(java.util.List.of(empty)).isEmpty());
    }

    @Test
    void storageSupportsCompatibleUploadSubdirectories() throws Exception {
        UploadStorageServiceImpl storageService = new UploadStorageServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(storageService, "uploadDir", uploadDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "homework.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "doc".getBytes()
        );

        com.ccut.dto.StoredUpload stored = storageService.store(file, "homework/teacher");

        assertTrue(stored.url().startsWith("/uploads/homework/teacher/" + LocalDate.now() + "/"));
        assertTrue(Files.exists(uploadDir.resolve(stored.url().replaceFirst("^/uploads/", ""))));
    }
}
