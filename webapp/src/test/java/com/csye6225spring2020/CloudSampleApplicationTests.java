package com.csye6225spring2020;

import com.csye6225spring2020.controller.UserController;
import com.csye6225spring2020.entity.User;
import com.csye6225spring2020.repository.UserRepositry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.boot.test.context.SpringBootTest;



import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class CloudSampleApplicationTests {

    @InjectMocks
    UserController userController;

    @Mock
    UserRepositry userRepositry;
    @Mock
    Environment environment;


    @Test
    public void testAddUser()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        User userRep = new User();
        String[]profile = {"aws"};
        when(userRepositry.save(any(User.class))).thenReturn(userRep);
        when(environment.getActiveProfiles()).thenReturn(profile);
        when(userRepositry.findByEmailAddress(any())).thenReturn(null).thenReturn(userRep);
        userRep.setFirst_name("test");
        userRep.setLast_name("last");
        userRep.setEmailAddress("test@gmail.com");
        userRep.setPassword("Test@1234567");
        ResponseEntity<String> responseEntity = userController.createUser(userRep);

        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(201);
    }

}
