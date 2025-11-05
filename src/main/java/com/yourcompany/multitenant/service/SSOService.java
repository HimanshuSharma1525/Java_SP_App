//// SSOService.java (Base service for common SSO operations)
//package com.yourcompany.multitenant.service;
//
//import com.yourcompany.multitenant.model.SSOProvider;
//import com.yourcompany.multitenant.model.Tenant;
//import com.yourcompany.multitenant.repository.SSOConfigRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class SSOService {
//
//    private final SSOConfigRepository ssoConfigRepository;
//    private final TenantService tenantService;
//
//    public List<String> getEnabledProvidersForCurrentTenant() {
//        Tenant tenant = tenantService.getCurrentTenant();
//        return ssoConfigRepository.findByTenantAndEnabledTrue(tenant)
//                .stream()
//                .map(config -> config.getProvider().name())
//                .collect(Collectors.toList());
//    }
//
//    public boolean isProviderEnabled(SSOProvider provider) {
//        Tenant tenant = tenantService.getCurrentTenant();
//        return ssoConfigRepository.findByTenantAndProvider(tenant, provider)
//                .map(config -> config.getEnabled())
//                .orElse(false);
//    }
//}