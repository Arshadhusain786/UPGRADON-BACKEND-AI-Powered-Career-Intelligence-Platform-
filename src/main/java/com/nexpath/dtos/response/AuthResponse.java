package com.nexpath.dtos.response;


import com.nexpath.dtos.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserDto user;
}
