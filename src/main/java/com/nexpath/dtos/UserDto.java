package com.nexpath.dtos;
import com.nexpath.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String profilePicture;
    private String bio;
    private boolean emailVerified;
    private String createdAt;


}