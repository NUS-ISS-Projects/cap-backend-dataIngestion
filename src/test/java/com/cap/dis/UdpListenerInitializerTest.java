package com.cap.dis;

import com.cap.dis.service.UdpListenerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UdpListenerInitializerTest {

    @Mock
    private UdpListenerService udpListenerService;

    @InjectMocks
    private UdpListenerInitializer udpListenerInitializer;

    @Test
    void run_shouldCallStartListeningOnService() throws Exception {
        // Arrange
        String[] args = {};

        // Act
        udpListenerInitializer.run(args);

        // Assert
        verify(udpListenerService, times(1)).startListening();
    }
}