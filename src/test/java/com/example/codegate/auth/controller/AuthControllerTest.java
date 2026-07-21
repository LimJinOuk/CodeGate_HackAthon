package com.example.codegate.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthControllerTest {

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void hospitalSignupEndpointIsMapped() throws Exception {
        boolean mapped = handlerMapping.getHandlerMethods().keySet().stream()
                .anyMatch(this::isHospitalSignupMapping);

        assertThat(mapped).isTrue();
    }

    private boolean isHospitalSignupMapping(RequestMappingInfo info) {
        return info.getPatternValues().contains("/api/v1/auth/hospitals/signup")
                && info.getMethodsCondition().getMethods().contains(RequestMethod.POST);
    }
}
