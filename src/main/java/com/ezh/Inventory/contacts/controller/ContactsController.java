package com.ezh.Inventory.contacts.controller;


import com.ezh.Inventory.contacts.dto.ContactDto;
import com.ezh.Inventory.contacts.dto.ContactFilter;
import com.ezh.Inventory.contacts.dto.NetworkRequestDto;
import com.ezh.Inventory.utils.common.dto.TenantDto;
import com.ezh.Inventory.contacts.entiry.NetworkRequest;
import com.ezh.Inventory.contacts.entiry.NetworkStatus;
import com.ezh.Inventory.contacts.service.ContactService;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.ResponseResource;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/contact")
@AllArgsConstructor
public class ContactsController {

    private final ContactService contactService;

    /**
     * @Method : createContact
     * @Discriptim :
     *
     * @param contactDto
     * @return
     * @throws CommonException
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> createContact(@RequestBody ContactDto contactDto) throws CommonException{
        log.info("Creating new Contact with {}", contactDto);
        CommonResponse response = contactService.createContact(contactDto);
        return ResponseResource.success(HttpStatus.CREATED, response, " SUCCESSFULLY Created ");
    }

    /**
     *
     * @Method : updateContact
     * @Discriptim :
     *
     * @param id
     * @param contactDto
     * @return
     * @throws CommonException
     */
    @PostMapping(value = "/{id}/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse> updateContact(@PathVariable Long id, @RequestBody ContactDto contactDto) throws CommonException {
        log.info("Updating Contact {}: {}", id, contactDto);
        CommonResponse response = contactService.updateContact(id, contactDto);
        return ResponseResource.success(HttpStatus.OK, response, "Successfully Updated");
    }

    /**
     * @Method : getContact
     * @Discriptim :
     *
     * @param id
     * @return
     * @throws CommonException
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<ContactDto> getContact(@PathVariable Long id) throws CommonException  {
        log.info("Fetching Contact with ID: {}", id);
        ContactDto contactDto = contactService.getContact(id);
        return ResponseResource.success(HttpStatus.OK, contactDto, "Contact fetched successfully");
    }

    /**
     * @Method : getAllContacts
     * @Discriptim :
     *
     *
     * @param page
     * @param size
     * @param contactFilter
     * @return
     * @throws CommonException
     */
    @PostMapping(path = "/all", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<Page<ContactDto>> getAllContacts(@RequestParam Integer page, @RequestParam Integer size,
                                                             @RequestBody ContactFilter contactFilter) throws CommonException{
        log.info("Fetching all contacts with filter: {}", contactFilter);
        Page<ContactDto> contacts = contactService.getAllContacts(contactFilter, page, size);
        return ResponseResource.success(HttpStatus.OK, contacts, "Contacts fetched successfully");
    }

    /**
     * @Method : toggleStatus
     * @Discriptim :
     *
     * @param id
     * @param active
     * @return
     * @throws CommonException
     */
    @PostMapping(value = "/{id}/status")
    public ResponseResource<CommonResponse> toggleStatus(@PathVariable Long id, @RequestParam Boolean active) throws CommonException {
        log.info("Toggling status of Contact {} to {}", id, active);
        CommonResponse response = contactService.toggleStatus(id, active);
        return ResponseResource.success(HttpStatus.OK, response, "Status updated successfully");
    }

    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<ContactDto>> searchContacts(@RequestBody ContactFilter contactFilter) throws CommonException {
        log.info("Fetching all contacts with filter {}", contactFilter);
        List<ContactDto> response = contactService.searchContact(contactFilter);
        return ResponseResource.success(HttpStatus.OK, response, "Contacts fetched successfully");
    }

    @GetMapping(value = "/network/all")
    public ResponseResource<List<TenantDto>> getMyNetwork() throws CommonException {
        log.info("Fetching established business network connections");
        List<TenantDto> response = contactService.getMyNetwork();
        return ResponseResource.success(HttpStatus.OK, response, "Network connections fetched successfully");
    }

    @PostMapping(value = "/network/{id}/update")
    public ResponseResource<CommonResponse<?>> updateNetworkStatus(@PathVariable Long id, @RequestParam NetworkStatus status) throws CommonException {
        log.info("Updating Network Request {} to status {}", id, status);
        CommonResponse<?> response = contactService.updateNetworkStatus(id, status);
        return ResponseResource.success(HttpStatus.OK, response, "Network status updated successfully");
    }

    @PostMapping(value = "/network/request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<CommonResponse<?>> sendNetworkRequest(@RequestBody NetworkRequestDto networkRequestDto) throws CommonException {
        log.info("Sending network trade request: {}", networkRequestDto);
        CommonResponse<?> response = contactService.sendNetworkRequest(networkRequestDto);
        return ResponseResource.success(HttpStatus.CREATED, response, "Trade request sent successfully");
    }

    @GetMapping(value = "/network/requests/incoming", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseResource<List<NetworkRequest>> getIncomingRequests() throws CommonException {
        log.info("Fetching incoming network requests for current tenant");
        List<NetworkRequest> requests = contactService.getIncomingRequests();
        return ResponseResource.success(HttpStatus.OK, requests, "Incoming requests fetched successfully");
    }
}
