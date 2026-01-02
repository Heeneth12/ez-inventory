package com.ezh.Inventory.employee.service;

import com.ezh.Inventory.contacts.dto.AddressDto;
import com.ezh.Inventory.contacts.entiry.Address;
import com.ezh.Inventory.contacts.repository.AddressRepository;
import com.ezh.Inventory.employee.dto.EmployeeDto;
import com.ezh.Inventory.employee.dto.EmployeeFilter;
import com.ezh.Inventory.employee.entity.Employee;
import com.ezh.Inventory.employee.repository.EmployeeRepository;
import com.ezh.Inventory.utils.common.CommonResponse;
import com.ezh.Inventory.utils.common.Status;
import com.ezh.Inventory.utils.exception.CommonException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public CommonResponse createEmployee(EmployeeDto employeeDto) throws CommonException {
        log.info("Creating Employee {}", employeeDto);

        Employee employee = toEntity(employeeDto);

        employeeRepository.save(employee);
        return CommonResponse
                .builder()
                .status(Status.SUCCESS)
                .message("Employee Created Successfully")
                .build();
    }


    @Override
    @Transactional
    public CommonResponse updateEmployee(Long id, EmployeeDto employeeDto) throws CommonException {
        log.info("Updating Employee {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CommonException("Employee Not Found", HttpStatus.NOT_FOUND));

        employee.setEmployeeCode(employeeDto.getEmployeeCode());
        employee.setFirstName(employeeDto.getFirstName());
        employee.setLastName(employeeDto.getLastName());
        employee.setGender(employeeDto.getGender());
        employee.setRole(employeeDto.getRole());
        employee.setOfficialEmail(employeeDto.getOfficialEmail());
        employee.setPersonalEmail(employeeDto.getPersonalEmail());
        employee.setContactNumber(employeeDto.getContactNumber());
        employee.setActive(employeeDto.getActive());

        // Update nested address
        employee.setAddress(convertAddress(employeeDto.getAddress()));

        employeeRepository.save(employee);

        return CommonResponse.builder()
                .status(Status.SUCCESS)
                .message("Employee Updated Successfully")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto getEmployee(Long id) throws CommonException {
        log.info("get");
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CommonException("Employee Not Found", HttpStatus.NOT_FOUND));

        return entityToDto(employee);
    }



    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDto> getAllEmployees(EmployeeFilter filter, Integer page, Integer size) throws CommonException {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<Employee> employees = employeeRepository.findAll(pageable);

        return employees.map(this::entityToDto);
    }


    @Override
    @Transactional
    public CommonResponse toggleStatus(Long id, Boolean active) throws CommonException {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new CommonException("Employee Not Found", HttpStatus.NOT_FOUND));

        employee.setActive(active);
        employeeRepository.save(employee);
        return CommonResponse
                .builder()
                .status(Status.SUCCESS)
                .message("Status Updated Successfully")
                .build();
    }

    private Address convertAddress(AddressDto dto) {
        if (dto == null) return null;
        return Address.builder()
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .pinCode(dto.getPinCode())
                .addressType(dto.getType())
                .build();
    }

    public Employee toEntity(EmployeeDto dto) {
        if (dto == null) return null;
        return Employee.builder()
                .employeeCode(dto.getEmployeeCode())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .gender(dto.getGender())
                .role(dto.getRole())
                .officialEmail(dto.getOfficialEmail())
                .personalEmail(dto.getPersonalEmail())
                .contactNumber(dto.getContactNumber())
                .active(dto.getActive())
                .address(convertAddress(dto.getAddress()))
                .build();
    }


    private EmployeeDto entityToDto(Employee entity) {
        if (entity == null) return null;
        return EmployeeDto.builder()
                .id(entity.getId())
                .employeeCode(entity.getEmployeeCode())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .gender(entity.getGender())
                .role(entity.getRole())
                .officialEmail(entity.getOfficialEmail())
                .personalEmail(entity.getPersonalEmail())
                .contactNumber(entity.getContactNumber())
                .active(entity.getActive())
                .address(EntityToAddressDto(entity.getAddress()))
                .build();
    }

    private AddressDto EntityToAddressDto(Address entity) {
        if (entity == null) return null;
        return AddressDto.builder()
                .id(entity.getId())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .pinCode(entity.getPinCode())
                .type(entity.getAddressType())
                .build();
    }

}
