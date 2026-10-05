package com.example.HealthcareSystem_Backend.controller;

import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.exception.BadRequestException;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StaffController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final StaffAttendanceRepository attendanceRepository;
    private final LeaveRecordRepository leaveRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    // --- 1. Employee Management ---

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<List<StaffResponseDTO>> getAllEmployees() {
        Map<String, StaffResponseDTO> unifiedStaff = new LinkedHashMap<>();

        // 1. Manually registered employees from the Employee table
        List<Employee> manualEmployees = employeeRepository.findAll();
        for (Employee emp : manualEmployees) {
            String key = (emp.getEmail() != null && !emp.getEmail().isBlank())
                    ? emp.getEmail().toLowerCase()
                    : "emp_" + emp.getId();

            unifiedStaff.put(key, StaffResponseDTO.builder()
                    .id(emp.getId())
                    .employeeCode(emp.getEmployeeCode())
                    .firstName(emp.getFirstName())
                    .lastName(emp.getLastName())
                    .email(emp.getEmail())
                    .phone(emp.getPhone() != null ? emp.getPhone() : "N/A")
                    .designation(emp.getDesignation() != null ? emp.getDesignation() : "Staff Member")
                    .department(emp.getDepartment() != null ? emp.getDepartment().getName() : "General")
                    .status(emp.getStatus() != null ? emp.getStatus().name() : "ACTIVE")
                    .build());
        }

        // 2. System Users (Admins, Nurses, Accountants, Lab Staff, Pharmacists,
        // Receptionists)
        List<User> users = userRepository.findAll();
        for (User u : users) {
            String email = (u.getEmail() != null && !u.getEmail().isBlank())
                    ? u.getEmail()
                    : u.getUsername() + "@medicore.com";

            if (!unifiedStaff.containsKey(email.toLowerCase())) {
                String roleName = (u.getRole() != null) ? u.getRole().name() : "STAFF";
                String code = "EMP-" + String.format("%04d", u.getId());
                String formattedUsername = capitalize(u.getUsername());

                unifiedStaff.put(email.toLowerCase(), StaffResponseDTO.builder()
                        .id(u.getId())
                        .employeeCode(code)
                        .firstName(formattedUsername)
                        .lastName("(" + roleName + ")")
                        .email(email)
                        .phone("N/A")
                        .designation(formatDesignation(roleName))
                        .department(formatDepartment(roleName))
                        .status("ACTIVE")
                        .build());
            }
        }

        // 3. Clinical Doctors
        List<Doctor> doctors = doctorRepository.findAll();
        for (Doctor doc : doctors) {
            String docEmail = (doc.getEmail() != null && !doc.getEmail().isBlank())
                    ? doc.getEmail()
                    : "dr." + doc.getId() + "@medicore.com";

            if (!unifiedStaff.containsKey(docEmail.toLowerCase())) {
                String deptName = (doc.getDepartment() != null)
                        ? doc.getDepartment().getName()
                        : "Outpatient & Clinical";

                String code = "DOC-" + String.format("%04d", doc.getId());

                unifiedStaff.put(docEmail.toLowerCase(), StaffResponseDTO.builder()
                        .id(doc.getId())
                        .employeeCode(code)
                        .firstName("Dr. " + (doc.getFirstName() != null ? doc.getFirstName() : ""))
                        .lastName(doc.getLastName() != null ? doc.getLastName() : "")
                        .email(docEmail)
                        .phone(doc.getPhone() != null ? doc.getPhone() : "N/A")
                        .designation(
                                doc.getSpecialization() != null ? doc.getSpecialization() : "Consultant Specialist")
                        .department(deptName)
                        .status(doc.getStatus() != null ? doc.getStatus() : "ACTIVE")
                        .build());
            }
        }

        return ResponseEntity.ok(new ArrayList<>(unifiedStaff.values()));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employee> registerEmployee(@RequestBody EmployeeDTO dto) {
        if (dto.getUsername() != null && userRepository.existsByUsername(dto.getUsername())) {
            throw new BadRequestException("Username '" + dto.getUsername() + "' is already taken");
        }
        if (dto.getEmail() != null && employeeRepository.existsByEmail(dto.getEmail())) {
            throw new BadRequestException("Email '" + dto.getEmail() + "' is already registered");
        }

        Department dept = null;
        if (dto.getDepartmentId() != null) {
            dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        }

        // 1. Create and persist Employee entity
        Employee emp = Employee.builder()
                .employeeCode(dto.getEmployeeCode())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .designation(dto.getDesignation())
                .department(dept)
                .dateOfJoining(dto.getDateOfJoining() != null ? dto.getDateOfJoining() : LocalDate.now())
                .status(Employee.EmploymentStatus.ACTIVE)
                .build();

        Employee savedEmp = employeeRepository.save(emp);

        // 2. Create and persist User login credentials if username & password provided
        if (dto.getUsername() != null && !dto.getUsername().isBlank()
                && dto.getPassword() != null && !dto.getPassword().isBlank()) {

            Role userRole = Role.NURSE;
            if (dto.getRole() != null && !dto.getRole().isBlank()) {
                try {
                    userRole = Role.valueOf(dto.getRole().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            User user = User.builder()
                    .username(dto.getUsername().trim())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .email(dto.getEmail() != null ? dto.getEmail().trim() : dto.getUsername().trim() + "@medicore.com")
                    .role(userRole)
                    .build();

            userRepository.save(user);
        }

        return ResponseEntity.ok(savedEmp);
    }

    @PutMapping("/employees/{id}/department")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Employee> assignDepartment(
            @PathVariable Long id,
            @RequestBody Map<String, Long> payload) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        Department dept = departmentRepository.findById(payload.get("departmentId"))
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        emp.setDepartment(dept);
        return ResponseEntity.ok(employeeRepository.save(emp));
    }

    // --- 2. Attendance ---

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT','RECEPTIONIST')")
    public ResponseEntity<List<StaffAttendance>> getAttendanceByDate(
            @RequestParam(required = false) String date) {
        LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(attendanceRepository.findByAttendanceDate(queryDate));
    }

    @PostMapping("/attendance/clock-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<StaffAttendance> clockIn(@RequestBody Map<String, Object> payload) {
        if (!payload.containsKey("employeeId") || payload.get("employeeId") == null) {
            throw new BadRequestException("employeeId is required");
        }
        Long staffId = Long.valueOf(payload.get("employeeId").toString());

        // Resolve or auto-provision Employee
        Employee emp = resolveOrProvisionEmployee(staffId);

        LocalDate today = LocalDate.now();
        StaffAttendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(emp.getId(), today)
                .orElse(StaffAttendance.builder()
                        .employee(emp)
                        .attendanceDate(today)
                        .checkInTime(LocalTime.now())
                        .status(StaffAttendance.AttendanceStatus.PRESENT)
                        .build());

        return ResponseEntity.ok(attendanceRepository.save(attendance));
    }

    @PutMapping("/attendance/clock-out/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<StaffAttendance> clockOut(@PathVariable Long id) {
        StaffAttendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found with id: " + id));
        attendance.setCheckOutTime(LocalTime.now());
        return ResponseEntity.ok(attendanceRepository.save(attendance));
    }

    // --- 3. Leave Records ---

    @GetMapping("/leaves")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT','RECEPTIONIST')")
    public ResponseEntity<List<LeaveRecord>> getLeaveRecords() {
        return ResponseEntity.ok(leaveRepository.findAll());
    }

    @PostMapping("/leaves")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR', 'NURSE', 'LAB_STAFF')")
    public ResponseEntity<LeaveRecord> submitLeaveRequest(@RequestBody LeaveDTO dto) {
        Employee emp = resolveOrProvisionEmployee(dto.getEmployeeId());

        LeaveRecord leave = LeaveRecord.builder()
                .employee(emp)
                .leaveType(dto.getLeaveType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(LeaveRecord.LeaveStatus.PENDING)
                .build();

        return ResponseEntity.ok(leaveRepository.save(leave));
    }

    @PutMapping("/leaves/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveRecord> updateLeaveStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication auth) {
        LeaveRecord leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave record not found with id: " + id));

        leave.setStatus(LeaveRecord.LeaveStatus.valueOf(payload.get("status").toUpperCase()));
        leave.setApprovedBy(auth != null ? auth.getName() : "ADMIN");
        return ResponseEntity.ok(leaveRepository.save(leave));
    }

    // --- Helper Methods ---

    private Employee resolveOrProvisionEmployee(Long staffId) {
        // 1. Direct Employee match
        Optional<Employee> empOpt = employeeRepository.findById(staffId);
        if (empOpt.isPresent()) {
            return empOpt.get();
        }

        // 2. Fallback: match User entity and provision
        Optional<User> userOpt = userRepository.findById(staffId);
        if (userOpt.isPresent()) {
            User u = userOpt.get();
            String email = (u.getEmail() != null && !u.getEmail().isBlank())
                    ? u.getEmail()
                    : u.getUsername() + "@medicore.com";

            // If an Employee already exists with this email, link to that Employee
            return employeeRepository.findAll().stream()
                    .filter(e -> e.getEmail().equalsIgnoreCase(email))
                    .findFirst()
                    .orElseGet(() -> {
                        String roleName = (u.getRole() != null) ? u.getRole().name() : "STAFF";
                        return employeeRepository.save(Employee.builder()
                                .employeeCode("EMP-" + String.format("%04d", u.getId()))
                                .firstName(capitalize(u.getUsername()))
                                .lastName("(" + roleName + ")")
                                .email(email)
                                .phone("N/A")
                                .designation(formatDesignation(roleName))
                                .status(Employee.EmploymentStatus.ACTIVE)
                                .dateOfJoining(LocalDate.now())
                                .build());
                    });
        }

        // 3. Fallback: match Doctor entity and provision
        Optional<Doctor> docOpt = doctorRepository.findById(staffId);
        if (docOpt.isPresent()) {
            Doctor d = docOpt.get();
            String docEmail = (d.getEmail() != null && !d.getEmail().isBlank())
                    ? d.getEmail()
                    : "dr." + d.getId() + "@medicore.com";

            return employeeRepository.findAll().stream()
                    .filter(e -> e.getEmail().equalsIgnoreCase(docEmail))
                    .findFirst()
                    .orElseGet(() -> employeeRepository.save(Employee.builder()
                            .employeeCode("DOC-" + String.format("%04d", d.getId()))
                            .firstName("Dr. " + (d.getFirstName() != null ? d.getFirstName() : ""))
                            .lastName(d.getLastName() != null ? d.getLastName() : "")
                            .email(docEmail)
                            .phone(d.getPhone() != null ? d.getPhone() : "N/A")
                            .designation(d.getSpecialization() != null ? d.getSpecialization() : "Consultant Physician")
                            .department(d.getDepartment())
                            .status(Employee.EmploymentStatus.ACTIVE)
                            .dateOfJoining(LocalDate.now())
                            .build()));
        }

        throw new ResourceNotFoundException("Staff record not found with id: " + staffId);
    }

    private String formatDesignation(String role) {
        return switch (role) {
            case "ADMIN" -> "Hospital Administrator";
            case "DOCTOR" -> "Consultant Physician";
            case "NURSE" -> "Registered Staff Nurse";
            case "ACCOUNTANT" -> "Financial Officer";
            case "LAB_STAFF" -> "Laboratory Technician";
            case "PHARMACIST" -> "Staff Pharmacist";
            case "RECEPTIONIST" -> "Front Desk Officer";
            default -> role.replace("_", " ");
        };
    }

    private String formatDepartment(String role) {
        return switch (role) {
            case "ADMIN" -> "Executive Administration";
            case "DOCTOR" -> "Outpatient & Clinical";
            case "NURSE" -> "Inpatient & Nursing Care";
            case "ACCOUNTANT" -> "Finance & Accounts";
            case "LAB_STAFF" -> "Diagnostic Pathology Lab";
            case "PHARMACIST" -> "Hospital Pharmacy";
            case "RECEPTIONIST" -> "Admissions & Front Desk";
            default -> "General Hospital Services";
        };
    }

    private String capitalize(String str) {
        if (str == null || str.isBlank())
            return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // --- DTO Classes ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffResponseDTO {
        private Long id;
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String designation;
        private String department;
        private String status;
    }

    @Data
    public static class EmployeeDTO {
        private String employeeCode;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String designation;
        private Long departmentId;
        private LocalDate dateOfJoining;

        // Login Credentials Fields
        private String username;
        private String password;
        private String role;
    }

    @Data
    public static class LeaveDTO {
        private Long employeeId;
        private LeaveRecord.LeaveType leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
    }
}