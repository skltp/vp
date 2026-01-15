package se.skl.tp.vp.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUtilsTest {

    @Mock
    private URL mockUrl;

    @Test
    void readFile_ShouldReturnContent_WhenUrlIsValid() throws IOException {
        // Arrange
        String expectedContent = "Test content";
        InputStream mockInputStream = new ByteArrayInputStream(expectedContent.getBytes());
        when(mockUrl.openStream()).thenReturn(mockInputStream);

        // Act
        String actualContent = FileUtils.readFile(mockUrl);

        // Assert
        assertEquals(expectedContent, actualContent);
        verify(mockUrl, times(1)).openStream();
    }

    @Test
    void readFile_ShouldThrowIllegalArgumentException_WhenUrlIsNull() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> FileUtils.readFile(null));
    }

    @Test
    void readFile_ShouldThrowIllegalStateException_WhenIOExceptionOccurs() throws IOException {
        // Arrange
        when(mockUrl.openStream()).thenThrow(new IOException("Simulated IO error"));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> FileUtils.readFile(mockUrl));
        assertTrue(exception.getMessage().contains("Failed to read resource"));
        assertInstanceOf(IOException.class, exception.getCause());
    }
}
