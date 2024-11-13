package com.example.securedrive.security;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.StrictHttpFirewall;

public class CustomHttpFirewall extends StrictHttpFirewall {

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
        String requestUri = request.getRequestURI();
        // Replace %0A (newline) with an empty string
        if (requestUri.contains("%0A")) {
            requestUri = requestUri.replace("%0A", "");
        }

        // Wrap the original request with our custom FirewalledRequestWrapper that returns modified URI
        return new FirewalledRequestWrapper(request, requestUri);
    }

    // Custom request wrapper class that overrides getRequestURI to return the modified requestUri
    private static class FirewalledRequestWrapper extends FirewalledRequest {

        private final String modifiedRequestUri;

        public FirewalledRequestWrapper(HttpServletRequest request, String modifiedRequestUri) {
            super(request);
            this.modifiedRequestUri = modifiedRequestUri;
        }

        @Override
        public String getRequestURI() {
            return this.modifiedRequestUri;  // Return the modified URI
        }

        @Override
        public void reset() {
            // No implementation needed for now, but required by FirewalledRequest
        }
    }
}
