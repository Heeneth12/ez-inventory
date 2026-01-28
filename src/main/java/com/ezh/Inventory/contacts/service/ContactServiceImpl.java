package com.ezh.Inventory.contacts.service;

import com.ezh.Inventory.contacts.dto.*;
import com.ezh.Inventory.contacts.entiry.Address;
import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.entiry.NetworkRequest;
import com.ezh.Inventory.contacts.entiry.NetworkStatus;
import com.ezh.Inventory.contacts.repository.ContactRepository;
import com.ezh.Inventory.contacts.repository.NetworkRequestRepository;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ExternalApiResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.exception.BadRequestException;
import com.ezh.Inventory.utils.exception.CommonException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ezh.Inventory.utils.UserContextUtil.getTenantIdOrThrow;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository repository;
    private final NetworkRequestRepository networkRepository;
    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Override
    @Transactional
    public CommonResponse createContact(ContactDto contactDto) throws CommonException {
        log.info("");
        if (repository.existsByContactCode(contactDto.getContactCode())) {
            throw new BadRequestException("Contact code already exists");
        }

        Contact contact = convertToEntity(contactDto);
        repository.save(contact);

        return CommonResponse.builder()
                .message("Contact created successfully")
                .status(Status.SUCCESS)
                .id(String.valueOf(contact.getId()))
                .build();
    }

    @Override
    @Transactional
    public CommonResponse updateContact(Long id, ContactDto contactDto) throws CommonException {
        Contact existing = repository.findByIdAndTenantId(id, getTenantIdOrThrow())
                .orElseThrow(() -> new BadRequestException("Contact not found"));

        existing.setContactCode(contactDto.getContactCode());
        existing.setName(contactDto.getName());
        existing.setEmail(contactDto.getEmail());
        existing.setPhone(contactDto.getPhone());
        existing.setCreditDays(contactDto.getCreditDays());
        existing.setGstNumber(contactDto.getGstNumber());
        existing.setContactType(contactDto.getType());
        existing.setActive(contactDto.getActive() != null ? contactDto.getActive() : true);

        repository.save(existing);

        return CommonResponse.builder()
                .message("Contact updated successfully")
                .status(Status.SUCCESS)
                .id(String.valueOf(existing.getId()))
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public ContactDto getContact(Long id) throws CommonException {
        Contact contact = repository.findByIdAndTenantId(id, getTenantIdOrThrow())
                .orElseThrow(() -> new BadRequestException("Contact not found"));

        return convertToDTO(contact);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactDto> getAllContacts(ContactFilter contactFilter, Integer page, Integer size) {
        log.info("get all");
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Contact> contacts = repository.searchContacts(
                getTenantIdOrThrow(),
                contactFilter.getSearchQuery(),
                contactFilter.getName(),
                contactFilter.getEmail(),
                contactFilter.getPhone(),
                contactFilter.getGstNumber(),
                contactFilter.getType(),
                contactFilter.getActive(),
                pageable
        );
        //Page<Contact> contacts = repository.findAll(pageable);
        return contacts.map(this::convertToDTO);
    }



    @Override
    @Transactional
    public CommonResponse toggleStatus(Long id, Boolean active) throws CommonException {
        Contact contact = repository.findByIdAndTenantId(id, getTenantIdOrThrow())
                .orElseThrow(() -> new BadRequestException("Contact not found"));

        contact.setActive(active);
        repository.save(contact);

        String statusMsg = active ? "activated" : "deactivated";
        return CommonResponse.builder()
                .message("Contact " + statusMsg + " successfully")
                .status(Status.SUCCESS)
                .id(String.valueOf(contact.getId()))
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> searchContact(ContactFilter contactFilter) throws CommonException {

        log.info("Searching contacts without pagination");

        Pageable pageable = Pageable.unpaged();

        Page<Contact> contacts = repository.searchContacts(
                getTenantIdOrThrow(),
                contactFilter.getSearchQuery(),
                contactFilter.getName(),
                contactFilter.getEmail(),
                contactFilter.getPhone(),
                contactFilter.getGstNumber(),
                contactFilter.getType(),
                contactFilter.getActive(),
                pageable
        );

        return contacts.getContent()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantDto> getMyNetwork() throws CommonException {

        Long myTenantId = getTenantIdOrThrow();
        String authToken = request.getHeader("Authorization");
        List<NetworkRequest> connections = networkRepository.findAllConnections(myTenantId, NetworkStatus.CONNECTED);

        if (connections.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> partnerIds = connections.stream()
                .map(r -> r.getSenderTenantId().equals(myTenantId) ? r.getReceiverTenantId() : r.getSenderTenantId())
                .distinct()
                .toList();

        return fetchLiveTenantDetails(partnerIds, authToken);
    }

    @Override
    @Transactional
    public CommonResponse<?> updateNetworkStatus(Long id, NetworkStatus status) throws CommonException {
        NetworkRequest request = networkRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Network request not found"));

        request.setStatus(status);
        networkRepository.save(request);

        return CommonResponse.builder()
                .message("Status updated to " + status)
                .status(Status.SUCCESS)
                .build();
    }

    @Override
    @Transactional
    public CommonResponse<?> sendNetworkRequest(NetworkRequestDto dto) throws CommonException {
        Long myTenantId = getTenantIdOrThrow();

        // 1. Validation: Prevent sending a request to yourself
        if (myTenantId.equals(dto.getReceiverTenantId())) {
            throw new BadRequestException("You cannot send a trade request to your own company.");
        }

        // 2. Validation: Check if a request already exists between these two tenants
        boolean exists = networkRepository.existsConnection(myTenantId, dto.getReceiverTenantId());
        if (exists) {
            throw new BadRequestException("A network request or connection already exists with this tenant.");
        }

        // 3. Create the Request
        NetworkRequest request = NetworkRequest.builder()
                .senderTenantId(myTenantId)
                .receiverTenantId(dto.getReceiverTenantId())
                .senderBusinessName(dto.getSenderBusinessName()) // This helps the receiver identify you
                .message(dto.getMessage())
                .status(NetworkStatus.PENDING)
                .build();

        networkRepository.save(request);

        return CommonResponse.builder()
                .message("Trade request sent successfully")
                .status(Status.SUCCESS)
                .id(String.valueOf(request.getId()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NetworkRequest> getIncomingRequests() throws CommonException {
        Long myTenantId = getTenantIdOrThrow();

        // Fetch only PENDING requests where the current user is the RECEIVER
        return networkRepository.findByReceiverTenantIdAndStatus(myTenantId, NetworkStatus.PENDING);
    }


    private Contact convertToEntity(ContactDto dto) {

        Contact contact = Contact.builder()
                .tenantId(getTenantIdOrThrow())
                .contactCode(dto.getContactCode())
                .name(dto.getName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .creditDays(dto.getCreditDays())
                .gstNumber(dto.getGstNumber())
                .contactType(dto.getType())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        if (dto.getAddresses() != null && !dto.getAddresses().isEmpty()) {
            dto.getAddresses().forEach(a -> {
                Address address = Address.builder()
                        .addressType(a.getType())
                        .addressLine1(a.getAddressLine1())
                        .addressLine2(a.getAddressLine2())
                        .area(a.getArea())
                        .route(a.getRoute())
                        .city(a.getCity())
                        .state(a.getState())
                        .country(a.getCountry())
                        .pinCode(a.getPinCode())
                        .contact(contact)
                        .build();
                contact.getAddresses().add(address);
            });
        }
        return contact;
    }


    private ContactDto convertToDTO(Contact contact) {

        ContactDto.ContactDtoBuilder builder = ContactDto.builder()
                .id(contact.getId())
                .tenantId(contact.getTenantId())
                .contactCode(contact.getContactCode())
                .name(contact.getName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .creditDays(contact.getCreditDays())
                .gstNumber(contact.getGstNumber())
                .type(contact.getContactType())
                .active(contact.getActive());

        if (contact.getAddresses() != null && !contact.getAddresses().isEmpty()) {
            List<AddressDto> addressDtos = contact.getAddresses()
                    .stream()
                    .map(a -> AddressDto.builder()
                            .id(a.getId())
                            .type(a.getAddressType())
                            .addressLine1(a.getAddressLine1())
                            .addressLine2(a.getAddressLine2())
                            .route(a.getRoute())
                            .area(a.getArea())
                            .city(a.getCity())
                            .state(a.getState())
                            .country(a.getCountry())
                            .pinCode(a.getPinCode())
                            .build()
                    )
                    .toList();

            builder.addresses(addressDtos);
        }
        return builder.build();
    }

    /**
     * Helper to fetch Tenant details from Auth Service and return as DTO list
     */
    private List<TenantDto> fetchLiveTenantDetails(List<Long> tenantIds, String token) {
        try {

            HttpHeaders  headers = new HttpHeaders();
            headers.setBearerAuth(token.replace("Bearer ", ""));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);

            // 2. Wrap into HttpEntity (For GET, the body is null)
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String idsParam = tenantIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            String url = authServiceUrl + "/api/v1/tenant/bulk?ids=" + idsParam;
            log.info("Calling Auth Service URL: {}", url);
            ParameterizedTypeReference<ExternalApiResponse<Map<Long, TenantDto>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ExternalApiResponse<Map<Long, TenantDto>>> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            responseType
                    );

            if (response.getBody() != null && response.getBody().getData() != null) {
                return new ArrayList<>(response.getBody().getData().values());
            }
        } catch (Exception e) {
            log.error("Failed to fetch live network data: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private TenantDto mapToTenantDto(Map<String, Object> data) {
        return TenantDto.builder()
                .id(Long.valueOf(data.get("id").toString()))
                .tenantUuid((String) data.get("tenantUuid"))
                .tenantName((String) data.get("tenantName"))
                .tenantCode((String) data.get("tenantCode"))
                .email((String) data.get("email"))
                .phone((String) data.get("phone"))
                .isActive((Boolean) data.get("isActive"))
                .build();
    }

}
