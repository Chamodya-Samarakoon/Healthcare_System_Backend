package com.example.HealthcareSystem_Backend.service;

import com.example.HealthcareSystem_Backend.dto.AuthDTOs.*;
import com.example.HealthcareSystem_Backend.dto.OperationalDTOs.*;
import com.example.HealthcareSystem_Backend.entity.*;
import com.example.HealthcareSystem_Backend.exception.BadRequestException;
import com.example.HealthcareSystem_Backend.exception.ResourceNotFoundException;
import com.example.HealthcareSystem_Backend.repository.*;
import com.example.HealthcareSystem_Backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HospitalServices {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final LaboratoryTestRepository labRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AdmissionRepository admissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;
    private final StaffAttendanceRepository staffAttendanceRepository;
    private final LeaveRecordRepository leaveRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;

    // --- Audit Log Helper ---
    public void recordAudit(String username, String action, String module, String desc) {
        auditLogRepository.save(AuditLog.builder()
                .username(username)
                .action(action)
                .module(module)
                .description(desc)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // --- Authentication & User Operations ---
    public LoginResponse authenticate(LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String jwt = jwtService.generateToken(user);
        recordAudit(user.getUsername(), "LOGIN", "AUTH", "User logged into system");
        return LoginResponse.builder()
                .token(jwt)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public User createUser(UserRequest req, String adminUser) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .email(req.getEmail())
                .role(req.getRole())
                .active(true)
                .build();
        User saved = userRepository.save(user);

        // SYNC 1: Automatically create or link matching Employee record
        String empEmail = (saved.getEmail() != null && !saved.getEmail().isBlank())
                ? saved.getEmail()
                : saved.getUsername() + "@medicore.com";

        if (!employeeRepository.existsByEmail(empEmail)) {
            String roleName = (saved.getRole() != null) ? saved.getRole().name() : "STAFF";
            String rawName = (saved.getFullName() != null && !saved.getFullName().isBlank())
                    ? saved.getFullName().trim()
                    : saved.getUsername();

            String firstName = rawName;
            String lastName = "(" + roleName + ")";
            if (rawName.contains(" ")) {
                int firstSpaceIndex = rawName.indexOf(" ");
                firstName = rawName.substring(0, firstSpaceIndex).trim();
                lastName = rawName.substring(firstSpaceIndex + 1).trim();
            }

            Employee employee = Employee.builder()
                    .id(saved.getId()) // Explicitly align primary key with User ID
                    .employeeCode("EMP-" + String.format("%04d", saved.getId()))
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(empEmail)
                    .designation(formatDesignation(roleName))
                    .status(Employee.EmploymentStatus.ACTIVE)
                    .active(true)
                    .dateOfJoining(LocalDate.now())
                    .build();

            employeeRepository.save(employee);
        }

        // SYNC 2: When an Admin adds a user with role DOCTOR
        if (req.getRole() == Role.DOCTOR) {
            String rawName = (req.getFullName() != null && !req.getFullName().isBlank())
                    ? req.getFullName().trim()
                    : req.getUsername().trim();

            rawName = rawName.replaceAll("(?i)^dr\\.?\\s*", "").trim();

            String firstName = rawName;
            String lastName = "";
            if (rawName.contains(" ")) {
                int firstSpaceIndex = rawName.indexOf(" ");
                firstName = rawName.substring(0, firstSpaceIndex).trim();
                lastName = rawName.substring(firstSpaceIndex + 1).trim();
            }

            String constructedFullName = ("Dr. " + firstName + " " + lastName).trim();

            Doctor doc = Doctor.builder()
                    .user(saved)
                    .firstName(firstName)
                    .lastName(lastName)
                    .fullName(constructedFullName)
                    .email(saved.getEmail())
                    .specialization("General Medicine")
                    .status("ACTIVE")
                    .active(true)
                    .workingDays("Monday - Friday")
                    .workingHours("08:00 AM - 02:00 PM")
                    .build();
            doctorRepository.save(doc);
        }

        recordAudit(adminUser, "CREATE", "USER",
                "Created user " + saved.getUsername() + " (" + saved.getRole() + ")");
        return saved;
    }

    @Transactional
    public Doctor createDoctor(DoctorRegistrationRequest req, String adminUser) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username already in use for login: " + req.getUsername());
        }

        String firstName = (req.getFirstName() != null) ? req.getFirstName().trim() : "";
        String lastName = (req.getLastName() != null) ? req.getLastName().trim() : "";

        firstName = firstName.replaceAll("(?i)^dr\\.?\\s*", "").trim();

        if (firstName.isEmpty() && req.getFullName() != null) {
            String clean = req.getFullName().replaceAll("(?i)^dr\\.?\\s*", "").trim();
            if (clean.contains(" ")) {
                int idx = clean.indexOf(" ");
                firstName = clean.substring(0, idx).trim();
                lastName = clean.substring(idx + 1).trim();
            } else {
                firstName = clean;
            }
        }

        String constructedFullName = ("Dr. " + firstName + " " + lastName).trim();

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(constructedFullName)
                .email(req.getEmail())
                .role(Role.DOCTOR)
                .active(true)
                .build();
        User savedUser = userRepository.save(user);

        Department dept = null;
        if (req.getDepartmentId() != null) {
            dept = departmentRepository.findById(req.getDepartmentId()).orElse(null);
        }

        Doctor doctor = Doctor.builder()
                .user(savedUser)
                .firstName(firstName)
                .lastName(lastName)
                .fullName(constructedFullName)
                .email(req.getEmail())
                .phone(req.getPhone())
                .specialization(req.getSpecialization() != null ? req.getSpecialization() : "General Medicine")
                .department(dept)
                .workingDays(req.getWorkingDays() != null ? req.getWorkingDays() : "Monday - Friday")
                .workingHours(req.getWorkingHours() != null ? req.getWorkingHours() : "08:00 AM - 02:00 PM")
                .status("ACTIVE")
                .active(true)
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);

        // SYNC: Create matching Employee record for Doctor
        String docEmail = (savedDoctor.getEmail() != null && !savedDoctor.getEmail().isBlank())
                ? savedDoctor.getEmail()
                : "dr." + savedDoctor.getId() + "@medicore.com";

        if (!employeeRepository.existsByEmail(docEmail)) {
            Employee emp = Employee.builder()
                    .employeeCode("DOC-" + String.format("%04d", savedDoctor.getId()))
                    .firstName("Dr. " + firstName)
                    .lastName(lastName)
                    .email(docEmail)
                    .phone(savedDoctor.getPhone())
                    .designation(savedDoctor.getSpecialization())
                    .department(dept)
                    .status(Employee.EmploymentStatus.ACTIVE)
                    .active(true)
                    .dateOfJoining(LocalDate.now())
                    .build();
            employeeRepository.save(emp);
        }

        recordAudit(adminUser, "CREATE", "DOCTOR",
                "Created doctor & user credentials: " + savedDoctor.getFullName());
        return savedDoctor;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + req.getUsername()));

        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(req.getEmail().trim())) {
            throw new BadRequestException("Provided email does not match this account");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        recordAudit(user.getUsername(), "UPDATE", "AUTH", "Password was reset via forgot password request");
    }

    // --- Patient Operations ---
    public Patient registerPatient(PatientRequest req, String username) {
        String patientNumber = "PAT-" + System.currentTimeMillis() % 1000000;
        Patient patient = Patient.builder()
                .patientNumber(patientNumber)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .nic(req.getNic())
                .phone(req.getPhone())
                .email(req.getEmail())
                .address(req.getAddress())
                .emergencyContact(req.getEmergencyContact())
                .bloodGroup(req.getBloodGroup())
                .conditionSummary(req.getConditionSummary())
                .registrationDate(LocalDate.now())
                .build();
        Patient saved = patientRepository.save(patient);
        recordAudit(username, "CREATE", "PATIENT", "Registered patient: " + patientNumber);
        return saved;
    }

    @Transactional
    public void deletePatient(Long patientId, String adminUser) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found with id: " + patientId));

        String patientRef = patient.getPatientNumber() != null ? patient.getPatientNumber()
                : "ID #" + patientId;

        appointmentRepository.deleteByPatientId(patientId);
        medicalRecordRepository.deleteByPatientId(patientId);
        labRepository.deleteByPatientId(patientId);
        admissionRepository.deleteByPatientId(patientId);

        patientRepository.delete(patient);
        recordAudit(adminUser, "DELETE", "PATIENT", "Admin deleted patient: " + patientRef);
    }

    // --- Doctor Operations ---
    @Transactional
    public void deleteDoctor(Long doctorId, String adminUser) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found with id: " + doctorId));

        String doctorDisplayName = doctor.getFullName() != null
                ? doctor.getFullName()
                : (doctor.getFirstName() != null ? doctor.getFirstName() + " " + doctor.getLastName()
                        : "ID #" + doctorId);

        appointmentRepository.deleteByDoctorId(doctorId);
        medicalRecordRepository.deleteByDoctorId(doctorId);

        User linkedUser = doctor.getUser();
        doctorRepository.delete(doctor);

        if (linkedUser != null) {
            userRepository.delete(linkedUser);
        }

        recordAudit(adminUser, "DELETE", "DOCTOR", "Admin removed doctor: " + doctorDisplayName);
    }

    // --- Appointment Operations ---
    public Appointment bookAppointment(AppointmentRequest req, String username) {
        Patient p = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Doctor d = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        LocalDateTime start = req.getAppointmentTime().minusMinutes(14);
        LocalDateTime end = req.getAppointmentTime().plusMinutes(14);
        List<Appointment> conflicts = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                d.getId(), start, end);
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Doctor already has an appointment scheduled near this time");
        }

        Appointment app = Appointment.builder()
                .patient(p)
                .doctor(d)
                .appointmentTime(req.getAppointmentTime())
                .reason(req.getReason())
                .notes(req.getNotes())
                .status(Appointment.Status.SCHEDULED)
                .build();
        Appointment saved = appointmentRepository.save(app);
        recordAudit(username, "CREATE", "APPOINTMENT", "Booked appointment for patient ID: " + p.getId());
        return saved;
    }

    @Transactional
    public Appointment updateAppointment(Long id, AppointmentRequest req, String adminUser) {
        Appointment app = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));

        if (req.getDoctorId() != null && !req.getDoctorId().equals(app.getDoctor().getId())) {
            Doctor newDoctor = doctorRepository.findById(req.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + req.getDoctorId()));
            app.setDoctor(newDoctor);
        }

        if (req.getAppointmentTime() != null) {
            LocalDateTime start = req.getAppointmentTime().minusMinutes(14);
            LocalDateTime end = req.getAppointmentTime().plusMinutes(14);
            List<Appointment> conflicts = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                    app.getDoctor().getId(), start, end);

            boolean hasConflict = conflicts.stream().anyMatch(existing -> !existing.getId().equals(id));
            if (hasConflict) {
                throw new BadRequestException("Doctor already has an appointment scheduled near this time");
            }
            app.setAppointmentTime(req.getAppointmentTime());
        }

        if (req.getReason() != null) {
            app.setReason(req.getReason());
        }
        if (req.getNotes() != null) {
            app.setNotes(req.getNotes());
        }

        Appointment updated = appointmentRepository.save(app);
        recordAudit(adminUser, "UPDATE", "APPOINTMENT",
                "Updated appointment #" + id + " for patient ID: " + app.getPatient().getId());
        return updated;
    }

    // --- Clinical & Medical Records ---
    public MedicalRecord addMedicalRecord(MedicalRecordRequest req, String username) {
        Patient p = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Doctor d = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        MedicalRecord record = MedicalRecord.builder()
                .patient(p)
                .doctor(d)
                .symptoms(req.getSymptoms())
                .diagnosis(req.getDiagnosis())
                .treatment(req.getTreatment())
                .visitDate(LocalDateTime.now())
                .build();
        MedicalRecord saved = medicalRecordRepository.save(record);
        recordAudit(username, "CREATE", "MEDICAL_RECORD", "Added record for patient: " + p.getPatientNumber());
        return saved;
    }

    // --- Pharmacy Management ---
    @Transactional
    public Prescription createPrescription(PrescriptionRequest req, String username) {
        Patient p = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        Doctor d = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Prescription pres = Prescription.builder()
                .patient(p)
                .doctor(d)
                .issueDate(LocalDateTime.now())
                .status("PENDING")
                .items(new ArrayList<>())
                .build();

        Prescription savedPres = prescriptionRepository.save(pres);

        for (PrescriptionItemRequest itemReq : req.getItems()) {
            Medicine med = medicineRepository.findById(itemReq.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found"));
            PrescriptionItem item = PrescriptionItem.builder()
                    .prescription(savedPres)
                    .medicine(med)
                    .quantity(itemReq.getQuantity())
                    .dosage(itemReq.getDosage())
                    .build();
            savedPres.getItems().add(item);
        }
        return prescriptionRepository.save(savedPres);
    }

    @Transactional
    public void dispensePrescription(Long prescriptionId, String username) {
        Prescription pres = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        if ("DISPENSED".equalsIgnoreCase(pres.getStatus())) {
            throw new BadRequestException("Prescription is already dispensed");
        }

        for (PrescriptionItem item : pres.getItems()) {
            Medicine med = item.getMedicine();
            if (med.getExpiryDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Cannot dispense expired medicine: " + med.getName());
            }
            if (med.getQuantity() < item.getQuantity()) {
                throw new BadRequestException("Insufficient stock for medicine: " + med.getName());
            }
            med.setQuantity(med.getQuantity() - item.getQuantity());
            medicineRepository.save(med);
        }

        pres.setStatus("DISPENSED");
        prescriptionRepository.save(pres);
        recordAudit(username, "DISPENSE", "PHARMACY", "Dispensed prescription #" + prescriptionId);
    }

    @Transactional
    public void deleteUser(Long userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getUsername().equalsIgnoreCase(adminUsername)) {
            throw new BadRequestException("You cannot delete your own active administrator account");
        }

        doctorRepository.findByUserId(userId).ifPresent(doc -> {
            appointmentRepository.deleteByDoctorId(doc.getId());
            medicalRecordRepository.deleteByDoctorId(doc.getId());
            doctorRepository.delete(doc);
        });

        // Delete any linked employee records by email or username
        String email = (user.getEmail() != null) ? user.getEmail() : user.getUsername() + "@medicore.com";
        employeeRepository.findAll().stream()
                .filter(e -> e.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .ifPresent(employeeRepository::delete);

        userRepository.delete(user);
        recordAudit(adminUsername, "DELETE", "USER",
                "Deleted staff account: " + user.getUsername() + " (" + user.getRole() + ")");
    }

    // --- Staff / Employee Operations ---
    @Transactional
    public void deleteEmployee(Long employeeId, String adminUsername) {
        // 1. Try finding by direct Employee entity ID
        Optional<Employee> empOpt = employeeRepository.findById(employeeId);
        if (empOpt.isPresent()) {
            Employee employee = empOpt.get();
            String empEmail = employee.getEmail();

            // Clear any attendance records pointing to this employee
            staffAttendanceRepository.findAll().stream()
                    .filter(a -> a.getEmployee() != null && a.getEmployee().getId().equals(employeeId))
                    .forEach(staffAttendanceRepository::delete);

            // Clear any leave requests pointing to this employee
            leaveRecordRepository.findAll().stream()
                    .filter(l -> l.getEmployee() != null && l.getEmployee().getId().equals(employeeId))
                    .forEach(leaveRecordRepository::delete);

            employeeRepository.delete(employee);

            // Clean up corresponding user/doctor account if one exists with the same email
            if (empEmail != null && !empEmail.isBlank()) {
                userRepository.findAll().stream()
                        .filter(u -> empEmail.equalsIgnoreCase(u.getEmail()))
                        .findFirst()
                        .ifPresent(user -> {
                            if (!user.getUsername().equalsIgnoreCase(adminUsername)) {
                                doctorRepository.findByUserId(user.getId()).ifPresent(doc -> {
                                    appointmentRepository.deleteByDoctorId(doc.getId());
                                    medicalRecordRepository.deleteByDoctorId(doc.getId());
                                    doctorRepository.delete(doc);
                                });
                                userRepository.delete(user);
                            }
                        });
            }

            recordAudit(adminUsername, "DELETE", "STAFF",
                    "Deleted employee: " + employee.getEmployeeCode() + " (" + employee.getFirstName() + " "
                            + employee.getLastName() + ")");
            return;
        }

        // 2. Fallback: Staff record originates from a User account (ID matches User.id)
        Optional<User> userOpt = userRepository.findById(employeeId);
        if (userOpt.isPresent()) {
            deleteUser(userOpt.get().getId(), adminUsername);
            return;
        }

        // 3. Fallback: Staff record originates from a Doctor entity (ID matches
        // Doctor.id)
        Optional<Doctor> docOpt = doctorRepository.findById(employeeId);
        if (docOpt.isPresent()) {
            deleteDoctor(docOpt.get().getId(), adminUsername);
            return;
        }

        throw new ResourceNotFoundException("Staff record not found with id: " + employeeId);
    }

    // --- Inpatient & Admission Operations ---
    @Transactional
    public Admission admitPatient(Admission admission, String username) {
        if (admission.getPatient() == null || admission.getPatient().getId() == null) {
            throw new BadRequestException("Patient information is required for admission.");
        }

        Patient patient = patientRepository.findById(admission.getPatient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (admissionRepository.existsByPatientIdAndStatus(patient.getId(), "ADMITTED")) {
            throw new BadRequestException("Patient " + patient.getFirstName() + " " + patient.getLastName()
                    + " is already currently admitted in a ward.");
        }

        String ward = admission.getWardNumber() != null ? admission.getWardNumber().trim() : "";
        String bed = admission.getBedNumber() != null ? admission.getBedNumber().trim() : "";

        if (ward.isBlank() || bed.isBlank()) {
            throw new BadRequestException("Ward number and bed number are required.");
        }

        boolean isBedOccupied = admissionRepository.existsByWardNumberIgnoreCaseAndBedNumberIgnoreCaseAndStatus(
                ward, bed, "ADMITTED");

        if (isBedOccupied) {
            throw new BadRequestException(
                    "Bed " + bed + " in " + ward + " is currently occupied. Please select a vacant bed.");
        }

        admission.setPatient(patient);
        admission.setWardNumber(ward);
        admission.setBedNumber(bed);
        admission.setAdmissionDate(LocalDateTime.now());
        admission.setStatus("ADMITTED");

        Admission saved = admissionRepository.save(admission);
        recordAudit(username, "ADMIT", "ADMISSION",
                "Admitted patient " + patient.getPatientNumber() + " to " + ward + ", " + bed);
        return saved;
    }

    @Transactional
    public Admission dischargePatient(Long admissionId, String username) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Admission record not found with id: " + admissionId));

        if ("DISCHARGED".equalsIgnoreCase(admission.getStatus())) {
            throw new BadRequestException("Patient is already discharged.");
        }

        admission.setStatus("DISCHARGED");
        admission.setDischargeDate(LocalDateTime.now());

        Admission saved = admissionRepository.save(admission);
        recordAudit(username, "DISCHARGE", "ADMISSION",
                "Discharged patient from " + admission.getWardNumber() + ", " + admission.getBedNumber());
        return saved;
    }

    // --- Billing & Payments ---
    @Transactional
    public Payment processPayment(PaymentRequest req, String username) {
        Invoice invoice = invoiceRepository.findById(req.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        double newPaid = invoice.getPaidAmount() + req.getAmount();
        if (newPaid > invoice.getTotalAmount()) {
            throw new BadRequestException("Paid amount cannot exceed total bill");
        }
        invoice.setPaidAmount(newPaid);
        if (newPaid >= invoice.getTotalAmount()) {
            invoice.setStatus(Invoice.Status.PAID);
        } else {
            invoice.setStatus(Invoice.Status.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(req.getAmount())
                .paymentMethod(req.getPaymentMethod())
                .paymentDate(LocalDateTime.now())
                .receiptNumber("REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        Payment saved = paymentRepository.save(payment);
        recordAudit(username, "PAYMENT", "BILLING",
                "Recorded payment of " + req.getAmount() + " for invoice #" + invoice.getId());
        return saved;
    }

    // --- Helper Formatter ---
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
}