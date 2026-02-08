package com.ezh.Inventory.contacts.service;

import com.ezh.Inventory.contacts.dto.*;
import com.ezh.Inventory.contacts.entiry.Address;
import com.ezh.Inventory.contacts.entiry.Contact;
import com.ezh.Inventory.contacts.entiry.ContactType;
import com.ezh.Inventory.contacts.entiry.NetworkRequest;
import com.ezh.Inventory.contacts.entiry.NetworkStatus;
import com.ezh.Inventory.contacts.repository.ContactRepository;
import com.ezh.Inventory.contacts.repository.NetworkRequestRepository;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ExternalApiResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.common.dto.TenantDto;
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

import java.util.*;
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
        NetworkRequest netRequest = networkRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Network request not found"));

        Long currentTenantId = getTenantIdOrThrow();

        // 1. Security Check
        if (!netRequest.getReceiverTenantId().equals(currentTenantId)) {
            if(status == NetworkStatus.CONNECTED) {
                throw new BadRequestException("Only the receiver can accept this request.");
            }
        }

        netRequest.setStatus(status);
        networkRepository.save(netRequest);

        // 2. If Accepted -> Create Links for BOTH Sender and Receiver
        if (status == NetworkStatus.CONNECTED) {
            String authToken = request.getHeader("Authorization");
            createReciprocalContacts(netRequest, authToken);
        }

        return CommonResponse.builder()
                .message("Status updated to " + status)
                .status(Status.SUCCESS)
                .build();
    }

    /**
     * Creates two contact records:
     * 1. In Receiver's Book -> Containing Sender's Info
     * 2. In Sender's Book -> Containing Receiver's Info
     */
    private void createReciprocalContacts(NetworkRequest req, String authToken) {
        Long senderId = req.getSenderTenantId();
        Long receiverId = req.getReceiverTenantId();

        // --- Step 1: Fetch Live Details for BOTH Tenants ---
        // We fetch both IDs at once to save network calls
        List<TenantDto> tenantDetails = fetchLiveTenantDetails(List.of(senderId, receiverId), authToken);

        // Map them for easy lookup
        Map<Long, TenantDto> detailsMap = tenantDetails.stream()
                .collect(Collectors.toMap(TenantDto::getId, dto -> dto));

        TenantDto senderInfo = detailsMap.get(senderId);
        TenantDto receiverInfo = detailsMap.get(receiverId);

        // --- Step 2: Create Contact for RECEIVER (The one who accepted) ---
        // They need to see the SENDER
        createSingleSideContact(receiverId, senderId, req, senderInfo);

        // --- Step 3: Create Contact for SENDER (The one who asked) ---
        // They need to see the RECEIVER
        createSingleSideContact(senderId, receiverId, req, receiverInfo);
    }

    private void createSingleSideContact(Long hostTenantId, Long partnerTenantId, NetworkRequest req, TenantDto partnerInfo) {
        // Check if link already exists in this host's book
        if (repository.findByTenantIdAndConnectedTenantId(hostTenantId, partnerTenantId).isPresent()) {
            return;
        }
        // Determine Name and Email (Live > Request > Fallback)
        String name = (partnerInfo != null && partnerInfo.getTenantName() != null)
                ? partnerInfo.getTenantName()
                : "Linked Partner " + partnerTenantId;

        String email = (partnerInfo != null && partnerInfo.getEmail() != null)
                ? partnerInfo.getEmail()
                : "network-" + partnerTenantId + "@placeholder.com";

        String phone = (partnerInfo != null) ? partnerInfo.getPhone() : null;

        Contact contact = Contact.builder()
                .tenantId(hostTenantId)           // <--- Created IN this tenant's scope
                .connectedTenantId(partnerTenantId) // <--- Pointing TO this partner
                .networkRequest(req)
                .contactCode("NET-" + partnerTenantId + "-" + UUID.randomUUID().toString().substring(0,4))
                .name(name)
                .email(email)
                .phone(phone)
                .contactType(ContactType.BOTH)     // Usually B2B partners are BOTH
                .active(true)
                .creditDays(30)
                .build();

        repository.save(contact);
        log.info("Created Seamless Contact in Tenant {} for Partner {}", hostTenantId, partnerTenantId);
    }

    @Override
    @Transactional
    public CommonResponse<?> sendNetworkRequest(NetworkRequestDto dto) throws CommonException {
        Long myTenantId = getTenantIdOrThrow();

        if (myTenantId.equals(dto.getReceiverTenantId())) {
            throw new BadRequestException("You cannot send a trade request to your own company.");
        }

        boolean exists = networkRepository.existsConnection(myTenantId, dto.getReceiverTenantId());
        if (exists) {
            throw new BadRequestException("A network request or connection already exists with this tenant.");
        }

        NetworkRequest req = NetworkRequest.builder()
                .senderTenantId(myTenantId)
                .receiverTenantId(dto.getReceiverTenantId())
                .senderBusinessName(dto.getSenderBusinessName())
                .message(dto.getMessage())
                .status(NetworkStatus.PENDING)
                .build();

        networkRepository.save(req);

        return CommonResponse.builder()
                .message("Trade request sent successfully")
                .status(Status.SUCCESS)
                .id(String.valueOf(req.getId()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NetworkRequest> getIncomingRequests() throws CommonException {
        Long myTenantId = getTenantIdOrThrow();
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
                .active(contact.getActive())
                // New field for frontend to know this is a Network Contact
                .connectedTenantId(contact.getConnectedTenantId());

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

    private List<TenantDto> fetchLiveTenantDetails(List<Long> tenantIds, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.replace("Bearer ", ""));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String idsParam = tenantIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            String url = authServiceUrl + "/api/v1/tenant/bulk?ids=" + idsParam;

            ParameterizedTypeReference<ExternalApiResponse<Map<Long, TenantDto>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ExternalApiResponse<Map<Long, TenantDto>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            if (response.getBody() != null && response.getBody().getData() != null) {
                return new ArrayList<>(response.getBody().getData().values());
            }
        } catch (Exception e) {
            log.error("Failed to fetch live network data: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
