// UpdateUserRequest.java
package com.yourcompany.multitenant.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;
    private Boolean active;
}
